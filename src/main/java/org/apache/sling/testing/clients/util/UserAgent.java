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

import javax.annotation.Generated;
import java.util.Objects;

public class UserAgent {

    private final String title;
    private final String version;

    private UserAgent nextToken;

    private UserAgent detailToken;

    /**
     * Create user-agent with simple class name.
     * @param clazz the class
     */
    @SuppressWarnings("unused")
    public UserAgent(Class<?> clazz) {
        this(clazz.getSimpleName(), clazz.getPackage());
    }

    /**
     * Create user-agent from title and determine version through implementation version of the provided package.
     * In case the implementation version of the package returns null, the version info is omitted.
     * @param title the user-agent title
     * @param pkg the package
     */
    public UserAgent(String title, Package pkg) {
        this(title, pkg.getImplementationVersion());
    }

    /**
     * Create user-agent from title and version-string [title]/[version].
     * @param title the user-agent title
     * @param version the user-agent version (or null to use title only)
     */
    public UserAgent(String title, String version) {
        this.title = title;
        this.version = version;
    }

    /**
     * Append another user-agent following the current one.
     * @param token the user-agent to be appended
     * @return the new user-agent object as a FluentInterface
     */
    @SuppressWarnings("UnusedReturnValue")
    public UserAgent append(UserAgent token) {
        if (nextToken == null) {
            nextToken = token;
        } else {
            nextToken.append(token); // propagate
        }
        return this;
    }

    /**
     * Append another user-agent as details to the current one.
     * @param token the user-agent to be appended
     * @return the new user-agent object as a FluentInterface
     */
    @SuppressWarnings("unused")
    public UserAgent appendDetails(UserAgent token) {
        if (detailToken == null) {
            detailToken = token;
        } else {
            detailToken.append(token); // propagate
        }
        return this;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(title);
        if (version != null) {
            result.append("/").append(version);
        }
        if(detailToken != null) {
            result.append(" (").append(detailToken).append(")");
        }
        if (nextToken != null) {
            result.append(" ").append(nextToken);
        }
        return result.toString();
    }

    @Generated({})
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserAgent userAgent = (UserAgent) o;
        return title.equals(userAgent.title)
                && version.equals(userAgent.version)
                && Objects.equals(nextToken, userAgent.nextToken)
                && Objects.equals(detailToken, userAgent.detailToken);
    }

    @Generated({})
    @Override
    public int hashCode() {
        return Objects.hash(title, version, nextToken, detailToken);
    }
}
