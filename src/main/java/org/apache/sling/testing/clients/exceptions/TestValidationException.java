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
package org.apache.sling.testing.clients.exceptions;

import org.apache.http.client.methods.HttpUriRequest;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingHttpResponse;

/**
 * indicates that an expected status is not given, this can include
 * <ul>
 *   <li>HTTP status codes</li>
 *   <li>Content within the response</li>
 *   <li>...
 * </ul>
 *
 */

public class TestValidationException extends ClientException {

	public TestValidationException(String message) {
		super(message);
	}
	
    public TestValidationException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public TestValidationException(String message, int httpStatusCode) {
        super(message,httpStatusCode);
    }

    public TestValidationException(String message, int httpStatusCode, Throwable throwable) {
    	super(message,httpStatusCode, throwable);
    }

    public TestValidationException(String message, Throwable throwable, HttpUriRequest request, SlingHttpResponse response) {
    	super(message,throwable,request,response);
    }
	
}
