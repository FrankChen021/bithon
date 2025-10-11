/*
 *    Copyright 2020 bithon.org
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.bithon.server.web.service.dashboard.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.impl.AbstractSchema;
import org.bithon.component.commons.concurrency.NamedThreadFactory;
import org.bithon.component.commons.concurrency.ScheduledExecutorServiceFactory;
import org.bithon.component.commons.time.DateTime;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.dashboard.Dashboard;
import org.bithon.server.storage.dashboard.IDashboardStorage;
import org.bithon.server.web.service.WebServiceModuleEnabler;
import org.bithon.server.web.service.dashboard.api.GetDashboardListRequest;
import org.bithon.server.web.service.dashboard.api.GetDashboardListResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Frank Chen
 * @date 19/8/22 2:36 pm
 */
@Slf4j
@Service
@Conditional(WebServiceModuleEnabler.class)
@ConditionalOnBean(IDashboardStorage.class)
public class DashboardManager implements SmartLifecycle {

    private final IDashboardStorage storage;
    private final ObjectMapper objectMapper;

    private ScheduledExecutorService loaderScheduler;
    private long lastLoadAt;
    private final Map<String, Dashboard> dashboards = new ConcurrentHashMap<>(9);

    // Calcite connection for querying in-memory dashboards
    private Connection calciteConnection;

    public DashboardManager(IDashboardStorage storage, ObjectMapper objectMapper) {
        this.storage = storage;
        this.objectMapper = objectMapper;
        initializeCalciteConnection();
    }

    public void update(String id, String folder, String title, boolean visible, String payload) {
        String sig = this.storage.put(id, folder, title, payload);

        this.dashboards.put(id, Dashboard.builder()
                                         .id(id)
                                         .folder(folder)
                                         .title(title)
                                         .payload(payload)
                                         .signature(sig)
                                         .visible(visible)
                                         .build());
    }

    @Override
    public void start() {
        log.info("Starting dashboard incremental loader...");

        loaderScheduler = ScheduledExecutorServiceFactory.newSingleThreadScheduledExecutor(NamedThreadFactory.nonDaemonThreadFactory("dashboard-loader"));
        loaderScheduler.scheduleWithFixedDelay(this::incrementalLoad,
                                               // no delay to execute the first task
                                               0,
                                               30,
                                               TimeUnit.SECONDS);
    }

    @Override
    public void stop() {
        if (loaderScheduler != null) {
            loaderScheduler.shutdownNow();
            try {
                //noinspection ResultOfMethodCallIgnored
                loaderScheduler.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            } finally {
                loaderScheduler = null;
            }
        }

        // Close Calcite connection
        if (calciteConnection != null) {
            try {
                calciteConnection.close();
            } catch (SQLException e) {
                log.warn("Failed to close Calcite connection", e);
            }
        }
    }

    @Override
    public boolean isRunning() {
        return loaderScheduler != null && !loaderScheduler.isShutdown();
    }

