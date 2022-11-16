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

import org.apache.sling.testing.Constants;
import org.apache.sling.testing.clients.SlingClient;
import org.apache.sling.testing.clients.util.UserAgent;

public class UserAgentHolder {

    private static final ThreadLocal<UserAgent> userAgent = new ThreadLocal<>();

    /**
     * Returns the current user-agent object to read or extend it.
     * @return the current user-agent instance
     */
    public static UserAgent get() {
        return userAgent.get();
    }

    /**
     * Returns the current user-agent object to read or extend it. In case its value is null, it'll be initialized
     * with its default value.
     * @return the current user-agent instance
     */
    public static UserAgent getOrInit() {
        if (userAgent.get() == null) {
            // set default user-agent
            set(new UserAgent(Constants.SLING_CLIENT_USERAGENT_TITLE, SlingClient.class.getPackage()));
        }
        return userAgent.get();
    }

    /**
     * Override the current user-agent with a completely new one.
     * @param userAgent the desired new user-agent (or null for default)
     */
    public static void set(UserAgent userAgent) {
        UserAgentHolder.userAgent.set(userAgent);
    }
}
