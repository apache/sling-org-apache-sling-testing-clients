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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * A simple Wrapper around the returned JSON when requesting the status of /system/console/services
 */
public class ServicesInfo {

    private JsonNode root = null;

    /**
     * The only constructor.
     *
     * @param root the root JSON node of the bundles info.
     * @throws ClientException if the json does not contain the proper info
     */
    public ServicesInfo(JsonNode root) throws ClientException {
        this.root = root;
        // some simple sanity checks
        if (root.get("status") == null)
            throw new ClientException("No Status returned!");
        if (root.get("serviceCount") == null)
            throw new ClientException("No serviceCount returned!");
    }

    /**
     * @return total number of bundles.
     */
    public int getTotalNumOfServices() {
        return root.get("serviceCount").intValue();
    }

    /**
     * Return service info for a service with given id
     *
     * @param id the id of the service
     * @return the BundleInfo
     * @throws ClientException if the info could not be retrieved
     */
    public ServiceInfo forId(String id) throws ClientException {
        JsonNode serviceInfo = findBy("id", id);
        return (serviceInfo != null) ? new ServiceInfo(serviceInfo) : null;
    }

    /**
     * Return service infos for a bundle with name {@code name}
     *
     * @param type the type of the service
     * @return a Collection of {@link ServiceInfo}s of all services with the given type. Might be empty, never {@code null}
     * @throws ClientException if the info cannot be retrieved
     */
    public Collection<ServiceInfo> forType(String type) throws ClientException {
        List<ServiceInfo> results = new LinkedList<>();
        List<JsonNode> serviceInfoNodes = findAllContainingValueInArray("types", type);
        for (JsonNode serviceInfoNode : serviceInfoNodes) {
            results.add(new ServiceInfo(serviceInfoNode));
        }
        return results;
    }

    private JsonNode findBy(String key, String value) {
        List<JsonNode> result = findBy(key, value, true, false);
        if (result.isEmpty()) {
            return null;
        } else {
            return result.get(0);
        }
    }

    private List<JsonNode> findAllContainingValueInArray(String key, String value) {
        return findBy(key, value, false, true);
    }

    private List<JsonNode> findBy(String key, String value, boolean onlyReturnFirstMatch, boolean arrayContainingMatch) {
        Iterator<JsonNode> nodes = root.get("data").elements();
        List<JsonNode> results = new LinkedList<>();
        while (nodes.hasNext()) {
            JsonNode node = nodes.next();
            if ((null != node.get(key)) && (node.get(key).isValueNode())) {
                final String valueNode = node.get(key).textValue();
                if (arrayContainingMatch) {
                    if (splitPseudoJsonValueArray(valueNode).contains(value)) {
                        results.add(node);
                    }
                } else {
                    if (valueNode.equals(value)) {
                        results.add(node);
                    }
                }
            }
        }
        return results;
    }

    /**
     * Array values are not returned as proper JSON array for Apache Felix.
     * Therefore we need this dedicated split method, which extracts the individual values from this "pseudo" JSON array.
     * Example value:
     * <pre>
     * [java.lang.Runnable, org.apache.sling.event.impl.jobs.queues.QueueManager, org.osgi.service.event.EventHandler]
     * </pre>
     *
     * @param value the value to split
     * @return the list of the individual values in the given array.
     * @see <a href="https://issues.apache.org/jira/browse/FELIX-5762">FELIX-5762</a>
     */
    static final List<String> splitPseudoJsonValueArray(String value) {
        // is this an array?
        if (value.startsWith("[") && value.length() >= 2) {
            // strip of first and last character
            String pureArrayValues = value.substring(1, value.length() - 1);
            String[] resultArray = pureArrayValues.split(", |,");
            return Arrays.asList(resultArray);
        }
        return Collections.singletonList(value);
    }
}