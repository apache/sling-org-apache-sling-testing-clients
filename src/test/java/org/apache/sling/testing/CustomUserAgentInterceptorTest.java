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

import org.apache.http.entity.StringEntity;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.HttpServerRule;
import org.apache.sling.testing.clients.SlingClient;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.apache.sling.testing.clients.interceptors.UserAgentHolder;
import org.apache.sling.testing.clients.interceptors.UserAgentInterceptor;
import org.apache.sling.testing.clients.util.UserAgent;
import org.junit.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CustomUserAgentInterceptorTest {

    private static final String PATH = "/mirror";

    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final String CUSTOM_USER_AGENT_TITLE = "test-client";
    private static final String CUSTOM_USER_AGENT_VERSION = "1.2.3";
    private static final String CUSTOM_USER_AGENT_DETAILS = "details";
    private static final String CUSTOM_USER_AGENT = String.format("%s/%s", CUSTOM_USER_AGENT_TITLE, CUSTOM_USER_AGENT_VERSION);
    private static final String CUSTOM_USER_AGENT_WITH_DETAILS = String.format("%s/%s (%s)", CUSTOM_USER_AGENT_TITLE, CUSTOM_USER_AGENT_VERSION, CUSTOM_USER_AGENT_DETAILS);
    private static final String CUSTOM_USER_AGENT_WITH_APPEND = String.format("%s/%s %s", CUSTOM_USER_AGENT_TITLE, CUSTOM_USER_AGENT_VERSION, CUSTOM_USER_AGENT_DETAILS);

    @ClassRule
    public static HttpServerRule httpServer = new HttpServerRule() {
        @Override
        protected void registerHandlers() {
            serverBootstrap.registerHandler(PATH, (request, response, context) -> {
                response.setEntity(new StringEntity("Success!"));
                response.setStatusCode(200);
                response.setHeaders(request.getHeaders(USER_AGENT_HEADER));
            });
        }
    };
    private static SlingClient c;

    @BeforeClass
    public static void beforeClass() throws ClientException {
        c = SlingClient.Builder
                .create(httpServer.getURI(), "user", "pass")
                .addInterceptorLast(new UserAgentInterceptor())
                .build();
    }

    @After
    public void after() {
        UserAgentHolder.reset();
    }

    @Test
    public void testDefault() throws ClientException {
        SlingHttpResponse response = c.doGet(PATH, 200);
        assertTrue(response.containsHeader(USER_AGENT_HEADER));
        assertEquals(Constants.SLING_CLIENT_USERAGENT_TITLE, response.getFirstHeader(USER_AGENT_HEADER).getValue());
    }

    @Test
    public void testCustom() throws ClientException {
        UserAgent userAgent = new UserAgent(CUSTOM_USER_AGENT_TITLE, CUSTOM_USER_AGENT_VERSION);
        UserAgentHolder.set(userAgent);

        SlingHttpResponse response = c.doGet(PATH, 200);
        assertTrue(response.containsHeader(USER_AGENT_HEADER));
        assertEquals(CUSTOM_USER_AGENT, response.getFirstHeader(USER_AGENT_HEADER).getValue());
    }

    @Test
    public void testCustomWithDetails() throws ClientException {
        UserAgent userAgent = new UserAgent(CUSTOM_USER_AGENT_TITLE, CUSTOM_USER_AGENT_VERSION);
        UserAgent userAgentDetails = new UserAgent(CUSTOM_USER_AGENT_DETAILS, ((String) null));
        UserAgentHolder.set(userAgent.appendDetails(userAgentDetails));

        SlingHttpResponse response = c.doGet(PATH, 200);
        assertTrue(response.containsHeader(USER_AGENT_HEADER));
        assertEquals(CUSTOM_USER_AGENT_WITH_DETAILS, response.getFirstHeader(USER_AGENT_HEADER).getValue());
    }

    @Test
    public void testCustomWithAppend() throws ClientException {
        UserAgent userAgent = new UserAgent(CUSTOM_USER_AGENT_TITLE, CUSTOM_USER_AGENT_VERSION);
        UserAgent userAgentDetails = new UserAgent(CUSTOM_USER_AGENT_DETAILS, ((String) null));
        UserAgentHolder.set(userAgent.append(userAgentDetails));

        SlingHttpResponse response = c.doGet(PATH, 200);
        assertTrue(response.containsHeader(USER_AGENT_HEADER));
        assertEquals(CUSTOM_USER_AGENT_WITH_APPEND, response.getFirstHeader(USER_AGENT_HEADER).getValue());
    }
}
