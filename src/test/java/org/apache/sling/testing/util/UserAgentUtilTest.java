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
package org.apache.sling.testing.util;

import org.apache.sling.testing.clients.util.UserAgentUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UserAgentUtilTest {

    private static final String AGENT_TITLE = "test-agent";
    private static final String AGENT_VERSION = "1.2.3";
    private static final String AGENT = AGENT_TITLE + "/" + AGENT_VERSION;

    @Test
    public void constructAgent() {
        String agent = UserAgentUtil.constructAgent(AGENT_TITLE, AGENT_VERSION);
        assertEquals(AGENT, agent);
    }

    @Test
    public void constructAgentWithoutNullVersion() {
        String agent = UserAgentUtil.constructAgent(AGENT_TITLE, (String) null);
        assertEquals(AGENT_TITLE, agent);
    }

    @Test
    public void constructAgentFromClass() {
        // just test for no exceptions
        UserAgentUtil.constructAgent(String.class);
    }
}
