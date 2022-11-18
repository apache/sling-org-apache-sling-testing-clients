/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.testing.clients.interceptors;

import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;
import org.apache.sling.testing.clients.SystemPropertiesConfig;
import org.slf4j.LoggerFactory;

public class UserAgentInterceptor implements HttpRequestInterceptor {

    private final org.slf4j.Logger log = LoggerFactory.getLogger(getClass());

    public static final String USER_AGENT_HEADER = "User-Agent";

    public void process(HttpRequest httpRequest, HttpContext httpContext) {
        if (UserAgentHolder.get() == null) {
            return;
        }

        // handle existing user-agent header
        if (httpRequest.containsHeader(USER_AGENT_HEADER)) {
            if (!httpRequest.getFirstHeader(USER_AGENT_HEADER).getValue().equals(SystemPropertiesConfig.getDefaultUserAgent())) {
                log.warn("User-agent of client-request changed manually; use CustomUserAgentRule instead!");
                return;
            }
            httpRequest.removeHeaders(USER_AGENT_HEADER);
        }

        // add custom user agent
        httpRequest.addHeader(USER_AGENT_HEADER, UserAgentHolder.get());
    }
}
