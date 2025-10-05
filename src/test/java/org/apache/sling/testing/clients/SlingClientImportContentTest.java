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
package org.apache.sling.testing.clients;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.IOUtils;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.HttpRequestHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.hc.core5.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.hc.core5.http.HttpStatus.SC_CREATED;

public class SlingClientImportContentTest {
    private static final Logger LOG = LoggerFactory.getLogger(SlingClientImportContentTest.class);

    private static final String IMPORT_PATH = "/test/import/parent";
    private static final String IMPORT_FILE_PATH = "/content/importfile";

    private static final String IMPORT_FILE_CONTENT = "{\"nodefromfile\":{\"prop1\":\"val1\"}}";

    @ClassRule
    public static HttpServerRule httpServer = new HttpServerRule() {
        @Override
        protected void registerHandlers() throws IOException {
            serverBootstrap.register(IMPORT_PATH, new HttpRequestHandler() {
                @Override
                public void handle(ClassicHttpRequest request, ClassicHttpResponse response, HttpContext context)
                        throws HttpException, IOException {
                    List<NameValuePair> params = extractParameters(request);
                    String operation = getParameter(":operation", params);
                    String content = getParameter(":content", params);

                    if (!"import".equals(operation)) {
                        response.setCode(SC_BAD_REQUEST);
                        response.setEntity(new StringEntity("Unexpected operation: " + operation));
                        return;
                    }

                    if (!"{\"something\":{\"prop1\":\"val1\"}}".equals(content)) {
                        response.setCode(SC_BAD_REQUEST);
                        response.setEntity(new StringEntity("Unexpected content: " + content));
                        return;
                    }

                    response.setCode(SC_CREATED);
                }
            });

            serverBootstrap.register(IMPORT_FILE_PATH, new HttpRequestHandler() {
                @Override
                public void handle(ClassicHttpRequest request, ClassicHttpResponse response, HttpContext context)
                        throws HttpException, IOException {
                    LOG.debug("received: {}", request);
                    if (request.getEntity() != null) {
                        HttpEntity entity = request.getEntity();
                        String content = IOUtils.toString(entity.getContent(), UTF_8);
                        LOG.debug("content: {}", content);

                        if (!content.contains(":operation") || !content.contains("import")) {
                            response.setCode(SC_BAD_REQUEST);
                            response.setEntity(new StringEntity("Operation not found"));
                            return;
                        } else if (!content.contains(IMPORT_FILE_CONTENT)) {
                            response.setCode(SC_BAD_REQUEST);
                            response.setEntity(new StringEntity("File content not found"));
                            return;
                        }

                        response.setCode(SC_CREATED);
                    } else {
                        response.setCode(SC_BAD_REQUEST);
                        response.setEntity(new StringEntity("Request doesn't contain an entity"));
                    }
                }
            });
        }

        private List<NameValuePair> extractParameters(ClassicHttpRequest request) {
            if (request.getEntity() != null) {
                HttpEntity entity = request.getEntity();
                try {
                    return EntityUtils.parse(entity);
                } catch (IOException e) {
                    LOG.error("Failed to parse entity", e);
                }
            }

            return new ArrayList<>();
        }

        private String getParameter(String parameterName, List<NameValuePair> parameters) {
            for (NameValuePair parameter : parameters) {
                if (parameter.getName().equals(parameterName)) {
                    return parameter.getValue();
                }
            }

            return null;
        }
    };

    private SlingClient client;

    public SlingClientImportContentTest() throws ClientException {
        client = new SlingClient(httpServer.getURI(), "user", "pass");
        // to use with an already running instance
        // client = new SlingClient(java.net.URI.create("http://localhost:8080"), "admin", "admin");
    }

    @Test
    public void testImportContent() throws Exception {
        client.importContent(IMPORT_PATH, "json", "{\"something\":{\"prop1\":\"val1\"}}");
    }

    @Test
    public void testImportJson() throws Exception {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        ObjectNode props = JsonNodeFactory.instance.objectNode();
        props.put("prop1", "val1");

        node.put("something", props);
        client.importJson(IMPORT_PATH, node);
    }

    @Test
    public void testImportContentFile() throws Exception {
        File tmp = File.createTempFile("import-json", null);
        LOG.debug("created: " + tmp);
        PrintWriter pw = new PrintWriter(tmp);
        pw.write(IMPORT_FILE_CONTENT);
        pw.close();

        client.importContent(IMPORT_FILE_PATH, "json", tmp);
    }
}
