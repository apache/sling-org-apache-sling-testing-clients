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
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.IOException;

public class SlingClientDoGetWithRetryTest {

    public static String SUCCESS_RESPONSE = "Success";
    public static String FAILURE_RESPONSE = "Failure";

    @ClassRule
    public static HttpServerRule httpServer = new HttpServerRule() {
        int attempt = 0;

        @Override
        protected void registerHandlers() throws IOException {
            serverBootstrap.registerHandler("/", new HttpRequestHandler() {
                @Override
                public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
                    if (attempt > 2) {
                        response.setEntity(new StringEntity(SUCCESS_RESPONSE));
                    } else {
                        response.setEntity(new StringEntity(FAILURE_RESPONSE));
                    }
                    attempt++;
                }
            });
        }
    };

    @Test
    public void testDoGetWithRetry() throws ClientException {
        SlingClient c = new SlingClient(httpServer.getURI(), "user", "pass");
        c.doGetWithRetry("/", response -> Assert.assertEquals(SUCCESS_RESPONSE, response.getContent()), 1000, 100, 200);
    }
}
