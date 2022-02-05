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
package org.apache.sling.testing;

public class Constants {

    public static final String DEFAULT_URL = "http://localhost:8080/";
    public static final String DEFAULT_USERNAME = "admin";
    public static final String DEFAULT_PASSWORD = "admin";

    /**
     * Handle to OSGI console
     */
    public static final String OSGI_CONSOLE = "/system/console";

    /**
     * General parameters and values
     */
    public static final String PARAMETER_CHARSET = "_charset_";
    public static final String CHARSET_UTF8 = "utf-8";

    /**
     * Http Context Attributes
     */
    public static final String EXPECTED_STATUS = "expected_status";

    /**
     * ClientException expected status prefix
     */
    public static final String EXPECTED_STATUS_ERROR_PREFIX = "Expected HTTP Status: ";

}
