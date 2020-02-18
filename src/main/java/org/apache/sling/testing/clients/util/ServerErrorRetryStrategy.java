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
import org.apache.http.HttpStatus;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.Args;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;

/**
 * {code ServiceUnavailableRetryStrategy} strategy for retrying request in case of a 5XX response code
 */
public class ServerErrorRetryStrategy implements ServiceUnavailableRetryStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(ServerErrorRetryStrategy.class);

    private final int maxRetries;
    private final int retryInterval;
    private final boolean logRetries;

    public ServerErrorRetryStrategy(final int maxRetries, final int retryInterval, final boolean logRetries) {
        super();
        Args.positive(maxRetries, "Max retries");
        Args.positive(retryInterval, "Retry interval");
        this.maxRetries = maxRetries;
        this.retryInterval = retryInterval;
        this.logRetries = logRetries;
    }

    public ServerErrorRetryStrategy(int maxRetries, int retryInterval) {
        this (1, 1000, true);
    }

    public ServerErrorRetryStrategy() {
        this(1, 1000);
    }

    @Override
    public boolean retryRequest(final HttpResponse response, final int executionCount, final HttpContext context) {
        boolean needsRetry = executionCount <= this.maxRetries &&
                response.getStatusLine().getStatusCode() >= HttpStatus.SC_INTERNAL_SERVER_ERROR &&
                response.getStatusLine().getStatusCode() < HttpStatus.SC_INTERNAL_SERVER_ERROR + 100;

        if (this.logRetries && needsRetry && LOG.isWarnEnabled()) {
            LOG.warn("Request retry needed due to service unavailable response");
            LOG.warn("Response headers contained:");
            Arrays.stream(response.getAllHeaders()).forEach(h -> LOG.warn("Header {}:{}", h.getName(), h.getValue()));
            try {
                String content = EntityUtils.toString(response.getEntity());
                LOG.warn("Response content: {}", content);
            } catch (IOException exc) {
                LOG.warn("Response as no content");
            }
        }
        return needsRetry;
    }

    @Override
    public long getRetryInterval() {
        return retryInterval;
    }

}
