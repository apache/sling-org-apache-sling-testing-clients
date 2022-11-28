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
package org.apache.sling.testing.clients.interceptors;

import org.apache.commons.lang3.StringUtils;

public class UserAgentHolder {

    private UserAgentHolder() {}

    private static final ThreadLocal<String> userAgent = new ThreadLocal<>();

    /**
     * Returns the current user-agent.
     * @return the current user-agent
     */
    public static String get() {
        return userAgent.get();
    }

    /**
     * Override the current user-agent with a completely new one.
     * @param agent the desired new user-agent (or null for default)
     */
    public static void set(String agent) {
        if (StringUtils.isBlank(agent)) {
            reset(); // don't store whitespace
            return;
        }
        userAgent.set(agent);
    }


    /**
     * Remove value of the user-agent
     */
    public static void reset() {
        userAgent.remove();
    }
}
