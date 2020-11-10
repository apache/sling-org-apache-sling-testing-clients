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
package org.apache.sling.testing;

import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.sling.testing.clients.HttpServerRule;
import org.apache.sling.testing.clients.SlingClient;
import org.apache.sling.testing.clients.interceptors.FormBasedAuthInterceptor;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Date;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;

public class FormBasedAuthInterceptorTest {

    private static final String LOGIN_COOKIE_NAME = "login-token";
    private static final String LOGIN_COOKIE_VALUE = "testvalue";
    private static final String OK_PATH = "/test/ok";
    private static final String LOGIN_OK_PATH = OK_PATH + "/j_security_check";
    private static final String UNAUTHORIZED_PATH = "/test/unauthorized";
    private static final String LOGIN_OK_RESPONSE = "TEST_OK LOGIN";
    private static final String UNAUTHORIZED_RESPONSE = "TEST_UNAUTHORIZED";

    @ClassRule
    public static HttpServerRule httpServer = new HttpServerRule() {
        @Override
        protected void registerHandlers() {
            serverBootstrap.registerHandler(LOGIN_OK_PATH, (request, response, context) -> {
                response.setEntity(new StringEntity(LOGIN_OK_RESPONSE));
                response.setStatusCode(200);
                response.setHeader("set-cookie", LOGIN_COOKIE_NAME + "=" + LOGIN_COOKIE_VALUE +
                        "; Path=/; HttpOnly; Max-Age=3600; Secure; SameSite=Lax");
            });
            serverBootstrap.registerHandler(UNAUTHORIZED_PATH, (request, response, context) -> {
                response.setEntity(new StringEntity(UNAUTHORIZED_RESPONSE));
                response.setStatusCode(401);
            });
        }
    };

    @Test
    public void testLoginToken() throws Exception {
        FormBasedAuthInterceptor interceptor = new FormBasedAuthInterceptor(LOGIN_COOKIE_NAME);
        SlingClient c = SlingClient.Builder.create(httpServer.getURI(), "user", "pass")
                .addInterceptorLast(interceptor).build();

        // Make sure cookie is stored
        c.doGet(LOGIN_OK_PATH, 200);
        Optional<Cookie> loginCookie = getLoginCookie(c);
        Assert.assertThat("login token cookie should be stored on the client config",
                loginCookie.isPresent(), is(true));
        Assert.assertThat("login token cookie should not be expired",
                loginCookie.get().isExpired(new Date()), is(false));

        c.doGet(UNAUTHORIZED_PATH, 401);
        loginCookie = getLoginCookie(c);
        Assert.assertThat("login token cookie should be forced removed from the client config",
                loginCookie.isPresent(), is(false));
    }

    private static Optional<Cookie> getLoginCookie(SlingClient c) {
        return c.getCookieStore().getCookies().stream().filter(
                cookie -> LOGIN_COOKIE_NAME.equals(cookie.getName())).findFirst();
    }

}
