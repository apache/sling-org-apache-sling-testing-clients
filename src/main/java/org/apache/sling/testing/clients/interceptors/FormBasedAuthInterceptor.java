/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.testing.clients.interceptors;

import org.apache.http.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.sling.testing.clients.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;

public class FormBasedAuthInterceptor implements HttpRequestInterceptor {
    static final Logger LOG = LoggerFactory.getLogger(FormBasedAuthInterceptor.class);

    private final String loginPath = "j_security_check";
    private final String loginTokenName;

    public FormBasedAuthInterceptor(String loginTokenName) {
        this.loginTokenName = loginTokenName;
    }

    public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
        final URI uri = URI.create(request.getRequestLine().getUri());
        if (uri.getPath().endsWith(loginPath)) {
            LOG.debug("Request ends with {} so I'm not intercepting the request", loginPath);
            return;
        }

        Cookie loginCookie = getLoginCookie(context, loginTokenName);
        if (loginCookie != null) {
            LOG.debug("Request has cookie {}={} so I'm not intercepting the request", loginCookie.getName(), loginCookie.getValue());
            return;
        }

        // get host
        final HttpHost host = HttpClientContext.adapt(context).getTargetHost();

        // get the username and password from the credentials provider
        final CredentialsProvider credsProvider = HttpClientContext.adapt(context).getCredentialsProvider();
        final AuthScope scope = new AuthScope(host.getHostName(), host.getPort());
        final String username = credsProvider.getCredentials(scope).getUserPrincipal().getName();
        final String password = credsProvider.getCredentials(scope).getPassword();

        List<NameValuePair> parameters = new LinkedList<>();
        parameters.add(new BasicNameValuePair("j_username", username));
        parameters.add(new BasicNameValuePair("j_password", password));
        HttpEntity httpEntity = new UrlEncodedFormEntity(parameters, "utf-8");

        HttpPost loginPost = new HttpPost(URI.create(request.getRequestLine().getUri()).resolve(loginPath));
        loginPost.setEntity(httpEntity);
        final CloseableHttpClient client = HttpClientBuilder.create()
                .setServiceUnavailableRetryStrategy(Constants.HTTP_RETRY_STRATEGY)
                .disableRedirectHandling().build();

        client.execute(host, loginPost, context);

    }

    /** Get login token cookie or null if not found */
    private Cookie getLoginCookie(HttpContext context, String loginTokenName) {
        for (Cookie cookie : HttpClientContext.adapt(context).getCookieStore().getCookies()) {
            if (cookie.getName().equalsIgnoreCase(loginTokenName) && !cookie.getValue().isEmpty()) {
                return cookie;
            }
        }
        return null;
    }
}
