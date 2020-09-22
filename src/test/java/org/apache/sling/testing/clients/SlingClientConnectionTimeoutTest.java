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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * SLING-9757 verify configurable connection timeout for SlingClient
 */
public class SlingClientConnectionTimeoutTest {
	private static final String GET_TIMEOUT_PATH = "/test/timeout/resource";
	private static final String OK_RESPONSE = "TEST_OK";

	@ClassRule
	public static HttpServerRule httpServer = new HttpServerRule() {
		@Override
		protected void registerHandlers() throws IOException {
			serverBootstrap.registerHandler(GET_TIMEOUT_PATH, new HttpRequestHandler() {
				@Override
				public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
					// block for 15 seconds
					try {
						Thread.sleep(TimeUnit.SECONDS.toMillis(15));
					} catch (InterruptedException e) {
						// ignore
					}
					response.setEntity(new StringEntity(OK_RESPONSE));
				}
			});
		}
	};

	/**
	 * Test that a configured connection timeout will kill the client request when it
	 * does not respond quickly enough
	 */
	@Test
	public void testConnectionTimeout() throws Exception {
		String originalValue = System.getProperty(SlingClient.CLIENT_CONNECTION_TIMEOUT_PROP, null);
		try {
			// timeout when the request takes more than 2 second
			System.setProperty(SlingClient.CLIENT_CONNECTION_TIMEOUT_PROP, "2");
			try (SlingClient c = new SlingClient(httpServer.getURI(), "user", "pass")) {
				// start the client request
				c.doGet(GET_TIMEOUT_PATH);
				
				// should not get here
				fail("Did not recieve the expected SocketTimeoutException");
			}
		} catch (Exception e) {
			Throwable cause = e.getCause();
			assertTrue("expected a SocketTimeoutException", cause instanceof SocketTimeoutException);
		} finally {
			//put the original value back
			if (originalValue == null) {
				System.clearProperty(SlingClient.CLIENT_CONNECTION_TIMEOUT_PROP);
			} else {
				System.setProperty(SlingClient.CLIENT_CONNECTION_TIMEOUT_PROP, originalValue);
			}
		}    	
	}
	
	/**
	 * Test that when no connection timeout is supplied, the client connection waits
	 */
	@Test
	public void testConnectionNoTimeout() throws Exception {
		String originalValue = System.getProperty(SlingClient.CLIENT_CONNECTION_TIMEOUT_PROP, null);
		try {
			// clear out any timeout configuration
			System.clearProperty(SlingClient.CLIENT_CONNECTION_TIMEOUT_PROP);

			try (SlingClient c = new SlingClient(httpServer.getURI(), "user", "pass")) {
				SlingHttpResponse response = null;
				CompletableFuture<SlingHttpResponse> endpointCall = CompletableFuture.supplyAsync(() -> {
					try {
						return c.doGet(GET_TIMEOUT_PATH);
					} catch (ClientException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					return null;
				});

				try {
					response = endpointCall.get(2, TimeUnit.SECONDS);
					assertNull("Did not expect a response from the endpoint", response);
				} catch (TimeoutException e) {
					// expected that we killed the future when it didn't finish
					//  on it's own in a timely manner
				}    		
			}
		} finally {
			//put the original value back
			if (originalValue == null) {
				System.clearProperty(SlingClient.CLIENT_CONNECTION_TIMEOUT_PROP);
			} else {
				System.setProperty(SlingClient.CLIENT_CONNECTION_TIMEOUT_PROP, originalValue);
			}
		}    	
	}

}
