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

package org.apache.sling.testing.clients.osgi;

public class Component {

    public enum Status {

        // the states being used in the DS Felix WebConsole are listed in https://github.com/apache/felix/blob/6e5cde8471febb36bc72adeba85989edba943188/webconsole-plugins/ds/src/main/java/org/apache/felix/webconsole/plugins/ds/internal/ComponentConfigurationPrinter.java#L374
        ACTIVE("active"),

        SATISFIED("satisfied"),

        UNSATISFIED_CONFIGURATION("unsatisfied (configuration)"),

        UNSATISFIED_REFERENCE("unsatisfied (reference)"),

        FAILED_ACTIVATION("failed activation"),

        UNKNOWN("unknown");

        String value;

        Status(String value) {
            this.value = value;
        }

        public static Status value(String o) {
            for(Status s : values()) {
                if(s.value.equalsIgnoreCase(o)) {
                    return s;
                }
            }
            return UNKNOWN;
        }

        public String toString() {
            return value;
        }

    }

}
