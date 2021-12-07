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

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.sling.testing.clients.ClientException;

import java.util.List;

public class ServiceInfo {

    private JsonNode service;

    public ServiceInfo(JsonNode root) throws ClientException {
        if(root.get("id") != null) {
            service = root;
        } else {
            if(root.get("data") == null && root.get("data").size() < 1) {
                throw new ClientException("No service info returned");
            }
            service = root.get("data").get(0);
        }
    }

    /**
     * @return the service identifier
     */
    public int getId() {
        return Integer.parseInt(service.get("id").asText());
    }

    /**
     * @return the service types name
     */
    public List<String> getTypes() {
        // this is not a proper JSON array (https://issues.apache.org/jira/browse/FELIX-5762)
        return ServicesInfo.splitPseudoJsonValueArray(service.get("types").textValue());
    }

    public String getPid() {
        return service.get("pid").textValue();
    }

    /**
     * @return the bundle id of the bundle exposing the service
     */
    public int getBundleId() {
        return service.get("bundleId").intValue();
    }

    /**
     * @return the bundle symbolic name of bundle implementing the service
     */
    public String getBundleSymbolicName() {
        return service.get("bundleSymbolicName").textValue();
    }

}
