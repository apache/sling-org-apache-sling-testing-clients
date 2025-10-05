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
package org.apache.sling.testing.clients.query;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.HttpRequestHandler;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.net.URLEncodedUtils;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.HttpServerRule;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryClientTest {
    private static final Logger LOG = LoggerFactory.getLogger(QueryClientTest.class);

    private static final String QUERY_PATH = "/system/testing/query"; // same as in QueryServlet
    private static final String BUNDLE_PATH = "/system/console/bundles/org.apache.sling.testing.clients.query";
    private static final String QUERY_RESPONSE = "{\"total\": 1234,\"time\": 1}";
    private static final String EXPLAIN_RESPONSE = "{\"plan\": \"some plan\",\"time\": 1}";
    private static final String JSON_BUNDLE = "{\n"
            + "  \"status\": \"Bundle information: 546 bundles in total, 537 bundles active, 8 bundles active fragments, 1 bundle resolved.\",\n"
            + "  \"s\": [\n"
            + "    546,\n"
            + "    537,\n"
            + "    8,\n"
            + "    1,\n"
            + "    0\n"
            + "  ],\n"
            + "  \"data\": [\n"
            + "    {\n"
            + "      \"id\": 560,\n"
            + "      \"name\": \"Query servlet for testing\",\n"
            + "      \"fragment\": false,\n"
            + "      \"stateRaw\": 32,\n"
            + "      \"state\": \"Active\",\n"
            + "      \"version\": \"1.0.0\",\n"
            + "      \"symbolicName\": \"org.apache.sling.testing.clients.query\",\n"
            + "      \"category\": \"\"\n"
            + "    }\n"
            + "  ]\n"
            + "}";

    @ClassRule
    public static HttpServerRule httpServer = new HttpServerRule() {
        @Override
        protected void registerHandlers() throws IOException {

            // Normal query request
            serverBootstrap.register(QUERY_PATH, new HttpRequestHandler() {
                @Override
                public void handle(ClassicHttpRequest request, ClassicHttpResponse response, HttpContext context)
                        throws HttpException, IOException {
                    List<NameValuePair> parameters =
                            URLEncodedUtils.parse(request.getRequestUri(), Charset.defaultCharset());

                    for (NameValuePair parameter : parameters) {
                        if (parameter.getName().equals("explain")
                                && !parameter.getValue().equals("false")) {
                            response.setEntity(new StringEntity(EXPLAIN_RESPONSE));
                            return;
                        }
                    }

                    response.setEntity(new StringEntity(QUERY_RESPONSE));
                }
            });

            // Install servlet
            serverBootstrap.register("/system/console/bundles", new HttpRequestHandler() {
                @Override
                public void handle(ClassicHttpRequest request, ClassicHttpResponse response, HttpContext context)
                        throws HttpException, IOException {
                    // is install (post) or checking status (get)
                    if (HttpPost.METHOD_NAME.equals(request.getMethod())) {
                        response.setCode(302);
                    } else {
                        response.setCode(200);
                    }
                }
            });

            // Check bundle status
            serverBootstrap.register(BUNDLE_PATH + ".json", new HttpRequestHandler() {
                @Override
                public void handle(ClassicHttpRequest request, ClassicHttpResponse response, HttpContext context)
                        throws HttpException, IOException {
                    response.setEntity(new StringEntity(JSON_BUNDLE));
                }
            });

            // Uninstall bundle
            serverBootstrap.register(BUNDLE_PATH, new HttpRequestHandler() {
                @Override
                public void handle(ClassicHttpRequest request, ClassicHttpResponse response, HttpContext context)
                        throws HttpException, IOException {
                    response.setCode(200);
                }
            });
        }
    };

    private static QueryClient client;

    public QueryClientTest() throws ClientException {
        client = new QueryClient(httpServer.getURI(), "admin", "admin");
        // for testing an already running instance
        // client = new QueryClient(java.net.URI.create("http://localhost:8080"), "admin", "admin");
    }

    @Test
    public void testInstallServlet() throws ClientException, InterruptedException {
        client.installServlet();
    }

    @Test
    public void testDoQuery() throws ClientException, InterruptedException {
        JsonNode response = client.doQuery(
                "SELECT * FROM [nt:file] WHERE ISDESCENDANTNODE([/etc/])",
                //        JsonNode response = client.doQuery("SELECT * FROM [cq:Tag] WHERE ISDESCENDANTNODE([/etc/])",
                QueryClient.QueryType.SQL2);
        LOG.info(response.toString());
        Assert.assertNotEquals(0, response.get("total").longValue());
    }

    @Test
    public void testDoCount() throws ClientException, InterruptedException {
        long results =
                client.doCount("SELECT * FROM [nt:file] WHERE ISDESCENDANTNODE([/etc/])", QueryClient.QueryType.SQL2);
        LOG.info("results={}", results);
        Assert.assertNotEquals(0, results);
    }

    @Test
    public void testGetPlan() throws ClientException, InterruptedException {
        String plan =
                client.getPlan("SELECT * FROM [nt:file] WHERE ISDESCENDANTNODE([/etc/])", QueryClient.QueryType.SQL2);
        LOG.info("plan={}", plan);
        Assert.assertNotEquals("", plan);
    }

    @Test
    public void testUninstallServlet() throws ClientException {
        client.uninstallServlet();
    }
}
