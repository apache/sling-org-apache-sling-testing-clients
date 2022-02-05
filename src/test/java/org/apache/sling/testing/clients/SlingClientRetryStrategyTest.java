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
import org.apache.http.protocol.HttpRequestHandler;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.IOException;

import static junit.framework.TestCase.assertFalse;
import static org.apache.sling.testing.clients.SystemPropertiesConfig.CONFIG_PROP_PREFIX;
import static org.apache.sling.testing.clients.SystemPropertiesConfig.HTTP_RETRIES_ERROR_CODES_PROP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class SlingClientRetryStrategyTest {
    private static final String GET_UNAVAILABLE_PATH = "/test/unavailable/resource";
    private static final String GET_INEXISTENT_PATH = "/test/inexistent/resource";
    private static final String GET_INTERNAL_ERROR_PATH = "/test/internalerror/resource";
    private static final String GET_505_PATH = "/test/unsupportedversion/resource";
    private static final String NOK_RESPONSE = "TEST_NOK";
    private static final String OK_RESPONSE = "TEST_OK";
    private static final String JSON_EXT = ".json";

    private static final int MAX_RETRIES = 4;

    private static int requestCount = 0;
    private static int availableAtRequestCount = Integer.MAX_VALUE;

    @Before
    public void defaultSetup() {
        System.clearProperty(CONFIG_PROP_PREFIX + HTTP_RETRIES_ERROR_CODES_PROP);
        System.setProperty(CONFIG_PROP_PREFIX + SystemPropertiesConfig.HTTP_LOG_RETRIES_PROP, "true");
        System.setProperty(CONFIG_PROP_PREFIX + SystemPropertiesConfig.HTTP_DELAY_PROP, "50");
        System.setProperty(CONFIG_PROP_PREFIX + SystemPropertiesConfig.HTTP_RETRIES_PROP, "4");
    }

    @ClassRule
    public static HttpServerRule httpServer = new HttpServerRule() {
        @Override
        protected void registerHandlers() {
            final HttpRequestHandler unavailableHandler = (request, response, context) -> {
                requestCount++;
                if (requestCount == availableAtRequestCount) {
                    response.setEntity(new StringEntity(OK_RESPONSE));
                    response.setStatusCode(200);
                } else {
                    response.setEntity(new StringEntity(NOK_RESPONSE));
                    response.setStatusCode(503);
                }
            };
            serverBootstrap.registerHandler(GET_UNAVAILABLE_PATH, unavailableHandler);
            serverBootstrap.registerHandler(GET_UNAVAILABLE_PATH + JSON_EXT, unavailableHandler);

            final HttpRequestHandler internalErrorHandler = (request, response, context) -> {
                requestCount++;
                if (requestCount == availableAtRequestCount) {
                    response.setEntity(new StringEntity(OK_RESPONSE));
                    response.setStatusCode(200);
                } else {
                    response.setEntity(new StringEntity(NOK_RESPONSE));
                    response.setStatusCode(500);
                }
            };
            serverBootstrap.registerHandler(GET_INTERNAL_ERROR_PATH, internalErrorHandler);
            serverBootstrap.registerHandler(GET_INTERNAL_ERROR_PATH + JSON_EXT, internalErrorHandler);

            final HttpRequestHandler notFoundHandler = (request, response, context) -> {
                requestCount++;
                response.setEntity(new StringEntity(NOK_RESPONSE));
                response.setStatusCode(404);
            };
            serverBootstrap.registerHandler(GET_INEXISTENT_PATH, notFoundHandler);
            serverBootstrap.registerHandler(GET_INEXISTENT_PATH + JSON_EXT, notFoundHandler);

            serverBootstrap.registerHandler(GET_505_PATH, (request, response, context) -> {
                requestCount++;
                if (requestCount == availableAtRequestCount) {
                    response.setEntity(new StringEntity(OK_RESPONSE));
                    response.setStatusCode(200);
                } else {
                    response.setEntity(new StringEntity(NOK_RESPONSE));
                    response.setStatusCode(505);
                }
            });
        }
    };

    @Test()
    public void testRetryReallyUnavailable() throws Exception {
        requestCount = 0;
        availableAtRequestCount = Integer.MAX_VALUE; // never available
        SlingClient c = new SlingClient(httpServer.getURI(), "user", "pass");
        try {
            c.doGet(GET_UNAVAILABLE_PATH, 200);
        } catch(ClientException err) {
            assertThat(err.getMessage(),CoreMatchers.containsString("Expected HTTP Status: 200 . Instead 503 was returned!"));
        }
        assertEquals(MAX_RETRIES + 1, requestCount);
    }

    @Test()
    public void testRetryReallyInternalError() throws Exception {
        requestCount = 0;
        availableAtRequestCount = Integer.MAX_VALUE; // never available
        SlingClient c = new SlingClient(httpServer.getURI(), "user", "pass");
        try {
            c.doGet(GET_INTERNAL_ERROR_PATH, 200);
        } catch (ClientException err) {
            assertThat(err.getMessage(),CoreMatchers.containsString("Expected HTTP Status: 200 . Instead 500 was returned!"));
        }
        assertEquals(MAX_RETRIES + 1, requestCount);
    }

    @Test
    public void test505ShouldNotRetry() throws Exception {
        System.setProperty(CONFIG_PROP_PREFIX + HTTP_RETRIES_ERROR_CODES_PROP, "500,503");
        requestCount = 0;
        availableAtRequestCount = Integer.MAX_VALUE; // never 200
        SlingClient c = new SlingClient(httpServer.getURI(), "user", "pass");
        SlingHttpResponse slingHttpResponse = c.doGet(GET_505_PATH, 505);
        assertEquals(1, requestCount);
        assertEquals(NOK_RESPONSE, slingHttpResponse.getContent());
    }

    @Test
    public void testExpectedStatusShouldNotRetry() throws Exception {
        requestCount = 0;
        availableAtRequestCount = Integer.MAX_VALUE; // never 200
        SlingClient c = new SlingClient(httpServer.getURI(), "user", "pass");
        SlingHttpResponse slingHttpResponse = c.doGet(GET_505_PATH, 505);
        assertEquals(1, requestCount);
        assertEquals(NOK_RESPONSE, slingHttpResponse.getContent());
    }

    @Test
    public void test505ShouldRetry() throws Exception {
        System.setProperty(CONFIG_PROP_PREFIX + HTTP_RETRIES_ERROR_CODES_PROP, "500,503,505");
        requestCount = 0;
        availableAtRequestCount = 3;
        SlingClient c = new SlingClient(httpServer.getURI(), "user", "pass");
        SlingHttpResponse slingHttpResponse = c.doGet(GET_505_PATH, 200);
        assertEquals(availableAtRequestCount, requestCount);
        assertEquals(OK_RESPONSE, slingHttpResponse.getContent());
    }

    @Test
    public void testNotExist404ShouldNotRetry() throws Exception {
        System.setProperty(CONFIG_PROP_PREFIX + HTTP_RETRIES_ERROR_CODES_PROP, "404");
        requestCount = 0;
        availableAtRequestCount = Integer.MAX_VALUE; // never available
        SlingClient c = new SlingClient(httpServer.getURI(), "user", "pass");
        assertFalse(c.exists(GET_INEXISTENT_PATH));
        assertEquals(1, requestCount);
    }

    @Test
    public void testNotExist500ShouldRetry() throws Exception {
        requestCount = 0;
        availableAtRequestCount = Integer.MAX_VALUE; // never available
        SlingClient c = new SlingClient(httpServer.getURI(), "user", "pass");
        assertFalse(c.exists(GET_INTERNAL_ERROR_PATH));
        assertEquals(MAX_RETRIES + 1, requestCount);
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
