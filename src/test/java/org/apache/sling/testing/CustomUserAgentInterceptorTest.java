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
import org.apache.sling.testing.clients.*;
import org.apache.sling.testing.clients.interceptors.UserAgentHolder;
import org.apache.sling.testing.clients.interceptors.UserAgentInterceptor;
import org.junit.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CustomUserAgentInterceptorTest {

    private static final String PATH = "/mirror";

    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final String MODIFIED_AGENT = "modified-agent";
    private static final String CUSTOM_AGENT = "test-client";

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
    private static SlingClient client;

    @BeforeClass
    public static void beforeClass() throws ClientException {
        client = createClient();
    }

    @After
    public void after() {
        UserAgentHolder.reset();
    }

    @Test
    public void testDefault() throws ClientException {
        assertUserAgent(client, SystemPropertiesConfig.getDefaultUserAgent());
    }

    @Test
    public void testCustom() throws ClientException {
        UserAgentHolder.set(CUSTOM_AGENT);
        assertUserAgent(client, CUSTOM_AGENT);
    }

    @Test
    public void testManualModify() throws ClientException {
        SlingClient clientModified = createClientWithBakedInUserAgent(MODIFIED_AGENT);
        UserAgentHolder.set(CUSTOM_AGENT);
        assertUserAgent(clientModified, MODIFIED_AGENT);
    }

    @Test
    public void testWhitespace() throws ClientException {
        UserAgentHolder.set(" ");
        assertUserAgent(client, SystemPropertiesConfig.getDefaultUserAgent());
    }

    /**
     * Sends a dummy request to the test-server to see if the response contains a specified user-agent header
     * to assert whether the requests contained the user-agent as well.
     * @param client the {@link SlingClient} to be used for sending the request
     * @param userAgent the expected user-agent as a string
     * @throws ClientException in case of request failure
     */
    private static void assertUserAgent(SlingClient client, String userAgent) throws ClientException {
        SlingHttpResponse response = client.doGet(PATH, 200);
        assertTrue(response.containsHeader(USER_AGENT_HEADER));
        assertEquals(userAgent, response.getFirstHeader(USER_AGENT_HEADER).getValue());
    }

    /**
     * Creates a simple {@link SlingClient} with the {@link UserAgentInterceptor}.
     * @return {@link SlingClient} instance
     * @throws ClientException in case of failure during client creation
     */
    private static SlingClient createClient() throws ClientException {
        return createClientWithBakedInUserAgent(null);
    }

    /**
     * Creates a simple {@link SlingClient} with the {@link UserAgentInterceptor} and a manually baked-in user-agent.
     * @param userAgent user-agent of the client as a {@link String} (or null to omit it)
     * @return {@link SlingClient} instance
     * @throws ClientException in case of failure during client creation
     */
    private static SlingClient createClientWithBakedInUserAgent(String userAgent) throws ClientException {
        SlingClient.InternalBuilder<SlingClient> builder = SlingClient.Builder
                .create(httpServer.getURI(), "user", "pass")
                .addInterceptorLast(new UserAgentInterceptor());

        if (userAgent != null) {
            builder.httpClientBuilder().setUserAgent(MODIFIED_AGENT);
        }

        return builder.build();
    }
}
