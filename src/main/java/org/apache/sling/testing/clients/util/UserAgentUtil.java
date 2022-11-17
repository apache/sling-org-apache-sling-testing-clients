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

public class UserAgentUtil {

    private UserAgentUtil() {}

    /**
     * Create user-agent with simple class name.
     * @param clazz the class
     */
    @SuppressWarnings("UnusedReturnValue")
    public static String constructAgent(Class<?> clazz) {
        return constructAgent(clazz.getSimpleName(), clazz.getPackage());
    }

    /**
     * Create user-agent from title and determine version through implementation version of the provided package.
     * In case the implementation version of the package returns null, the version info is omitted.
     * @param title the user-agent title
     * @param pkg the package
     */
    public static String constructAgent(String title, Package pkg) {
        return constructAgent(title, pkg.getImplementationVersion());
    }

    /**
     * Create user-agent from title and version-string [title]/[version].
     * @param title the user-agent title
     * @param version the user-agent version (or null to use title only)
     */
    public static String constructAgent(String title, String version) {
        if (version == null) {
            return title;
        }
        return title + "/" + version;
    }
}
