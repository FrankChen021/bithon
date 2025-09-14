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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

import lombok.extern.slf4j.Slf4j;

/**
 * @author Frank Chen
 * @date 19/8/22 2:36 pm
 */
@Slf4j
@Service
@Conditional(WebServiceModuleEnabler.class)
@ConditionalOnBean(IDashboardStorage.class)
public class DashboardManager implements SmartLifecycle {

    public interface IDashboardChangedListener {
        void onChanged();
    }

    private final IDashboardStorage storage;
    private ScheduledExecutorService loaderScheduler;
    private long lastLoadAt;
    private final Map<String, Dashboard> dashboards = new ConcurrentHashMap<>(9);

    private final List<IDashboardChangedListener> listeners = Collections.synchronizedList(new ArrayList<>());

    // Calcite connection for querying in-memory dashboards
    private Connection calciteConnection;

    public DashboardManager(IDashboardStorage storage) {
        this.storage = storage;
        initializeCalciteConnection();
    }

    public void update(String id, String folder, String title, String payload) {
        String sig = this.storage.put(id, folder, title, payload);

        this.dashboards.put(id, Dashboard.builder()
                                         .id(id)
                                         .folder(folder)
                                         .title(title)
                                         .payload(payload)
                                         .signature(sig)
                                         .build());
        this.onChanged();
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
                    this.dashboards.put(dashboard.getId(), dashboard);
                }
            }

            onChanged();
        }

        this.lastLoadAt = now;
    }

    public Dashboard getDashboard(String boardName) {
        return dashboards.get(boardName);
    }

    public List<Dashboard> getAllDashboards() {
        return new ArrayList<>(dashboards.values());
    }

    private void onChanged() {
        IDashboardChangedListener[] listeners = this.listeners.toArray(new IDashboardChangedListener[0]);
        for (IDashboardChangedListener listener : listeners) {
            try {
                listener.onChanged();
            } catch (Exception e) {
                log.error("onChanged", e);
            }
        }
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
     * Build SQL WHERE clauses based on filter criteria
     */
    private String toDashboardSQLFilter(GetDashboardListRequest filter) {
        StringBuilder whereClause = new StringBuilder(" WHERE 1=1 AND visible = true");

        // Build WHERE clauses based on filter
        if (StringUtils.hasText(filter.getSearch())) {
            String escapedSearch = filter.getSearch().replace("'", "''");
            whereClause.append(StringUtils.format(" AND (title ILIKE '%%%s%%')", escapedSearch));
        }

        if (StringUtils.hasText(filter.getFolder())) {
            String escapedFolder = filter.getFolder().replace("'", "''");
            whereClause.append(StringUtils.format(" AND folder = '%s'", escapedFolder));
        }

        // Note: deleted filtering is handled at the application level since 'deleted' field is not in the Calcite table

        return whereClause.toString();
    }

    /**
     * Use Calcite-based query from in-memory data
     */
    public GetDashboardListResponse getDashboards(GetDashboardListRequest filter) {
        try {
            StringBuilder sqlBuilder = new StringBuilder("SELECT id, title, folder, signature, createdAt, lastModified, visible FROM dashboard.dashboards");
 
            // Add shared WHERE clause
            sqlBuilder.append(toDashboardSQLFilter(filter));

            // Add ordering
            if (StringUtils.hasText(filter.getSort())) {
                if ("desc".equalsIgnoreCase(filter.getOrder())) {
                    sqlBuilder.append(StringUtils.format(" ORDER BY %s DESC", filter.getSort()));
                } else {
                    sqlBuilder.append(StringUtils.format(" ORDER BY %s", filter.getSort()));
                }
            }

            // Add pagination
            if (filter.getSize() > 0) {
                if (filter.getPage() > 0) {
                    sqlBuilder.append(StringUtils.format(" LIMIT %d OFFSET %d", filter.getSize(), filter.getPage() * filter.getSize()));
                } else {
                    sqlBuilder.append(StringUtils.format(" LIMIT %d", filter.getSize()));
                }
            }

            List<Dashboard> results = new ArrayList<>();
            try (Statement stmt = calciteConnection.createStatement();
                 ResultSet rs = stmt.executeQuery(sqlBuilder.toString())) {

                while (rs.next()) {
                    Dashboard dashboard = Dashboard.builder()
                        .id(rs.getString("id"))
                        .title(rs.getString("title"))
                        .folder(rs.getString("folder"))
                        .signature(rs.getString("signature"))
                        .createdAt(rs.getTimestamp("createdAt"))
                        .lastModified(rs.getTimestamp("lastModified"))
                        .visible(rs.getBoolean("visible"))
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
     * Get total count for pagination using SQL over Calcite
     */
    private long getTotalCount(GetDashboardListRequest filter) {
        try {
            String sql = "SELECT COUNT(*) as total FROM dashboard.dashboards" + toDashboardSQLFilter(filter);

            try (Statement stmt = calciteConnection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

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
