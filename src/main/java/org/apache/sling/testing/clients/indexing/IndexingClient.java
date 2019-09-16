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
package org.apache.sling.testing.clients.indexing;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingClient;
import org.apache.sling.testing.clients.SlingClientConfig;
import org.apache.sling.testing.clients.osgi.OsgiConsoleClient;
import org.apache.sling.testing.clients.query.QueryClient;
import org.apache.sling.testing.clients.util.poller.Polling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.UUID.randomUUID;
import static org.apache.http.HttpStatus.SC_OK;

/**
 * <p>Interface to the oak indexing mechanism</p>
 *
 * <p>Exposes {@link #waitForAsyncIndexing(long, long)} for waiting all the indexing lanes to finish
 * indexing and to guarantee all the indices are up to date</p>
 *
 * <p>For using {@link #waitForAsyncIndexing(long, long)}, the user must have access rights to:</p>
 *  <ul>
 *      <li>read/write in {@code /tmp}</li>
 *      <li>install bundles via {@link org.apache.sling.testing.clients.osgi.OsgiConsoleClient}
 *      (if the query servlet was not previously installed)</li>
 *  </ul>
 *  <p>In short, it requires administrative rights.</p>
 */
public class IndexingClient extends SlingClient {
    private static final Logger LOG = LoggerFactory.getLogger(IndexingClient.class);

    /** Configuration name in {@link SlingClientConfig} to be used to initialize pre-defined index lanes
     *  Configured value, if any, is supposed to be an array of lane names
     */
    private static final String INDEX_LANES_CSV_CONFIG_NAME = "indexLanesCsv";

    /** Root of all the data created by this tool. Its presence marks that it was already installed */
    private static final String WAIT_FOR_ASYNC_INDEXING_ROOT = "/tmp/testing/waitForAsyncIndexing";

    /** Where new index definitions are added */
    private static final String INDEX_PATH = WAIT_FOR_ASYNC_INDEXING_ROOT + "/oak:index";

    /** Where the content to be indexed is created */
    private static final String CONTENT_PATH = WAIT_FOR_ASYNC_INDEXING_ROOT + "/content";

    /** Prefix to be added to all the index names */
    private static final String INDEX_PREFIX = "testIndexingLane-";

    /** Prefix to be added to all the properties */
    private static final String PROPERTY_PREFIX = "testProp-";

    /** Prefix to be added to all the property values */
    private static final String VALUE_PREFIX = "testasyncval-";

    /** Prefix to be added to all the tags */
    private static final String TAG_PREFIX = "testTag";

    /** Placeholder for index name */
    private static final String INDEX_NAME_PLACEHOLDER = "<<INDEXNAME>>";

    /** Placeholder for random, unique parts in content and queries */
    private static final String PROPERTY_PLACEHOLDER = "<<PROPNAME>>";

    /** Placeholder for random, unique parts in content and queries */
    private static final String VALUE_PLACEHOLDER = "<<RANDVAL>>";

    /** Placeholder for identifying the lane to which the index and the content belongs to */
    private static final String LANE_PLACEHOLDER = "<<LANE>>";

    /** Placeholder for identifying the tag to which the index and the queries belongs to */
    private static final String TAG_PLACEHOLDER = "<<TAG>>";

    /** Template for index definitions to be installed */
    private static final String INDEX_DEFINITION = "{" +
            "  '" + INDEX_NAME_PLACEHOLDER + "': {\n" +
            "    'jcr:primaryType': 'oak:QueryIndexDefinition',\n" +
            "    'type': 'lucene',\n" +
            "    'async': '" + LANE_PLACEHOLDER + "',\n" +
            "    'tags': '" + TAG_PLACEHOLDER + "',\n" +
            "    'indexRules': {\n" +
            "      'jcr:primaryType': 'nt:unstructured',\n" +
            "      'nt:base': {\n" +
            "        'jcr:primaryType': 'nt:unstructured',\n" +
            "        'properties': {\n" +
            "          'jcr:primaryType': 'nt:unstructured',\n" +
            "          '" + PROPERTY_PLACEHOLDER + "': {\n" +
            "            'jcr:primaryType': 'nt:unstructured',\n" +
            "            'name': '" + PROPERTY_PLACEHOLDER + "',\n" +
            "            'analyzed': true\n" +
            "            }\n" +
            "          }\n" +
            "        }\n" +
            "      }\n" +
            "    }" +
            "}";

