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

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.sling.testing.clients.SystemPropertiesConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.sling.testing.Constants.EXPECTED_STATUS;

/**
 * {code ServiceUnavailableRetryStrategy} strategy for retrying request in case of a 5XX response code
 */
public class ServerErrorRetryStrategy implements ServiceUnavailableRetryStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(ServerErrorRetryStrategy.class);
    private Collection<Integer> httpRetriesErrorCodes;

    public ServerErrorRetryStrategy() {
        super();
    }

    @Override
    public boolean retryRequest(final HttpResponse response, final int executionCount, final HttpContext context) {
        int[] expectedStatus = (int[]) context.getAttribute(EXPECTED_STATUS);
        boolean needsRetry = executionCount <= SystemPropertiesConfig.getHttpRetries() &&
                responseRetryCondition(response, expectedStatus);

        if (SystemPropertiesConfig.isHttpLogRetries() && needsRetry && LOG.isWarnEnabled()) {
            LOG.warn("Request retry condition met: [count={}/{}], [expected-codes={}], [retry-codes={}]",
                    executionCount, SystemPropertiesConfig.getHttpRetries(), expectedStatus,
                    httpRetriesErrorCodes);
            LOG.warn("Request: {}", getRequestDetails(context));
            LOG.warn("Response: {}", getResponseDetails(response));
            try {
                String content = EntityUtils.toString(response.getEntity());
                LOG.warn("Response Body: {}", content);
            } catch (IOException exc) {
                LOG.warn("Failed to read the response body: {}", exc.getMessage());
            }
        }
        return needsRetry;
    }

    @Override
    public long getRetryInterval() {
        return SystemPropertiesConfig.getHttpRetriesDelay();
    }

    private boolean responseRetryCondition(final HttpResponse response, int... expectedStatus) {
        final Integer statusCode = response.getStatusLine().getStatusCode();
        final Collection<Integer> errorCodes = SystemPropertiesConfig.getHttpRetriesErrorCodes();
        if ((expectedStatus != null) && (expectedStatus.length > 0) &&
                Arrays.stream(expectedStatus).anyMatch(expected -> statusCode == expected)) {
            return false;
        }
        if (errorCodes != null && !errorCodes.isEmpty()) {
            return errorCodes.contains(statusCode);
        } else {
            return statusCode >= SC_INTERNAL_SERVER_ERROR &&
                    statusCode < SC_INTERNAL_SERVER_ERROR + 100;
        }
    }

    /**
     * Best effort attempt to build a request detail string for logging.
     */
    private String getRequestDetails(HttpContext context) {
        String details = "Not available";
        HttpClientContext clientContext = HttpClientContext.adapt(context);
        HttpRequest request = clientContext.getRequest();
        if (request != null) {
            // Build a request detail string like following example:
            // GET /test/internalerror/resource HTTP/1.1
            details = request.getRequestLine().toString();
        }
        return details;
    }

    /**
     * Best effort attempt to build response detail string for logging.
     */
    private String getResponseDetails(HttpResponse response) {
        String details = "Not available";
        if (response != null) {
            // Build a response string like following example:
            // HTTP/1.1 500 Internal Server Error [Date: Thu, 12 Jan 2023 08:32:42 GMT, Server: TEST/1.1,
            //   Content-Length: 8, Content-Type: text/plain; charset=ISO-8859-1, Connection: Keep-Alive, ]
            final StringBuilder sb = new StringBuilder(response.getStatusLine().toString());
            sb.append(" [");
            Arrays.stream(response.getAllHeaders()).forEach(h ->
                    sb.append(h.getName()).append(": ").append(h.getValue()).append(", "));
            sb.append("]");
            details = sb.toString();
        }
        return details;
    }
}
