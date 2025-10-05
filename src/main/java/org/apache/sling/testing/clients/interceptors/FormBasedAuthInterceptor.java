/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.testing.clients.interceptors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.Credentials;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.cookie.BasicClientCookie;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.sling.testing.clients.util.ServerErrorRetryStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FormBasedAuthInterceptor implements HttpRequestInterceptor, HttpRequestResponseInterceptor {
    static final Logger LOG = LoggerFactory.getLogger(FormBasedAuthInterceptor.class);

    private final String loginPath = "j_security_check";
    private final String loginTokenName;

    public FormBasedAuthInterceptor(String loginTokenName) {
        this.loginTokenName = loginTokenName;
    }

    @Override
    public void process(HttpRequest request, EntityDetails entityDetails, HttpContext context) throws IOException {
        final URI uri = URI.create(request.getRequestUri());
        if (uri.getPath().endsWith(loginPath)) {
            LOG.trace("Request ends with {} so I'm not intercepting the request", loginPath);
            return;
        }

        Cookie loginCookie = getLoginCookie(context, loginTokenName);
        if (loginCookie != null) {
            LOG.trace("Request has cookie {} so I'm not intercepting the request", loginCookie.getName());
            return;
        }

        doLogin(request, context);
    }

    @Override
    public void process(HttpResponse response, EntityDetails entityDetails, HttpContext context) {
        if (response.getCode() != HttpStatus.SC_UNAUTHORIZED) {
            return;
        }

        if (URI.create(HttpClientContext.adapt(context).getRequest().getRequestUri())
                .getPath()
                .endsWith(loginPath)) {
            LOG.trace("Request ends with {} so I'm not intercepting the request", loginPath);
            return;
        }

        Cookie loginCookie = getLoginCookie(context, loginTokenName);
        if (loginCookie == null) {
            return;
        }
        LOG.info("Response code was 401 even though {} is set. Removing the cookie.", loginCookie.getName());
        BasicClientCookie expiredLoginTokenCookie = new BasicClientCookie(loginCookie.getName(), "expired");
        expiredLoginTokenCookie.setExpiryDate(new Date(1)); // far enough in the past
        expiredLoginTokenCookie.setDomain(loginCookie.getDomain());
        expiredLoginTokenCookie.setPath(loginCookie.getPath());
        HttpClientContext.adapt(context).getCookieStore().addCookie(expiredLoginTokenCookie);
    }

    /**
     * Get login token cookie or null if not found
     */
    private Cookie getLoginCookie(HttpContext context, String loginTokenName) {
        for (Cookie cookie : HttpClientContext.adapt(context).getCookieStore().getCookies()) {
            if (cookie.getName().equalsIgnoreCase(loginTokenName)
                    && !cookie.getValue().isEmpty()) {
                return cookie;
            }
        }
        return null;
    }

    private void doLogin(HttpRequest request, HttpContext context) throws IOException {
        // get host
        final HttpHost host = HttpClientContext.adapt(context).getHttpRoute().getTargetHost();

        // get the username and password from the credentials provider
        final CredentialsProvider credsProvider =
                HttpClientContext.adapt(context).getCredentialsProvider();
        final AuthScope scope = new AuthScope(host.getHostName(), host.getPort());
        final String username = Optional.ofNullable(credsProvider.getCredentials(scope, context))
                .map(Credentials::getUserPrincipal)
                .map(Principal::getName)
                .orElse(null);
        if (username == null) {
            return;
        }
        final String password = Optional.ofNullable(credsProvider.getCredentials(scope, context))
                .map(Credentials::getPassword)
                .map(String::valueOf)
                .orElse(null);
        List<NameValuePair> parameters = new LinkedList<>();
        parameters.add(new BasicNameValuePair("j_username", username));
        parameters.add(new BasicNameValuePair("j_password", password));
        HttpEntity httpEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);

        URI loginURI = URI.create(request.getRequestUri()).resolve(loginPath);
        HttpPost loginPost = new HttpPost(loginURI);
        loginPost.setEntity(httpEntity);

        try (CloseableHttpClient client = HttpClientBuilder.create()
                .useSystemProperties()
                .setRetryStrategy(new ServerErrorRetryStrategy())
                .disableRedirectHandling()
                .build()) {

            try (CloseableHttpResponse response = client.execute(host, loginPost, context)) {
                if (response.getCode() >= HttpStatus.SC_BAD_REQUEST) {
                    LOG.error("Got error login response code {} from '{}'", response.getCode(), loginURI);

                    LOG.error("Dumping headers: ");
                    for (Header header : response.getHeaders()) {
                        LOG.error("\t '{}' = '{}'", header.getName(), header.getValue());
                    }

                    try (InputStream inputStream = response.getEntity().getContent()) {
                        String responseText = new BufferedReader(
                                        new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                                .lines()
                                .collect(Collectors.joining("\n"));

                        LOG.error("Error response body was : '{}'", responseText);
                    }
                } else if (getLoginCookie(context, loginTokenName) == null) {
                    LOG.error(
                            "Login response {} from '{}' did not include cookie '{}'.",
                            response.getCode(),
                            loginURI,
                            loginTokenName);
                } else {
                    LOG.debug("Login response {} from '{}'", response.getCode(), loginURI);
                }
            }
        }
    }
}
