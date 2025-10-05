/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.testing.clients.util;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;

/**
 * Helper for creating Entity objects for POST requests.
 */
public class FormEntityBuilder {
    public static final Charset DEFAULT_ENCODING = StandardCharsets.UTF_8;

    private final List<NameValuePair> params;
    private Charset encoding;

    public static FormEntityBuilder create() {
        return new FormEntityBuilder();
    }

    FormEntityBuilder() {
        params = new ArrayList<NameValuePair>();
        encoding = DEFAULT_ENCODING;
    }

    public FormEntityBuilder addAllParameters(Map<String, String> parameters) {
        if (parameters != null) {
            for (String key : parameters.keySet()) {
                addParameter(key, parameters.get(key));
            }
        }

        return this;
    }

    public FormEntityBuilder addAllParameters(List<NameValuePair> parameters) {
        if (parameters != null) {
            params.addAll(parameters);
        }

        return this;
    }

    public FormEntityBuilder addParameter(String name, String value) {
        params.add(new BasicNameValuePair(name, value));
        return this;
    }

    public FormEntityBuilder setEncoding(Charset encoding) {
        this.encoding = encoding;
        return this;
    }

    public UrlEncodedFormEntity build() {
        return new UrlEncodedFormEntity(params, encoding);
    }
}
