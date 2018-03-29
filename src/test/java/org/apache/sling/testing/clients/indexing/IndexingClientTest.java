/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ******************************************************************************/
package org.apache.sling.testing.clients.indexing;

import org.apache.http.*;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.HttpServerRule;
import org.apache.sling.testing.clients.query.servlet.QueryServlet;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class IndexingClientTest {
    private static final Logger LOG = LoggerFactory.getLogger(IndexingClientTest.class);

    private static final String EXPLAIN_RESPONSE = "{\"plan\": \"random plan with testIndexingLane-async and testIndexingLane-fulltext-async\",\"time\": 1}";
    private static final String QUERY_RESPONSE = "{\"total\": 1234,\"time\": 1}";

    @ClassRule
    public static HttpServerRule httpServer = new HttpServerRule() {
        HttpRequestHandler okHandler =  new HttpRequestHandler() {
            @Override
            public void handle(HttpRequest request, HttpResponse response, HttpContext context)
                    throws HttpException, IOException {
                response.setStatusCode(200);
                response.setEntity(new StringEntity("Everything's fine"));
            }
        };

        HttpRequestHandler createdHandler =  new HttpRequestHandler() {
            @Override
            public void handle(HttpRequest request, HttpResponse response, HttpContext context)
                    throws HttpException, IOException {
                response.setStatusCode(201);
                response.setEntity(new StringEntity("Created"));
            }
        };

        @Override
        protected void registerHandlers() throws IOException {
            // Normal query request
            serverBootstrap.registerHandler(QueryServlet.SERVLET_PATH, new HttpRequestHandler() {
                @Override
                public void handle(HttpRequest request, HttpResponse response, HttpContext context)
                        throws HttpException, IOException {
                    List<NameValuePair> parameters = URLEncodedUtils.parse(
                            request.getRequestLine().getUri(), Charset.defaultCharset());

                    for (NameValuePair parameter : parameters) {
                        if (parameter.getName().equals("explain") && !parameter.getValue().equals("false")) {
                            response.setEntity(new StringEntity(EXPLAIN_RESPONSE));
                            return;
                        }
                    }

                    response.setEntity(new StringEntity(QUERY_RESPONSE));
                }
            });

            // Install servlet
            serverBootstrap.registerHandler("/system/console/bundles", new HttpRequestHandler() {
                @Override
                public void handle(HttpRequest request, HttpResponse response, HttpContext context)
                        throws HttpException, IOException {
                    // is install (post) or checking status (get)
                    if (request instanceof BasicHttpEntityEnclosingRequest) {
                        response.setStatusCode(302);
                    } else {
                        response.setStatusCode(200);
                    }
                }
            });

            // Check bundle status
            serverBootstrap.registerHandler("BUNDLE_PATH" + ".json", new HttpRequestHandler() {
                @Override
                public void handle(HttpRequest request, HttpResponse response, HttpContext context)
                        throws HttpException, IOException {
                    response.setEntity(new StringEntity("JSON_BUNDLE"));
                }
            });

            // Uninstall bundle
            serverBootstrap.registerHandler("BUNDLE_PATH", new HttpRequestHandler() {
                @Override
                public void handle(HttpRequest request, HttpResponse response, HttpContext context)
                        throws HttpException, IOException {
                    response.setStatusCode(200);
                }
            });

            // Uninstall bundle
            serverBootstrap.registerHandler(
                    "/system/console/configMgr/org.apache.jackrabbit.oak.plugins.index.AsyncIndexerService",
                    new HttpRequestHandler() {
                        @Override
                        public void handle(HttpRequest request, HttpResponse response, HttpContext context)
                                throws HttpException, IOException {
                            response.setStatusCode(200);
                            response.setEntity(new StringEntity("{\"properties\":{" +
                                    "\"asyncConfigs\":{\"values\":[\"async:5\",\"fulltext-async:5\"]}}}"));
                        }
                    }
            );

            serverBootstrap.registerHandler("/tmp/testing/waitForAsyncIndexing/content/*", new HttpRequestHandler() {
                @Override
                public void handle(HttpRequest request, HttpResponse response, HttpContext context)
                        throws HttpException, IOException {
                    List<NameValuePair> params = extractParameters(request);

                    for (NameValuePair param : params) {
                        if (param.getName().equals(":operation") && (param.getValue().equals("delete"))) {
                            response.setStatusCode(200);
                            return;
                        }
                    }

                    response.setStatusCode(201);
                    response.setEntity(new StringEntity("Created!"));
                }
            });

            serverBootstrap.registerHandler("/tmp/testing/waitForAsyncIndexing/oak:index/*", new HttpRequestHandler() {
                @Override
                public void handle(HttpRequest request, HttpResponse response, HttpContext context)
                        throws HttpException, IOException {
                    List<NameValuePair> params = extractParameters(request);

                    for (NameValuePair param : params) {
                        if (param.getName().equals(":operation") && (param.getValue().equals("delete"))) {
                            response.setStatusCode(200);
                            return;
                        }
                    }

                    response.setStatusCode(200);
                    response.setEntity(new StringEntity("Created!"));
                }
            });

            // unimportant requests
            serverBootstrap.registerHandler("/tmp.json", okHandler);
            serverBootstrap.registerHandler("/tmp/testing.json", okHandler);
            serverBootstrap.registerHandler("/tmp/testing/waitForAsyncIndexing", okHandler);
            serverBootstrap.registerHandler("/tmp/testing", okHandler);
            serverBootstrap.registerHandler("/tmp/testing/waitForAsyncIndexing/oak:index", createdHandler);
            serverBootstrap.registerHandler("/tmp/testing/waitForAsyncIndexing/content", createdHandler);
        }
    };

    private IndexingClient client;

    public IndexingClientTest() throws ClientException {
        client = new IndexingClient(httpServer.getURI(), "admin", "admin");
        //client = new IndexingClient(java.net.URI.create("http://localhost:4502"), "admin", "admin");
    }

    @Test
    public void testInstall() throws ClientException {
        client.install();
    }

    @Test
    public void testUninstall() throws ClientException {
        client.uninstall();
    }

    @Test
    public void testWaitForAsyncIndexing() throws ClientException, TimeoutException, InterruptedException {
        client.waitForAsyncIndexing();
    }

    private static List<NameValuePair> extractParameters(HttpRequest request) {
        if (request instanceof HttpEntityEnclosingRequest) {
            HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
            try {
                return URLEncodedUtils.parse(entity);
            } catch (IOException e) {
                LOG.error("Failed to parse entity", e);
            }
        }

        return new ArrayList<>();
    }
}