    /** Template for the content to be created and searched */
    private static final String CONTENT_DEFINITION = "{" +
            "'testContent-" + LANE_PLACEHOLDER + "-" + VALUE_PLACEHOLDER + "': {" +
            "  'jcr:primaryType': 'nt:unstructured', " +
            "  '" + PROPERTY_PLACEHOLDER +"': '" + VALUE_PLACEHOLDER + "'" +
            "}}";


    /** Templates for queries to be executed against each index, in order of priority */
    private static final List<String> QUERIES = Arrays.asList(
            // for Oak versions that support option(index tag testTag)
            "/jcr:root" + WAIT_FOR_ASYNC_INDEXING_ROOT + "//*" +
                    "[jcr:contains(@" + PROPERTY_PLACEHOLDER + ", '" + VALUE_PLACEHOLDER +"')] " +
                    "option(traversal ok, index tag " + TAG_PLACEHOLDER + ")",
            // for older Oak versions
            "/jcr:root" + WAIT_FOR_ASYNC_INDEXING_ROOT + "//*" +
                    "[jcr:contains(@" + PROPERTY_PLACEHOLDER + ", '" + VALUE_PLACEHOLDER +"')] " +
                    "option(traversal ok)"
    );

    /** Global counter for how much time was spent in total waiting for async indexing */
    private static final AtomicLong totalWaited = new AtomicLong();

    /**
     * Constructor used by Builders and adaptTo(). <b>Should never be called directly from the code.</b>
     *
     * @param http the underlying HttpClient to be used
     * @param config sling specific configs
     * @throws ClientException if the client could not be created
     */
    public IndexingClient(CloseableHttpClient http, SlingClientConfig config) throws ClientException {
        super(http, config);
    }

    /**
     * <p>Handy constructor easy to use in simple tests. Creates a client that uses basic authentication.</p>
     *
     * <p>For constructing clients with complex configurations, use a {@link InternalBuilder}</p>
     *
     * <p>For constructing clients with the same configuration, but a different class, use {@link #adaptTo(Class)}</p>
     *
     * @param url url of the server (including context path)
     * @param user username for basic authentication
     * @param password password for basic authentication
     * @throws ClientException never, kept for uniformity with the other constructors
     */
    public IndexingClient(URI url, String user, String password) throws ClientException {
        super(url, user, password);
    }

    public void setLaneNames(String ... laneNames) {
        getValues().put(INDEX_LANES_CSV_CONFIG_NAME, StringUtils.join(laneNames));
    }

    /**
     * Retrieves the list of indexing lanes configured on the instance
     *
     * @return list of lane names
     * @throws ClientException if the request fails
     */
    public List<String> getLaneNames() throws ClientException {
        List<String> configuredLanes = getConfiguredLaneNames();
        if (!configuredLanes.isEmpty()) {
            return configuredLanes;
        }

        try {
            Object asyncConfigs = adaptTo(OsgiConsoleClient.class)
                    .getConfiguration("org.apache.jackrabbit.oak.plugins.index.AsyncIndexerService")
                    .get("asyncConfigs");

            if (asyncConfigs instanceof String[]) {
                String[] configs = (String[]) asyncConfigs;  // ugly, we should refactor OsgiConsoleClient

                List<String> lanes = new ArrayList<>(configs.length);
                for (String asyncConfig : configs) {
                    lanes.add(asyncConfig.split(":")[0]);
                }
                return lanes;
            } else {
                throw new ClientException("Cannot retrieve config from AsyncIndexerService, asyncConfigs is not a String[]");
            }
        } catch (Exception e) {
            throw new ClientException("Failed to retrieve lanes", e);
        }
    }

    private List<String> getConfiguredLaneNames() {
        String configLanesCsv = getValue(INDEX_LANES_CSV_CONFIG_NAME);
        if (configLanesCsv == null) {
            return Collections.emptyList();
        }

        String[] configLanesArr = configLanesCsv.split(",");
        for (int i = 0; i < configLanesArr.length; i++) {
            configLanesArr[i] = configLanesArr[i].trim();
        }

        return Collections.unmodifiableList(Arrays.asList(configLanesArr));
    }

