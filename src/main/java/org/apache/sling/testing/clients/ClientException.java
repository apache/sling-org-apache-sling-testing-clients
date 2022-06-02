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

import org.apache.http.client.methods.HttpUriRequest;

/**
 * An exception thrown when something went wrong with using the sling testing clients.
 * 
 * This class will be turned into an abstract class eventually, so do use the specialized
 * sub-classes instead:
 * <ul>
 *   <li>TestingIOException to indicate network and IO problems</li>
 *   <li>TestingValidationException to indicate a mismatch between expecation and result</li>
 *   <li>TestingSetupException to indicate problems in the test setup (incorrect parameters etc)</li>
 * </ul>
 * 
 * 
 */
public class ClientException extends Exception {

    private static final long serialVersionUID = 1L;
    private int httpStatusCode = -1;
    private HttpUriRequest request;
    private SlingHttpResponse response;

    @Deprecated
    public ClientException(String message) {
        this(message, null);
    }

    @Deprecated
    public ClientException(String message, Throwable throwable) {
        this(message, -1, throwable);
    }

    @Deprecated
    public ClientException(String message, int httpStatusCode) {
        this(message, httpStatusCode, null);
    }

    @Deprecated
    public ClientException(String message, int httpStatusCode, Throwable throwable) {
        super(message, throwable);
        this.httpStatusCode = httpStatusCode;
    }

    @Deprecated
    public ClientException(String message, Throwable throwable, HttpUriRequest request, SlingHttpResponse response) {
        this(message, throwable);
        this.request = request;
        this.response = response;
        if (this.response != null) {
            this.httpStatusCode = response.getStatusLine().getStatusCode();
        }
    }

    /**
     * @return The request associated with this exception or {{null}}
     */
    public HttpUriRequest getRequest() {
        return request;
    }

    /**
     * Set the request associated with this exception
     * @param request
     */
    public void setRequest(HttpUriRequest request) {
        this.request = request;
    }

    /**
     * @return The response associated with this exception or {{null}}
     */
    public SlingHttpResponse getResponse() {
        return response;
    }

    /**
     * Set the response associated with this exception or {{null}}
     * @param response
     */
    public void setResponse(SlingHttpResponse response) {
        this.response = response;
    }

    /**
     * @return the httpStatusCode
     */
    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    @Override
    public String getMessage() {
        String message = super.getMessage();
        if (httpStatusCode > -1) {
            message = message + "(return code=" + httpStatusCode + ")";
        }
        return message;
    }

    @Override
    public String toString() {
        StringBuilder messageBuilder = new StringBuilder(super.toString()).append(System.lineSeparator());
        if (null != request) {
            messageBuilder.append("Request: ");
            messageBuilder.append(request.getMethod()).append(" ").append(request.getURI());
            messageBuilder.append(System.lineSeparator());
        }
        if (null != response) {
            messageBuilder.append("Response: ");
            messageBuilder.append(response.getStatusLine().getStatusCode()).append(" ")
                    .append(response.getStatusLine().getReasonPhrase());
            messageBuilder.append(response.getContent());
        }
        return messageBuilder.toString();
    }
}
