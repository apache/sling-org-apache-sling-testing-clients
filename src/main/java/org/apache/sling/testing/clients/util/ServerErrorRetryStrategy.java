/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.testing.clients.util;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.apache.hc.client5.http.HttpRequestRetryStrategy;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.HttpEntityContainer;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.TimeValue;
import org.apache.sling.testing.clients.SystemPropertiesConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.hc.core5.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.sling.testing.Constants.EXPECTED_STATUS;

/**
 * {code ServiceUnavailableRetryStrategy} strategy for retrying request in case of a 5XX response code
 */
public class ServerErrorRetryStrategy implements HttpRequestRetryStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(ServerErrorRetryStrategy.class);
    private Collection<Integer> httpRetriesErrorCodes;

    public ServerErrorRetryStrategy() {
        super();
    }

    @Override
    public boolean retryRequest(HttpRequest request, IOException exception, int executionCount, HttpContext context) {
        return false;
    }

    @Override
    public boolean retryRequest(final HttpResponse response, final int executionCount, final HttpContext context) {
        int[] expectedStatus = (int[]) context.getAttribute(EXPECTED_STATUS);
        boolean needsRetry = executionCount <= SystemPropertiesConfig.getHttpRetries()
                && responseRetryCondition(response, expectedStatus);

        if (SystemPropertiesConfig.isHttpLogRetries() && needsRetry && LOG.isWarnEnabled()) {
            LOG.warn(
                    "Request retry condition met: [count={}/{}], [expected-codes={}], [retry-codes={}]",
                    executionCount,
                    SystemPropertiesConfig.getHttpRetries(),
                    expectedStatus,
                    httpRetriesErrorCodes);
            LOG.warn("Request: {}", getRequestDetails(context));
            LOG.warn("Response: {}", getResponseDetails(response));
            if ((response instanceof HttpEntityContainer) && (((HttpEntityContainer) response).getEntity() != null)) {
                try {
                    String content = EntityUtils.toString(((HttpEntityContainer) response).getEntity());
                    LOG.warn("Response Body: {}", content);
                } catch (IOException | ParseException exc) {
                    LOG.warn("Failed to read the response body: {}", exc.getMessage());
                }
            }
        }
        return needsRetry;
    }

    @Override
    public TimeValue getRetryInterval(HttpRequest request, IOException exception, int execCount, HttpContext context) {
        return SystemPropertiesConfig.getHttpRetriesDelay();
    }

    @Override
    public TimeValue getRetryInterval(HttpResponse response, int execCount, HttpContext context) {
        return SystemPropertiesConfig.getHttpRetriesDelay();
    }

    private boolean responseRetryCondition(final HttpResponse response, int... expectedStatus) {
        final Integer statusCode = response.getCode();
        final Collection<Integer> errorCodes = SystemPropertiesConfig.getHttpRetriesErrorCodes();
        if ((expectedStatus != null)
                && (expectedStatus.length > 0)
                && Arrays.stream(expectedStatus).anyMatch(expected -> statusCode == expected)) {
            return false;
        }
        if (errorCodes != null && !errorCodes.isEmpty()) {
            return errorCodes.contains(statusCode);
        } else {
            return statusCode >= SC_INTERNAL_SERVER_ERROR && statusCode < SC_INTERNAL_SERVER_ERROR + 100;
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
            // GET /test/internalerror/resource
            details = request.getMethod() + " " + request.getPath();
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
            final StringBuilder sb = new StringBuilder(response.getCode() + " " + response.getReasonPhrase());
            sb.append(" [");
            Arrays.stream(response.getHeaders()).forEach(h -> sb.append(h.getName())
                    .append(": ")
                    .append(h.getValue())
                    .append(", "));
            sb.append("]");
            details = sb.toString();
        }
        return details;
    }
}
