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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.cookie.Cookie;
import org.junit.Before;
import org.junit.Test;

public class SlingClientImpersonationTest {

    private SlingClient slingClient;

    @Before
    public void setup() throws ClientException, URISyntaxException {
        slingClient = new SlingClient(new URI("http://localhost:8080"), "admin", "pass");
        assertEquals("admin", slingClient.getUser());
    }

    private Cookie getSudoCookie(String cookieName) {
        return slingClient.getCookieStore().getCookies().stream().filter(c -> c.getName().equals(cookieName)).findAny()
                .orElse(null);
    }

    @Test
    public void testImpersonate() throws Exception {

        slingClient.impersonate("user");

        assertEquals("user", slingClient.getUser());

        Cookie sudoCookie = getSudoCookie("sling.sudo");

        assertNotNull(sudoCookie);
        assertEquals("user", sudoCookie.getValue());
    }

    @Test
    public void testUndoImpersonation() throws Exception {

        slingClient.impersonate("user");

        assertEquals("user", slingClient.getUser());

        slingClient.endImpersonation();
        assertEquals("admin", slingClient.getUser());

        Cookie sudoCookie = getSudoCookie("sling.sudo");

        assertNull(sudoCookie);
    }

    @Test
    public void testNullImpersonation() throws Exception {

        slingClient.impersonate("user");

        assertEquals("user", slingClient.getUser());

        slingClient.impersonate(null);
        assertEquals("admin", slingClient.getUser());

        Cookie sudoCookie = getSudoCookie("sling.sudo");

        assertNull(sudoCookie);
    }

    @Test
    public void testDifferentSudoCookie() throws Exception {
        String sudoCookieName = "sudo.make.me.a.sandwich";

        slingClient.addValue(SlingClient.SUDO_COOKIE_NAME, sudoCookieName);

        slingClient.impersonate("user");

        assertEquals("user", slingClient.getUser());

        Cookie defaultSudoCookie = getSudoCookie("sling.sudo");
        assertNull(defaultSudoCookie);

        Cookie customSudoCookie = getSudoCookie(sudoCookieName);
        assertNotNull(customSudoCookie);
        assertEquals("user", customSudoCookie.getValue());
    }

}