    /**
     * <p>Blocks until all the async indices are up to date, to guarantee that the susequent queries return
     * all the results.</p>
     *
     * <p>Works by creating a custom index for each lane, adding specific content to
     * be indexed by these indices and then repeatedly searching this content until everything is found (indexed).
     * All the content is created under {@value #WAIT_FOR_ASYNC_INDEXING_ROOT}</p>
     *
     * <p>Indices are automatically created, but only if not already present.
     * This method does not delete the indices at the end to avoid generating too much noise on the instance.
     * To completely clean any traces, the user must call {@link #uninstall()}</p>
     *
     * <p>Requires administrative rights to install bundles and to create nodes under
     * {@value #WAIT_FOR_ASYNC_INDEXING_ROOT}</p>
     *
     * @param timeout max time to wait, in milliseconds, before throwing {@code TimeoutException}
     * @param delay time to sleep between retries
     * @throws TimeoutException if the {@code timeout} was reached before all the indices were updated
     * @throws InterruptedException to mark this method as waiting
     * @throws ClientException if an error occurs during http requests/responses
     */
    public void waitForAsyncIndexing(final long timeout, final long delay)
            throws TimeoutException, InterruptedException, ClientException {

        install();  // will install only if needed

        final String uniqueValue = randomUUID().toString();  // to be added in all the content nodes
        final List<String> lanes = getLaneNames();  // dynamically detect which lanes to wait for

        Polling p = new Polling(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return searchContent(lanes, uniqueValue);
            }
        });

        try {
            createContent(lanes, uniqueValue);
            p.poll(timeout, delay);
        } finally {
            long total = totalWaited.addAndGet(p.getWaited()); // count waited in all the cases (timeout)
            LOG.info("Waited for async index {} ms (overall: {} ms)", p.getWaited(), total);
            try {
                deleteContent(uniqueValue);
            } catch (ClientException e) {
                LOG.warn("Failed to delete temporary content", e);
            }
        }
    }

    /**
     * Same as {@link #waitForAsyncIndexing(long timeout, long delay)},
     * but with default values for {@code timeout=1min} and {@code delay=500ms}.
     *
     * @see #waitForAsyncIndexing(long, long)
     *
     * @throws TimeoutException if the {@code timeout} was reached before all the indices were updated
     * @throws InterruptedException to mark this method as waiting
     * @throws ClientException if an error occurs during http requests/responses
     */
    public void waitForAsyncIndexing() throws InterruptedException, ClientException, TimeoutException {
        waitForAsyncIndexing(TimeUnit.MINUTES.toMillis(1), 500);
    }

    /**
     * <p>Creates the necessary custom indices in the repository, if not already present.</p>
     *
     * <p>It is automatically called in each wait, there's no need to
     * explicitly invoke it from the test.</p>
     *
     * @throws ClientException if the installation fails
     */
    public void install() throws ClientException {
        if (exists(WAIT_FOR_ASYNC_INDEXING_ROOT)) {
            LOG.debug("Skipping install since {} already exists", WAIT_FOR_ASYNC_INDEXING_ROOT);
            return;
        }

        createNodeRecursive(WAIT_FOR_ASYNC_INDEXING_ROOT, "sling:Folder");
        createNode(INDEX_PATH, "nt:unstructured");
        createNode(CONTENT_PATH, "sling:Folder");

        final List<String> lanes = getLaneNames();
        for (String lane : lanes) {
            String indexName = getIndexName(lane);
            String indexDefinition = replacePlaceholders(INDEX_DEFINITION, lane, null);
            LOG.info("Creating index {} in {}", indexName, INDEX_PATH);
            LOG.debug(indexDefinition);
            importContent(INDEX_PATH, "json", indexDefinition);
            // Trigger reindex to make sure the complete index definition is used
            setPropertyString(INDEX_PATH + "/" + indexName, "reindex", "true");
        }
    }

    /**
     * <p>Cleans all the data generated by {@link #install()} and {@link #waitForAsyncIndexing(long, long)}.</p>
     *
     * <p>User must manually call this if needed, as opposed to {@link #install()}, which is called
     * automatically.</p>
     *
     * @throws ClientException if the cleanup failed
     */
    public void uninstall() throws ClientException {
        deletePath(WAIT_FOR_ASYNC_INDEXING_ROOT, SC_OK);
    }

    /**
     * Creates all the content structures to be indexed, one for each lane,
     * with the given {@code uniqueValue}, to make them easily identifiable
     *
     * @param lanes list of lanes for which to create the content
     * @param uniqueValue the unique value to be added
     * @throws ClientException if the content creation fails
     */
    private void createContent(final List<String> lanes, final String uniqueValue) throws ClientException {
        // All the content is grouped under the same node
        String contentHolder = CONTENT_PATH + "/" + uniqueValue;
        LOG.debug("creating content in {}", contentHolder);
        createNode(contentHolder, "sling:Folder");

        for (String lane : lanes) {
            String contentNode = replacePlaceholders(CONTENT_DEFINITION, lane, uniqueValue);
            LOG.debug("creating: {}", contentNode);
            importContent(contentHolder, "json", contentNode);
        }
    }

    /**
     * Deletes the temporary nodes created in {@link #createContent(List, String)}
     *
     * @throws ClientException if the content cannot be deleted
     */
    private void deleteContent(String uniqueValue) throws ClientException {
        if (uniqueValue != null) {
            String contentHolder = CONTENT_PATH + "/" + uniqueValue;
            LOG.debug("deleting {}", contentHolder);
            deletePath(contentHolder, SC_OK);
        }
    }

    /**
     * Performs queries for each of the created content and checks that all return results
     *
     * @param lanes list of lanes for which to run queries
     * @param uniqueValue the unique value to be used in queries
     * @return true if all the queries returned at least one result (all indices are up to date)
     * @throws ClientException if the http request failed
     * @throws InterruptedException to mark this method as waiting
     */
    private boolean searchContent(final List<String> lanes, final String uniqueValue)
            throws ClientException, InterruptedException {
        for (String lane : lanes) {
            if (!searchContentForIndex(lane, uniqueValue)) {
                return false;
            }
        }
        // Queries returned at least one result for each index
        return true;
    }

    /**
     * Tries all the known queries for a specific index lane,
     * until one of them returns at least one result.
     *
     * @param lane the indexing lane to query
     * @param uniqueValue the unique value to be used in queries
     * @return true if at least one query returned results
     * @throws ClientException if the http request fails
     * @throws InterruptedException to mark this method as waiting
     */
    private boolean searchContentForIndex(final String lane, final String uniqueValue)
            throws ClientException, InterruptedException {
        QueryClient queryClient = adaptTo(QueryClient.class);

        for (String query : QUERIES) {
            // prepare the query with the final values
            String indexName = getIndexName(lane);
            String effectiveQuery = replacePlaceholders(query, lane, uniqueValue);

            try {
                // Check query plan to make sure we use the good index
                String plan = queryClient.getPlan(effectiveQuery, QueryClient.QueryType.XPATH);
                if (plan.contains(indexName)) {
                    // The proper index is used, we can check the results
                    long results = queryClient.doCount(effectiveQuery, QueryClient.QueryType.XPATH);
                    if (results > 0) {
                        LOG.debug("Found {} results using query {}", results, effectiveQuery);
                        return true;
                    }
                } else {
                    LOG.debug("Did not find index {} in plan: {}", indexName, plan);
                    LOG.debug("Will try the next query, if available");
                }
            } catch (ClientException e) {
                if (e.getHttpStatusCode() == 400) {
                    LOG.debug("Unsupported query: {}", effectiveQuery);
                    LOG.debug("Will try the next query, if available");
                } else {
                    // We don't continue if there's another problem
                    throw e;
                }
            }
        }
        // No query returned results
        return false;
    }

    private String replacePlaceholders(String original, String lane, String value) {
        // Tags must be alphanumeric
        String tag = StringUtils.capitalize(lane.replaceAll("[^A-Za-z0-9]", ""));

        String result = original;
        result = StringUtils.replace(result, LANE_PLACEHOLDER, lane);
        result = StringUtils.replace(result, INDEX_NAME_PLACEHOLDER, INDEX_PREFIX + lane);
        result = StringUtils.replace(result, PROPERTY_PLACEHOLDER, PROPERTY_PREFIX + lane);
        result = StringUtils.replace(result, VALUE_PLACEHOLDER, VALUE_PREFIX + value);
        result = StringUtils.replace(result, TAG_PLACEHOLDER, TAG_PREFIX + tag);

        return result;
    }

    private String getIndexName(final String lane) {
        return INDEX_PREFIX + lane;
    }
}
