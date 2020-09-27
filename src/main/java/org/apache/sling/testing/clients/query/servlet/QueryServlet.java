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
package org.apache.sling.testing.clients.query.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;

import javax.jcr.Session;
import javax.jcr.query.*;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Date;

import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_METHODS;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_PATHS;

@Component(
        name = QueryServlet.SERVLET_NAME,
        service = {Servlet.class},
        property = {
                SLING_SERVLET_PATHS + "=" + QueryServlet.SERVLET_PATH,
                SLING_SERVLET_METHODS + "=GET"
        }
)
public class QueryServlet extends SlingSafeMethodsServlet {
    private static final long serialVersionUID = 1L;

    public static final String SERVLET_PATH = "/system/testing/query";
    public static final String SERVLET_NAME = "sling.testing.clients.query.servlet";

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            final QueryManager qm = request.getResourceResolver().adaptTo(Session.class)
                    .getWorkspace().getQueryManager();

            long before = 0;
            long after = 0;
            long total = 0;

            String query = request.getParameter("query");
            String type = request.getParameter("type");

            // default for showResults is true, unless parameter is matching exactly "false"
            boolean showResults = !("false".equalsIgnoreCase(request.getParameter("showresults")));
            // default for explainQuery is false, unless parameter is present and is not matching "false"
            String explainParam = request.getParameter("explain");
            boolean explainQuery = (explainParam != null) && !("false".equalsIgnoreCase(explainParam));

            boolean tidy = false;
            for (String selector : request.getRequestPathInfo().getSelectors()) {
                if ("tidy".equals(selector)) {
                    tidy = true;
                }
            }

            if ((query == null) || query.equals("") || (type == null) || type.equals("")) {
                response.sendError(400, "Parameters query and type are required"); // invalid request
                return;
            }

            // prepare
            if (explainQuery) {
                query = "explain " + query;
            }

            Query q = qm.createQuery(query, type);

            // execute
            before = new Date().getTime();
            QueryResult result = q.execute();
            after = new Date().getTime();

            // collect results
            String firstSelector = null;
            if (result.getSelectorNames().length > 1) {
                firstSelector = result.getSelectorNames()[0];
                try {
                    String[] columnNames = result.getColumnNames();
                    if (columnNames.length > 0) {
                        String firstColumnName = columnNames[0];
                        int firstDot = firstColumnName.indexOf('.');
                        if (firstDot > 0) {
                            firstSelector = firstColumnName.substring(0, firstDot);
                        }
                    }
                } catch (Exception ignored) {
                }
            }

            ObjectMapper mapper = new ObjectMapper();
            ObjectNode responseJson = mapper.createObjectNode();

            if (explainQuery) {
                responseJson.put("plan", result.getRows().nextRow().getValue("plan").getString());
            } else if (showResults) {
                ArrayNode results = mapper.createArrayNode();

                RowIterator rows = result.getRows();
                while (rows.hasNext()) {
                    Row row = rows.nextRow();
                    String rowPath = (firstSelector != null) ? row.getPath(firstSelector) : row.getPath();
                    String rowType = (firstSelector != null)
                            ? row.getNode(firstSelector).getPrimaryNodeType().getName()
                            : row.getNode().getPrimaryNodeType().getName();

                    ObjectNode rowJson = mapper.createObjectNode();
                    rowJson.put("path", rowPath);
                    rowJson.put("type", rowType);
                    results.add(rowJson);

                    total++;
                }

                responseJson.set("results", results);
            } else {
                // only count results
                RowIterator rows = result.getRows();
                while (rows.hasNext()) {
                    rows.nextRow();
                    total++;
                }
            }

            responseJson.put("total", total);
            responseJson.put("time", after - before);

            if (tidy) {
                response.getWriter().write(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(responseJson));
            } else {
                response.getWriter().write(responseJson.toString());
            }

        } catch (InvalidQueryException e) {
            // Consider InvalidQueryException as an invalid request instead of sending 500 server error
            response.sendError(400, e.getMessage());
            e.printStackTrace(response.getWriter());
        } catch (final Exception e) {
            response.sendError(500, e.getMessage());
            e.printStackTrace(response.getWriter());
        }
    }
}
