/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ******************************************************************************/
package org.apache.sling.testing.util;

import org.apache.sling.testing.clients.util.ResourceUtil;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ResourceUtilTest {

    private static final long EXPECTED_NB_CARRIAGE_RETURNS = 6;

    @Test
    public void testReadResourceAsStringNoExtraLineBreak() throws IOException {
        String input = ResourceUtil.readResourceAsString("/resource-util-test-file.txt");
        assertNotNull(input);
        long count = input.chars().filter(ch -> ch == '\n').count();
        assertEquals("Expecting to have 6 line break in the given text file.", EXPECTED_NB_CARRIAGE_RETURNS, count);
    }

}
