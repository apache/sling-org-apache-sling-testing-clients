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
package org.apache.sling.testing.clients;

import org.apache.http.entity.StringEntity;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class SlingClientRetryStrategyTest {
    private static final String GET_UNAVAILABLE_PATH = "/test/unavailable/resource";
    private static final String GET_INEXISTENT_PATH = "/test/inexistent/resource";
    private static final String GET_INTERNAL_ERROR_PATH = "/test/internalerror/resource";
    private static final String NOK_RESPONSE = "TEST_NOK";
    private static final String OK_RESPONSE = "TEST_OK";

    private static final int MAX_RETRIES = 5;

    private static int requestCount = 0;
    private static int availableAtRequestCount = Integer.MAX_VALUE;

    static {
        System.setProperty(Constants.CONFIG_PROP_PREFIX + Constants.HTTP_LOG_RETRIES, "true");
        System.setProperty(Constants.CONFIG_PROP_PREFIX + Constants.HTTP_DELAY, "50");
        System.setProperty(Constants.CONFIG_PROP_PREFIX + Constants.HTTP_RETRIES, "5");
    }

    @ClassRule
    public static HttpServerRule httpServer = new HttpServerRule() {
        @Override
        protected void registerHandlers() throws IOException {
            serverBootstrap.registerHandler(GET_UNAVAILABLE_PATH, (request, response, context) -> {
                requestCount++;
                if (requestCount == availableAtRequestCount) {
                    response.setEntity(new StringEntity(OK_RESPONSE));
                    response.setStatusCode(200);
                } else {
                    response.setEntity(new StringEntity(NOK_RESPONSE));
                    response.setStatusCode(503);
                }
            });

            serverBootstrap.registerHandler(GET_INTERNAL_ERROR_PATH, (request, response, context) -> {
                requestCount++;
                if (requestCount == availableAtRequestCount) {
                    response.setEntity(new StringEntity(OK_RESPONSE));
                    response.setStatusCode(200);
                } else {
                    response.setEntity(new StringEntity(NOK_RESPONSE));
                    response.setStatusCode(500);
                }
            });

            serverBootstrap.registerHandler(GET_INEXISTENT_PATH, (request, response, context) -> {
                requestCount++;
                response.setEntity(new StringEntity(NOK_RESPONSE));
                response.setStatusCode(404);
            });
        }
    };

    @Test
    public void testRetryReallyUnavailable() throws Exception {
        requestCount = 0;
        availableAtRequestCount = Integer.MAX_VALUE; // never available
        SlingClient c = new SlingClient(httpServer.getURI(), "user", "pass");
        SlingHttpResponse slingHttpResponse = c.doGet(GET_UNAVAILABLE_PATH, 503);
        assertEquals(MAX_RETRIES + 1, requestCount);
        assertEquals(NOK_RESPONSE, slingHttpResponse.getContent());
    }

    @Test
    public void testRetryReallyInternalError() throws Exception {
        requestCount = 0;
        availableAtRequestCount = Integer.MAX_VALUE; // never available
        SlingClient c = new SlingClient(httpServer.getURI(), "user", "pass");
        SlingHttpResponse slingHttpResponse = c.doGet(GET_INTERNAL_ERROR_PATH, 500);
        assertEquals(MAX_RETRIES + 1, requestCount);
        assertEquals(NOK_RESPONSE, slingHttpResponse.getContent());
    }

    @Test
    public void testRetryInexistent() throws Exception {
        requestCount = 0;
        availableAtRequestCount = Integer.MAX_VALUE; // never available
        SlingClient c = new SlingClient(httpServer.getURI(), "user", "pass");
        SlingHttpResponse slingHttpResponse = c.doGet(GET_INEXISTENT_PATH, 404);
        // should not retry at all
        assertEquals(1, requestCount);
        assertEquals(NOK_RESPONSE, slingHttpResponse.getContent());
    }

    @Test
    public void testRetryEventuallyAvailable() throws Exception {
        requestCount = 0;
        availableAtRequestCount = 3;
        SlingClient c = new SlingClient(httpServer.getURI(), "user", "pass");
        SlingHttpResponse slingHttpResponse = c.doGet(GET_UNAVAILABLE_PATH, 200);
        assertEquals(availableAtRequestCount, requestCount);
        assertEquals(OK_RESPONSE, slingHttpResponse.getContent());

    }

    @Test
    public void testRetryEventuallyNoError() throws Exception {
        requestCount = 0;
        availableAtRequestCount = 3;
        SlingClient c = new SlingClient(httpServer.getURI(), "user", "pass");
        SlingHttpResponse slingHttpResponse = c.doGet(GET_INTERNAL_ERROR_PATH, 200);
        assertEquals(availableAtRequestCount, requestCount);
        assertEquals(OK_RESPONSE, slingHttpResponse.getContent());

    }

}
