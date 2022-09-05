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
package org.apache.sling.testing.clients;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

public class SystemPropertiesConfig {

    /**
     * Prefix for IT-specific system properties
     */
    public static final String CONFIG_PROP_PREFIX = "sling.it.";

    /**
     * System property for {@link SystemPropertiesConfig#getHttpDelay()}
     * Prefixed by {@link SystemPropertiesConfig#CONFIG_PROP_PREFIX}
     */
    public static final String HTTP_DELAY_PROP = "http.delay";

    /**
     * System property for {@link SystemPropertiesConfig#getHttpRetries()}
     * Prefixed by {@link SystemPropertiesConfig#CONFIG_PROP_PREFIX}
     */
    public static final String HTTP_RETRIES_PROP = "http.retries";

    /**
     * System property for {@link SystemPropertiesConfig#getHttpRetriesDelay()}
     * Prefixed by {@link SystemPropertiesConfig#CONFIG_PROP_PREFIX}
     */
    public static final String HTTP_RETRIES_DELAY_PROP = "http.retriesDelay";

    /**
     * System property for {@link SystemPropertiesConfig#isHttpLogRetries()}
     * Prefixed by {@link SystemPropertiesConfig#CONFIG_PROP_PREFIX}
     */
    public static final String HTTP_LOG_RETRIES_PROP = "http.logRetries";

    /**
     * System property for {@link SystemPropertiesConfig#getHttpRetriesErrorCodes()}
     * Prefixed by {@link SystemPropertiesConfig#CONFIG_PROP_PREFIX}
     */
    public static final String HTTP_RETRIES_ERROR_CODES_PROP = "http.retriesErrorCodes";

    public static String getPrefixedPropertyName(String prop) {
        return SystemPropertiesConfig.CONFIG_PROP_PREFIX + prop;
    }

    /**
     * Custom delay in milliseconds before an HTTP request goes through.
     * Used by {@link org.apache.sling.testing.clients.interceptors.DelayRequestInterceptor}
     * @return the delay in muliseconds
     */
    public static long getHttpDelay() {
        try {
            return Long.getLong(getPrefixedPropertyName(HTTP_DELAY_PROP), 0);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Number of http call retries in case of a 5XX response code
     * @return the number of retries to be made
     */
    public static int getHttpRetries() {
        try {
            return Integer.getInteger(getPrefixedPropertyName(HTTP_RETRIES_PROP), 10);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * The delay in milliseconds between http retries
     * @return the delay between http retries
     */
    public static int getHttpRetriesDelay() {
        try {
            return Integer.getInteger(getPrefixedPropertyName(HTTP_RETRIES_DELAY_PROP), 1000);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Whether to log or not http request retries
     * @return true if retries should be logged
     */
    public static boolean isHttpLogRetries() {
        try {
            return Boolean.getBoolean(getPrefixedPropertyName(HTTP_LOG_RETRIES_PROP));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Comma-separated list of http response codes for which to retry the request
     * If empty, all 5XX error codes will be retried
     * @return a non-null collection with the http resonse codes
     */
    public static Collection<Integer> getHttpRetriesErrorCodes() {
        try {
            final String errorCodes = System.getProperty(getPrefixedPropertyName(HTTP_RETRIES_ERROR_CODES_PROP), "");
            return Arrays.asList(errorCodes.split(",")).stream().map(s -> {
                try {
                    return Integer.valueOf(s);
                } catch (NumberFormatException e) {
                    return null;
                }
            }).filter(Objects::nonNull).collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

}
