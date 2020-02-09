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

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SlingClientRetryServiceUnavailableTest {
    private static final String GET_UNAVAILABLE_PATH = "/test/unavailable/resource";
    private static final String NOK_RESPONSE = "TEST_NOK";
    private static final String OK_RESPONSE = "TEST_OK";

    private static final int MAX_RETRIES = 5;

    private static int requestCount = 0;
    private static int availableAtRequestCount = Integer.MAX_VALUE;

    static {
        System.setProperty(Constants.CONFIG_PROP_PREFIX + "http.logRetries", "true");
    }

    @ClassRule
    public static HttpServerRule httpServer = new HttpServerRule() {
        @Override
        protected void registerHandlers() throws IOException {
            serverBootstrap.registerHandler(GET_UNAVAILABLE_PATH, new HttpRequestHandler() {
                @Override
                public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
                    requestCount++;
                    if (requestCount == availableAtRequestCount) {
                        response.setEntity(new StringEntity(OK_RESPONSE));
                        response.setStatusCode(200);
                    } else {
                        response.setEntity(new StringEntity(NOK_RESPONSE));
                        response.setStatusCode(503);
                    }
                }
            });
        }
    };

    @Test
    public void testRetryReallyUnavailable() throws Exception {
        requestCount = 0;
        availableAtRequestCount = Integer.MAX_VALUE; // never available
        SlingClient c = new SlingClient(httpServer.getURI(), "user", "pass");
        SlingHttpResponse slingHttpResponse = c.doGet(GET_UNAVAILABLE_PATH, 503, 10);
        assertEquals(MAX_RETRIES + 1, requestCount);
        assertEquals(NOK_RESPONSE, slingHttpResponse.getContent());
    }

    @Test
    public void testRetryEventuallyAvailable() throws Exception {
        requestCount = 0;
        availableAtRequestCount = 3;
        SlingClient c = new SlingClient(httpServer.getURI(), "user", "pass");
        SlingHttpResponse slingHttpResponse = c.doGet(GET_UNAVAILABLE_PATH, 200, 10);
        assertEquals(availableAtRequestCount, requestCount);
        assertEquals(OK_RESPONSE, slingHttpResponse.getContent());

    }

}
