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
package org.apache.sling.testing.clients.osgi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.HttpServerRule;
import org.apache.sling.testing.clients.util.JsonUtils;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class OsgiConsoleClientTest {

    @Test
    public void testExtractOSGiConfigurationNoConfig() throws ClientException {
        final String jsonResult = "{\"pid\":\"org.apache.sling.Factory\","
                + "\"title\":\"Factory\","
                + "\"description\":\"A factory\","
                + "\"properties\":{"
                + "\"prop\":{\"name\":\"Prop\",\"optional\":false,\"is_set\":false,\"type\":1,\"values\":[\"a\",\"b\"],"
                + "\"description\":\"A property\"}}}";

        final JsonNode rootNode = JsonUtils.getJsonNodeFromString(jsonResult);

        // configuration does not exist, expect null
        assertNull(OsgiConsoleClient.extractOSGiConfiguration(rootNode));
    }

    @Test
    public void testExtractOSGiConfiguration() throws ClientException {
        final String jsonResult = "{\"pid\":\"org.apache.sling.Factory\","
                + "\"title\":\"Factory\","
                + "\"description\":\"A factory\","
                + "\"properties\":{"
                + "\"propset\":{\"name\":\"Prop\",\"optional\":false,\"is_set\":true,\"type\":1,\"value\":\"a\","
                + "\"description\":\"A property\"},"
                + "\"prop\":{\"name\":\"Prop\",\"optional\":false,\"is_set\":false,\"type\":1,\"values\":[\"a\",\"b\"],"
                + "\"description\":\"A property\"}"
                + "},\"bundleLocation\":\"\",\"bundle_location\":null,\"service_location\":\"\"}";

        final JsonNode rootNode = JsonUtils.getJsonNodeFromString(jsonResult);

        // one property is set
        final Map<String, Object> result = OsgiConsoleClient.extractOSGiConfiguration(rootNode);
        assertEquals(1, result.size());
        assertEquals("a", result.get("propset"));
    }

    @ClassRule
    public static HttpServerRule httpServer = new HttpServerRule() {
        @Override
        protected void registerHandlers() throws IOException {
            String JSON_RESPONSE = "[{\"pid\":\"pidValue\",\"properties\":{\"testPropertyName\":{\"value\":\"testPropertyValue\"}}}]";
            serverBootstrap.registerHandler("/system/console/configMgr/*.json", new HttpRequestHandler() {
                @Override
                public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
                    response.setEntity(new StringEntity(JSON_RESPONSE));
                }
            });
        }
    };

    @Test
    public void testGetConfigPIDFromServices() throws ClientException, InterruptedException, TimeoutException {
        OsgiConsoleClient c = new OsgiConsoleClient(httpServer.getURI(),"user","pass");
        String pid = c.getConfigPIDFromServices("testServiceType","testPropertyName", "testPropertyValue",1000, 200);
        assertEquals("pidValue", pid);
    }
}
