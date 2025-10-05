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
package org.apache.sling.testing;

import java.util.Date;
import java.util.Optional;

import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.sling.testing.clients.*;
import org.apache.sling.testing.clients.interceptors.FormBasedAuthInterceptor;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import static org.apache.sling.testing.clients.SystemPropertiesConfig.CONFIG_PROP_PREFIX;
import static org.apache.sling.testing.clients.SystemPropertiesConfig.HTTP_RETRIES_ERROR_CODES_PROP;

public class FormBasedAuthInterceptorTest {

    private static final String LOGIN_COOKIE_NAME = "login-token";
    private static final String LOGIN_COOKIE_VALUE = "testvalue";
    private static final String OK_PATH = "/test/ok";
    private static final String ANONYMOUS_PATH = "/test/anonymous";
    private static final String LOGIN_PATH = "/test/j_security_check";
    private static final String LOGIN_OK_PATH = OK_PATH + "/j_security_check";
    private static final String UNAUTHORIZED_PATH = "/test/unauthorized";
    private static final String LOGIN_OK_RESPONSE = "TEST_OK LOGIN";
    private static final String UNAUTHORIZED_RESPONSE = "TEST_UNAUTHORIZED";
    private static final String ANONYMOUS_RESPONSE = "TEST_ANONYMOUS";
    private static final String OK_RESPONSE = "TEST_OK";
    private static final String UNREACHABLE_PATH = "/unreachable/path";
    private static final String UNREACHABLE_LOGIN_PATH = "/unreachable/j_security_check";

    static {
        System.setProperty(CONFIG_PROP_PREFIX + SystemPropertiesConfig.HTTP_LOG_RETRIES_PROP, "true");
        System.setProperty(CONFIG_PROP_PREFIX + SystemPropertiesConfig.HTTP_DELAY_PROP, "50");
        System.setProperty(CONFIG_PROP_PREFIX + SystemPropertiesConfig.HTTP_RETRIES_PROP, "4");
        System.setProperty(CONFIG_PROP_PREFIX + HTTP_RETRIES_ERROR_CODES_PROP, "500,502,503");
    }

    @ClassRule
    public static HttpServerRule httpServer = new HttpServerRule() {
        @Override
        protected void registerHandlers() {
            serverBootstrap.register(LOGIN_OK_PATH, (request, response, context) -> {
                response.setEntity(new StringEntity(LOGIN_OK_RESPONSE));
                response.setCode(HttpStatus.SC_OK);
                response.setHeader(
                        "set-cookie",
                        LOGIN_COOKIE_NAME + "=" + LOGIN_COOKIE_VALUE
                                + "; Path=/; HttpOnly; Max-Age=3600; Secure; SameSite=Lax");
            });
            serverBootstrap.register(LOGIN_PATH, (request, response, context) -> {
                response.setEntity(new StringEntity(LOGIN_OK_RESPONSE));
                response.setCode(HttpStatus.SC_OK);
                response.setHeader(
                        "set-cookie",
                        LOGIN_COOKIE_NAME + "=" + LOGIN_COOKIE_VALUE
                                + "; Path=/; HttpOnly; Max-Age=3600; Secure; SameSite=Lax");
            });
            serverBootstrap.register(UNAUTHORIZED_PATH, (request, response, context) -> {
                response.setEntity(new StringEntity(UNAUTHORIZED_RESPONSE));
                response.setCode(HttpStatus.SC_UNAUTHORIZED);
            });
            serverBootstrap.register(ANONYMOUS_PATH, (request, response, context) -> {
                response.setEntity(new StringEntity(ANONYMOUS_RESPONSE));
                response.setCode(HttpStatus.SC_OK);
            });
            serverBootstrap.register(OK_PATH, (request, response, context) -> {
                response.setEntity(new StringEntity(OK_RESPONSE));
                response.setCode(HttpStatus.SC_OK);
            });
            serverBootstrap.register(UNREACHABLE_PATH, (request, response, context) -> {
                response.setEntity(new StringEntity(OK_RESPONSE));
                response.setCode(HttpStatus.SC_BAD_REQUEST);
            });
            serverBootstrap.register(UNREACHABLE_LOGIN_PATH, (request, response, context) -> {
                response.setEntity(new StringEntity(OK_RESPONSE));
                response.setCode(HttpStatus.SC_BAD_GATEWAY);
            });
        }
    };

