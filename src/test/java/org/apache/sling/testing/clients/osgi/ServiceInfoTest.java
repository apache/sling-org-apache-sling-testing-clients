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

import com.google.common.io.Resources;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.util.JsonUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class ServiceInfoTest {

    @Test
    public void testSpecifyServiceIdentifierAsString() throws IOException, ClientException {
        assertServiceInfo("service-id-as-string");
    }

    @Test
    public void testSpecifyServiceIdentifierAsInteger() throws IOException, ClientException {
        assertServiceInfo("service-id-as-int");
    }

    private void assertServiceInfo(final String file) throws IOException, ClientException {
        final ServiceInfo serviceInfo = new ServiceInfo(JsonUtils.getJsonNodeFromString(
                Resources.toString(Resources.getResource("service-info/" + file + ".json"), StandardCharsets.UTF_8)));
        assertEquals(10, serviceInfo.getId());
        assertEquals("org.example.MyService", serviceInfo.getPid());
        assertEquals(6, serviceInfo.getBundleId());
        assertEquals(Collections.singletonList("org.example.MyService"), serviceInfo.getTypes());
        assertEquals("org.example", serviceInfo.getBundleSymbolicName());
    }
}