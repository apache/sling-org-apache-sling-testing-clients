package org.apache.sling.testing;

import org.apache.http.entity.StringEntity;
import org.apache.sling.testing.clients.HttpServerRule;
import org.apache.sling.testing.clients.SlingClient;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.apache.sling.testing.clients.interceptors.UserAgentHolder;
import org.apache.sling.testing.clients.interceptors.UserAgentInterceptor;
import org.apache.sling.testing.clients.util.UserAgent;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CustomUserAgentInterceptorTest {

    private static final String PATH = "/mirror";

    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final String CUSTOM_USER_AGENT_TITLE = "test-client";
    private static final String CUSTOM_USER_AGENT_VERSION = "1.2.3";
    private static final String CUSTOM_USER_AGENT_DETAILS = "details";
    private static final String CUSTOM_USER_AGENT = String.format("%s/%s", CUSTOM_USER_AGENT_TITLE, CUSTOM_USER_AGENT_VERSION);
    private static final String CUSTOM_USER_AGENT_WITH_DETAILS = String.format("%s/%s (%s)", CUSTOM_USER_AGENT_TITLE, CUSTOM_USER_AGENT_VERSION, CUSTOM_USER_AGENT_DETAILS);
    private static final String CUSTOM_USER_AGENT_WITH_APPEND = String.format("%s/%s %s", CUSTOM_USER_AGENT_TITLE, CUSTOM_USER_AGENT_VERSION, CUSTOM_USER_AGENT_DETAILS);

    @ClassRule
    public static HttpServerRule httpServer = new HttpServerRule() {
        @Override
        protected void registerHandlers() {
            serverBootstrap.registerHandler(PATH, (request, response, context) -> {
                response.setEntity(new StringEntity("Success!"));
                response.setStatusCode(200);
                response.setHeaders(request.getHeaders(USER_AGENT_HEADER));
            });
        }
    };

    @Test
    public void testDefault() throws Exception {
        UserAgentInterceptor interceptor = new UserAgentInterceptor();
        SlingClient c = SlingClient.Builder.create(httpServer.getURI(), "user", "pass")
                .addInterceptorLast(interceptor).build();

        // reset user-agent
        UserAgentHolder.set(null);

        SlingHttpResponse response = c.doGet(PATH, 200);
        assertTrue(response.containsHeader(USER_AGENT_HEADER));
        assertEquals(Constants.SLING_CLIENT_USERAGENT_TITLE, response.getFirstHeader(USER_AGENT_HEADER).getValue());
    }

    @Test
    public void testCustom() throws Exception {
        UserAgentInterceptor interceptor = new UserAgentInterceptor();
        SlingClient c = SlingClient.Builder.create(httpServer.getURI(), "user", "pass")
                .addInterceptorLast(interceptor).build();

        UserAgent userAgent = new UserAgent(CUSTOM_USER_AGENT_TITLE, CUSTOM_USER_AGENT_VERSION);
        UserAgentHolder.set(userAgent);

        SlingHttpResponse response = c.doGet(PATH, 200);
        assertTrue(response.containsHeader(USER_AGENT_HEADER));
        assertEquals(response.getFirstHeader(USER_AGENT_HEADER).getValue(), CUSTOM_USER_AGENT);
    }

    @Test
    public void testCustomWithDetails() throws Exception {
        UserAgentInterceptor interceptor = new UserAgentInterceptor();
        SlingClient c = SlingClient.Builder.create(httpServer.getURI(), "user", "pass")
                .addInterceptorLast(interceptor).build();

        UserAgent userAgent = new UserAgent(CUSTOM_USER_AGENT_TITLE, CUSTOM_USER_AGENT_VERSION);
        UserAgent userAgentDetails = new UserAgent(CUSTOM_USER_AGENT_DETAILS, ((String) null));
        UserAgentHolder.set(userAgent.appendDetails(userAgentDetails));

        SlingHttpResponse response = c.doGet(PATH, 200);
        assertTrue(response.containsHeader(USER_AGENT_HEADER));
        assertEquals(response.getFirstHeader(USER_AGENT_HEADER).getValue(), CUSTOM_USER_AGENT_WITH_DETAILS);
    }

    @Test
    public void testCustomWithAppend() throws Exception {
        UserAgentInterceptor interceptor = new UserAgentInterceptor();
        SlingClient c = SlingClient.Builder.create(httpServer.getURI(), "user", "pass")
                .addInterceptorLast(interceptor).build();

        UserAgent userAgent = new UserAgent(CUSTOM_USER_AGENT_TITLE, CUSTOM_USER_AGENT_VERSION);
        UserAgent userAgentDetails = new UserAgent(CUSTOM_USER_AGENT_DETAILS, ((String) null));
        UserAgentHolder.set(userAgent.append(userAgentDetails));

        SlingHttpResponse response = c.doGet(PATH, 200);
        assertTrue(response.containsHeader(USER_AGENT_HEADER));
        assertEquals(response.getFirstHeader(USER_AGENT_HEADER).getValue(), CUSTOM_USER_AGENT_WITH_APPEND);
    }
}
