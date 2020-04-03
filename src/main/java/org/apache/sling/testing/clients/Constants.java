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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Constants {

    /**
     * Prefix for IT-specific system properties
     */
    public static final String CONFIG_PROP_PREFIX = "sling.it.";
    public static final String DEFAULT_URL = "http://localhost:8080/";
    public static final String DEFAULT_USERNAME = "admin";
    public static final String DEFAULT_PASSWORD = "admin";

    // Custom delay for requests
    private static long delay;

    // Custom number of retries for failing requests
    private static int retries;

    // Custom delay between retries in millisec
    private static int retriesDelay;

    // Custom log retries
    private static boolean logRetries;

    // CSV list of error codes to retry requests for
    private static String retryCodes = "";

    /**
     * System property for {@link Constants#HTTP_DELAY}
     * Prefixed by {@link Constants#CONFIG_PROP_PREFIX}
     */
    public static final String HTTP_DELAY_PROP = "http.delay";

    /**
     * System property for {@link Constants#HTTP_RETRIES}
     * Prefixed by {@link Constants#CONFIG_PROP_PREFIX}
     */
    public static final String HTTP_RETRIES_PROP = "http.retries";

    /**
     * System property for {@link Constants#HTTP_RETRIES_DELAY}
     * Prefixed by {@link Constants#CONFIG_PROP_PREFIX}
     */
    public static final String HTTP_RETRIES_DELAY_PROP = "http.retriesDelay";

    /**
     * System property for {@link Constants#HTTP_LOG_RETRIES}
     * Prefixed by {@link Constants#CONFIG_PROP_PREFIX}
     */
    public static final String HTTP_LOG_RETRIES_PROP = "http.logRetries";

    /**
     * System property for {@link Constants#HTTP_RETRIES_ERROR_CODES}
     * Prefixed by {@link Constants#CONFIG_PROP_PREFIX}
     */
    public static final String HTTP_RETRIES_ERROR_CODES_PROP = "http.retriesErrorCodes";

    static {
        try {
            Constants.delay = Long.getLong(Constants.CONFIG_PROP_PREFIX + HTTP_DELAY_PROP, 0);
            Constants.retries = Integer.getInteger(Constants.CONFIG_PROP_PREFIX + HTTP_RETRIES_PROP, 10);
            Constants.retriesDelay = Integer.getInteger(Constants.CONFIG_PROP_PREFIX + HTTP_RETRIES_DELAY_PROP, 1000);
            Constants.logRetries = Boolean.getBoolean(Constants.CONFIG_PROP_PREFIX + HTTP_LOG_RETRIES_PROP);
            Constants.retryCodes = System.getProperty(Constants.CONFIG_PROP_PREFIX + HTTP_RETRIES_ERROR_CODES_PROP, "");
        } catch (NumberFormatException e) {
            Constants.delay = 0;
            Constants.retries = 5;
            Constants.retriesDelay = 1000;
            Constants.logRetries = false;
        }
    }

    /**
     * Custom delay in milliseconds before an HTTP request goes through.
     * Used by {@link org.apache.sling.testing.clients.interceptors.DelayRequestInterceptor}
     */
    public static final long HTTP_DELAY = delay;

    /**
     * Number of http call retries in case of a 5XX response code
     */
    public static final int HTTP_RETRIES = retries;

    /**
     * The delay in milliseconds between http retries
     */
    public static final int HTTP_RETRIES_DELAY = retriesDelay;

    /**
     * Whether to log or not http request retries
     */
    public static final boolean HTTP_LOG_RETRIES = logRetries;

    /**
     * Comma-separated list of http response codes for which to retry the request
     * If empty, all 5XX error codes will be retried
     */
    public static final String HTTP_RETRIES_ERROR_CODES = retryCodes;



    /**
     * Handle to OSGI console
     */
    public static final String OSGI_CONSOLE = "/system/console";

    /**
     * General parameters and values
     */
    public static final String PARAMETER_CHARSET = "_charset_";
    public static final String CHARSET_UTF8 = "utf-8";
}