    /**
     * Test a login cookie is set by the response when request is successful
     * and removed in case of unauthorized access
     *
     * @throws Exception if problem occurs
     */
    @Test
    public void testLoginToken() throws Exception {
        FormBasedAuthInterceptor interceptor = new FormBasedAuthInterceptor(LOGIN_COOKIE_NAME);
        SlingClient c = SlingClient.Builder.create(httpServer.getURI(), "user", "pass")
                .addInterceptorLast(interceptor)
                .build();

        // Make sure cookie is stored
        c.doGet(LOGIN_OK_PATH, HttpStatus.SC_OK);
        Optional<Cookie> loginCookie = getLoginCookie(c);
        Assert.assertTrue("login token cookie should be stored on the client config", loginCookie.isPresent());
        Assert.assertFalse(
                "login token cookie should not be expired", loginCookie.get().isExpired(new Date()));

        c.doGet(UNAUTHORIZED_PATH, HttpStatus.SC_UNAUTHORIZED);
        loginCookie = getLoginCookie(c);
        Assert.assertFalse(
                "login token cookie should be forced removed from the client config", loginCookie.isPresent());
    }

    /**
     * Test no authentication attempt is performed when user is {@code null}
     *
     * @throws ClientException if problem occurs
     */
    @Test
    public void testAnonymousUser() throws ClientException {
        FormBasedAuthInterceptor interceptor = new FormBasedAuthInterceptor(LOGIN_COOKIE_NAME);
        SlingClient client = SlingClient.Builder.create(httpServer.getURI(), null, "pass")
                .addInterceptorLast(interceptor)
                .build();
        SlingHttpResponse response = client.doGet(ANONYMOUS_PATH, HttpStatus.SC_OK);

        Assert.assertSame(null, client.getUser());
        Assert.assertSame("pass", client.getPassword());
        Assert.assertEquals("Should return expected response", ANONYMOUS_RESPONSE, response.getContent());
    }

    /**
     * Test authentication attempt is performed when the user is set to the empty string.
     * This user is valid according to the basic authentication schema
     *
     * @throws ClientException if problem occurs
     */
    @Test
    public void testUser() throws ClientException {
        FormBasedAuthInterceptor interceptor = new FormBasedAuthInterceptor(LOGIN_COOKIE_NAME);
        SlingClient client = SlingClient.Builder.create(httpServer.getURI(), "", "pass")
                .addInterceptorLast(interceptor)
                .build();
        SlingHttpResponse response = client.doGet(OK_PATH, HttpStatus.SC_OK);

        Assert.assertSame("", client.getUser());
        Assert.assertSame("pass", client.getPassword());
        Assert.assertEquals("Should return expected response", OK_RESPONSE, response.getContent());
    }

    /**
     * Simulate a login issue when login is not successful due to i.e. network issue,
     * this should also dump the response headers
     *
     * @throws ClientException if problem occurs
     */
    @Test
    public void testLoginIssue() throws ClientException {
        FormBasedAuthInterceptor interceptor = new FormBasedAuthInterceptor(LOGIN_COOKIE_NAME);
        SlingClient client = SlingClient.Builder.create(httpServer.getURI(), "user", "pass")
                .addInterceptorLast(interceptor)
                .build();
        SlingHttpResponse response = client.doGet(UNREACHABLE_PATH, HttpStatus.SC_BAD_REQUEST);

        Assert.assertSame("user", client.getUser());
        Assert.assertSame("pass", client.getPassword());
        Assert.assertEquals("Should return expected response", OK_RESPONSE, response.getContent());
    }

    private static Optional<Cookie> getLoginCookie(SlingClient c) {
        return c.getCookieStore().getCookies().stream()
                .filter(cookie -> LOGIN_COOKIE_NAME.equals(cookie.getName()))
                .findFirst();
    }
}
