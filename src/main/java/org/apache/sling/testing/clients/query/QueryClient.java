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
package org.apache.sling.testing.clients.query;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.NameValuePair;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingClient;
import org.apache.sling.testing.clients.SlingClientConfig;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.apache.sling.testing.clients.osgi.OsgiConsoleClient;
import org.apache.sling.testing.clients.query.servlet.QueryServlet;
import org.apache.sling.testing.clients.util.JsonUtils;
import org.apache.sling.testing.clients.util.URLParameterBuilder;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;

/**
 * <p>Sling client for performing oak queries.</p>
 *
 * <p>Uses a custom servlet {@link QueryServlet} to execute the query on the server
 * and return the results as a json. If the servlet is not yet present, it automatically
 * installs it and creates the corresponding nodes</p>
 *
 * <p>The servlet is exposed under {@value QueryServlet#SERVLET_PATH}.</p>
 *
 * <p>The servlet is not automatically uninstalled to avoid too much noise on the instance.
 * The caller should take care of it, if needed, by calling {@link #uninstallServlet()}</p>
 */
public class QueryClient extends SlingClient {

    /**
     * Query types, as defined in {@code org.apache.jackrabbit.oak.query.QueryEngineImpl}
     */
    public enum QueryType {
        SQL2("JCR-SQL2"),
        SQL("sql"),
        XPATH("xpath"),
        JQOM("JCR-JQOM");

        private final String name;

        QueryType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(QueryClient.class);

    private static final String BUNDLE_BSN = "org.apache.sling.testing.clients.query";
    private static final String BUNDLE_NAME = "Sling Testing Clients Query Servlet";
    private static final String BUNDLE_VERSION = "1.0.0";

    private static final long BUNDLE_START_TIMEOUT = TimeUnit.SECONDS.toMillis(10);

    /**
     * Constructor used by adaptTo
     *
     * @param http underlying HttpClient
     * @param config config state
     * @throws ClientException if the client cannot be created
     */
    public QueryClient(CloseableHttpClient http, SlingClientConfig config) throws ClientException {
        super(http, config);
    }

    /**
     * Convenience constructor
     *
     * @param url host url
     * @param user username
     * @param password password
     * @throws ClientException if the client cannot be constructed
     */
    public QueryClient(URI url, String user, String password) throws ClientException {
        super(url, user, password);
    }

    /**
     * Executes a query on the server and returns the results as a json
     *
     * @param query query to be executed
     * @param type type of the query
     * @return the results in json as exported by {@link QueryServlet}
     * @throws ClientException if the request failed to execute
     * @throws InterruptedException to mark that this method blocks
     */
    public JsonNode doQuery(final String query, final QueryType type) throws ClientException, InterruptedException {
        return doQuery(query, type, true, false);
    }

    /**
     * Executes a query on the server and returns only the number of rows in the result
     *
     * @param query query to be executed
     * @param type type of the query
     * @return total results returned by the query
     * @throws ClientException if the request failed to execute
     * @throws InterruptedException to mark that this method blocks
     */
    public long doCount(final String query, final QueryType type) throws ClientException, InterruptedException {
        return doQuery(query, type, false, false).get("total").longValue();
    }

    /**
     * Retrieves the plan of the query. Useful for determining which index is used
     *
     * @param query query to be executed
     * @param type type of the query
     * @return total results returned by the query
     * @throws ClientException if the request failed to execute
     * @throws InterruptedException to mark that this method blocks
     */
    public String getPlan(final String query, final QueryType type) throws ClientException, InterruptedException {
        return doQuery(query, type, false, true).get("plan").toString();
    }

    protected JsonNode doQuery(final String query, final QueryType type, final boolean showResults, final boolean explain)
            throws ClientException, InterruptedException {

        List<NameValuePair> params = URLParameterBuilder.create()
                .add("query", query)
                .add("type", type.toString())
                .add("showresults", Boolean.toString(showResults))
                .add("explain", Boolean.toString(explain))
                .getList();

        try {
            // try optimistically to execute the query
            SlingHttpResponse response = this.doGet(QueryServlet.SERVLET_PATH, params, SC_OK);
            return JsonUtils.getJsonNodeFromString(response.getContent());
        } catch (ClientException e) {
            if (e.getHttpStatusCode() == SC_NOT_FOUND) {
                LOG.info("Could not find query servlet, will try to install it");
                installServlet();
                LOG.info("Retrying the query");
                SlingHttpResponse response = this.doGet(QueryServlet.SERVLET_PATH, params, SC_OK);
                return JsonUtils.getJsonNodeFromString(response.getContent());
            } else {
                throw e;
            }
        }
    }

    /**
     * <p>Installs the servlet to be able to perform queries.</p>
     *
     * <p>By default, methods of this client automatically install the servlet if needed,
     * so there is no need to explicitly call from outside</p>
     *
     * @return this
     * @throws ClientException if the installation fails
     * @throws InterruptedException to mark that this method blocks
     */
    public QueryClient installServlet() throws ClientException, InterruptedException {
        InputStream bundleStream = TinyBundles.bundle()
                .set("Bundle-SymbolicName", BUNDLE_BSN)
                .set("Bundle-Version", BUNDLE_VERSION)
                .set("Bundle-Name", BUNDLE_NAME)
                .add(QueryServlet.class)
                .build(TinyBundles.withBnd());

        try {
            File bundleFile = File.createTempFile(BUNDLE_BSN + "-" + BUNDLE_VERSION, ".jar");
            Files.copy(bundleStream, bundleFile.toPath(), REPLACE_EXISTING);

            adaptTo(OsgiConsoleClient.class).installBundle(bundleFile, true);
            adaptTo(OsgiConsoleClient.class).waitBundleStarted(BUNDLE_BSN, BUNDLE_START_TIMEOUT, 100);

            LOG.info("query servlet installed at {}", getUrl(QueryServlet.SERVLET_PATH));
        } catch (IOException e) {
            throw new ClientException("Failed to create the query servlet bundle", e);
        } catch (TimeoutException e) {
            throw new ClientException("The query servlet bundle did not successfully start", e);
        }

        return this;
    }

    /**
     * Deletes all the resources created by {@link #installServlet()}
     *
     * @return this
     * @throws ClientException if any of the resources fails to uninstall
     */
    public QueryClient uninstallServlet() throws ClientException {
        adaptTo(OsgiConsoleClient.class).uninstallBundle(BUNDLE_BSN);
        return this;
    }
}