    private void incrementalLoad() {
        long now = TimeSpan.now().toSeconds() * 1000;

        List<Dashboard> changedDashboards = storage.getDashboard(this.lastLoadAt);
        log.info("{} dashboards have been changed since {}.", changedDashboards.size(), DateTime.toYYYYMMDDhhmmss(this.lastLoadAt));

        if (!changedDashboards.isEmpty()) {
            for (Dashboard dashboard : changedDashboards) {
                if (dashboard.isDeleted()) {
                    this.dashboards.remove(dashboard.getId());
                } else {
                    // Set the title/folder into payload so that when a dashboard is fetched by id, the title/folder is always up-to-date
                    try {
                        ObjectNode payload = (ObjectNode) this.objectMapper.readTree(dashboard.getPayload());
                        payload.set("title", new TextNode(dashboard.getTitle()));
                        payload.set("folder", new TextNode(dashboard.getFolder()));
                        dashboard.setPayload(this.objectMapper.writeValueAsString(payload));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                    this.dashboards.put(dashboard.getId(), dashboard);
                }
            }
        }

        this.lastLoadAt = now;
    }

    public Dashboard getDashboard(String boardName) {
        return dashboards.get(boardName);
    }

    public List<Dashboard> getAllDashboards() {
        return new ArrayList<>(dashboards.values());
    }

    /**
     * Initialize Calcite JDBC connection for querying in-memory dashboard data
     */
    private void initializeCalciteConnection() {
        try {
            Properties info = new Properties();
            info.setProperty("lex", "JAVA");

            calciteConnection = DriverManager.getConnection("jdbc:calcite:", info);
            CalciteConnection calciteConn = calciteConnection.unwrap(CalciteConnection.class);
            SchemaPlus rootSchema = calciteConn.getRootSchema();

            // Add dashboard schema with anonymous class
            SchemaPlus dashboardSchema = rootSchema.add("dashboard", new AbstractSchema() {
                // Empty anonymous implementation, tables are added manually
            });
            dashboardSchema.add("dashboards", new DashboardCalciteTable(dashboards));

            log.info("Calcite connection initialized for dashboard queries");
        } catch (SQLException e) {
            log.error("Failed to initialize Calcite connection", e);
            throw new RuntimeException("Failed to initialize dashboard query engine", e);
        }
    }


    /**
     * Build secure SQL with parameterized queries where possible
     */
    private static class SecureSqlBuilder {
        // Allowlist of valid sort columns to prevent SQL injection
        private static final Set<String> ALLOWED_SORT_COLUMNS = Set.of(
            "id", "title", "folder", "signature", "createdAt", "lastModified"
        );

        // Valid order directions
        private static final Set<String> ALLOWED_ORDER_DIRECTIONS = Set.of("asc", "desc");

        // Maximum length for search and folder inputs to prevent extremely long malicious inputs
        private static final int MAX_INPUT_LENGTH = 1000;

        private final StringBuilder sql = new StringBuilder();
        private final List<Object> parameters = new ArrayList<>();

        public SecureSqlBuilder append(String sqlFragment) {
            sql.append(sqlFragment);
            return this;
        }

        public SecureSqlBuilder appendParameter(String paramPlaceholder, Object value) {
            sql.append(paramPlaceholder);
            parameters.add(value);
            return this;
        }

        public SecureSqlBuilder orderBy(String sortColumn, String orderDirection) {
            sql.append(" ORDER BY ")
               .append(validateSortColumn(sortColumn))
               .append(" ")
               .append(validateOrderDirection(orderDirection));
            return this;
        }

        public SecureSqlBuilder limit(int limit) {
            sql.append(" LIMIT ").append(limit);
            return this;
        }

        public SecureSqlBuilder offset(int offset) {
            sql.append(" OFFSET ").append(offset);
            return this;
        }

        public PreparedStatement prepareStatement(Connection connection) throws SQLException {
            PreparedStatement stmt = connection.prepareStatement(sql.toString());
            try {
                for (int i = 0; i < parameters.size(); i++) {
                    stmt.setObject(i + 1, parameters.get(i));
                }
                return stmt;
            } catch (SQLException e) {
                // Ensure the PreparedStatement is closed if parameter binding fails
                try {
                    stmt.close();
                } catch (SQLException closeException) {
                    // Add the close exception as suppressed to preserve the original exception
                    e.addSuppressed(closeException);
                }
                throw e;
            }
        }

        /**
         * Build secure SQL query with proper input validation and parameterization
         */
        public static SecureSqlBuilder from(GetDashboardListRequest filter, boolean isCountQuery) {
            // Validate inputs
            validateInput(filter.getSearch(), "search");
            validateInput(filter.getFolder(), "folder");

            SecureSqlBuilder builder = new SecureSqlBuilder();

            if (isCountQuery) {
                builder.append("SELECT COUNT(*) as total FROM dashboard.dashboards");
            } else {
                builder.append("SELECT id, title, folder, signature, createdAt, lastModified FROM dashboard.dashboards");
            }

            builder.append(" WHERE 1=1 AND visible = true");

            // Add search filter with parameterization
            if (StringUtils.hasText(filter.getSearch())) {
                builder.append(" AND (title ILIKE ").appendParameter("?", "%" + filter.getSearch() + "%").append(")");
            }

            // Add folder filter with parameterization  
            if (StringUtils.hasText(filter.getFolder())) {
                builder.append(" AND folder = ").appendParameter("?", filter.getFolder());
            }

            if (StringUtils.hasText(filter.getId())) {
                builder.append(" AND id = ").appendParameter("?", filter.getId());
            }

            return builder;
        }

        /**
         * Validate and sanitize user input to prevent SQL injection
         */
        private static void validateInput(String input, String fieldName) {
            if (input != null && input.length() > MAX_INPUT_LENGTH) {
                throw new IllegalArgumentException(fieldName + " exceeds maximum length of " + MAX_INPUT_LENGTH);
            }
        }

        /**
         * Validate sort column against allowlist
         */
        private static String validateSortColumn(String sortColumn) {
            if (StringUtils.hasText(sortColumn)) {
                String normalizedSort = sortColumn.toLowerCase(Locale.ENGLISH).trim();
                if (ALLOWED_SORT_COLUMNS.contains(normalizedSort)) {
                    return normalizedSort;
                }
            }
            return "title"; // Default safe sort column
        }

        /**
         * Validate order direction against allowlist
         */
        private static String validateOrderDirection(String order) {
            if (StringUtils.hasText(order)) {
                String normalizedOrder = order.toLowerCase(Locale.ENGLISH).trim();
                if (ALLOWED_ORDER_DIRECTIONS.contains(normalizedOrder)) {
                    return normalizedOrder;
                }
            }
            return "asc"; // Default safe order
        }
    }


    /**
     * Use Calcite-based query from in-memory data with secure SQL building
     */
    public GetDashboardListResponse getDashboards(GetDashboardListRequest filter) {
        try {
            // Build secure query with ordering and pagination
            SecureSqlBuilder builder = SecureSqlBuilder.from(filter, false)
                                                       .orderBy(filter.getSort(), filter.getOrder());

            // Add pagination if specified
            if (filter.getSize() > 0) {
                builder.limit(filter.getSize());
                if (filter.getPage() > 0) {
                    builder.offset(filter.getPage() * filter.getSize());
                }
            }

            List<Dashboard> results = new ArrayList<>();
            try (PreparedStatement stmt = builder.prepareStatement(calciteConnection);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    Dashboard dashboard = Dashboard.builder()
                                                   .id(rs.getString("id"))
                                                   .title(rs.getString("title"))
                                                   .folder(rs.getString("folder"))
                                                   .signature(rs.getString("signature"))
                                                   .createdAt(rs.getTimestamp("createdAt"))
                                                   .lastModified(rs.getTimestamp("lastModified"))
                                                   .deleted(false) // Since we filter out deleted dashboards in the WHERE clause
                                                   .payload(null) // Payload is not included in list API responses
                                                   .build();
                    results.add(dashboard);
                }
            }

            // Get total count for pagination
            long totalCount = getTotalCount(filter);

            return GetDashboardListResponse.of(results, filter.getPage(), filter.getSize(), totalCount);

        } catch (SQLException e) {
            log.error("Failed to query dashboards with Calcite", e);
            throw new RuntimeException("Dashboard query failed", e);
        }
    }

    /**
     * Get total count for pagination using secure SQL over Calcite
     */
    private long getTotalCount(GetDashboardListRequest filter) {
        try {
            SecureSqlBuilder builder = SecureSqlBuilder.from(filter, true);

            try (PreparedStatement stmt = builder.prepareStatement(calciteConnection);
                 ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0;
            }

        } catch (SQLException e) {
            log.error("Failed to get total count with Calcite", e);
            throw new RuntimeException("Dashboard count query failed", e);
        }
    }

}
