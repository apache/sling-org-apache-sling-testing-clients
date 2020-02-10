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
package org.apache.sling.testing.clients.util;

import org.apache.http.HttpResponse;
import org.apache.http.annotation.Immutable;
import org.apache.http.impl.client.DefaultServiceUnavailableRetryStrategy;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;

@Immutable
public class LoggedServiceUnavailableRetryStrategy extends DefaultServiceUnavailableRetryStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggedServiceUnavailableRetryStrategy.class);

    public LoggedServiceUnavailableRetryStrategy(final int maxRetries, final int retryInterval) {
        super(maxRetries, retryInterval);
    }

    @Override
    public boolean retryRequest(final HttpResponse response, final int executionCount, final HttpContext context) {
        boolean needRetry = super.retryRequest(response, executionCount, context);
        if (needRetry && LOGGER.isWarnEnabled()) {
            LOGGER.warn("Request retry needed due to service unavailable response");
            LOGGER.warn("Response headers contained:");
            Arrays.stream(response.getAllHeaders()).forEach(h -> LOGGER.warn("Header {}:{}", h.getName(), h.getValue()));
            try {
                String content = EntityUtils.toString(response.getEntity());
                LOGGER.warn("Response content: {}", content);
            } catch (IOException exc) {
                LOGGER.warn("Response as no content");
            }
        }
        return needRetry;
    }

}
