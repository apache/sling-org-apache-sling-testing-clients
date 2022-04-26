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

import java.io.IOException;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.sling.testing.clients.util.FormEntityBuilder;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SlingClientDoPostTest {
    private static final String POST_PATH = "/test/a/b/c";
    private static final String HTML_RESPONSE = "<div id=\"Path\">/a/b/c</div>";

    @ClassRule
    public static HttpServerRule httpServer = new HttpServerRule() {
        @Override
        protected void registerHandlers() throws IOException {
            serverBootstrap.registerHandler(POST_PATH, new HttpRequestHandler() {
                @Override
                public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
                    response.setEntity(new StringEntity(HTML_RESPONSE));
                }
            });
        }
    };

    @Test
    public void testDoPostGetSlingPath() throws Exception {
        SlingClient c = new SlingClient(httpServer.getURI(), "user", "pass");
        SlingHttpResponse res = c.doPost(POST_PATH, FormEntityBuilder.create().build(), 200);
        assertEquals("/a/b/c", res.getSlingPath());
    }

}
