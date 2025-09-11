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

package org.bithon.server.storage.jdbc.clickhouse.web;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.component.commons.utils.HashUtils;
import org.bithon.server.storage.dashboard.Dashboard;
import org.bithon.server.storage.dashboard.DashboardFilter;
import org.bithon.server.storage.dashboard.DashboardListResult;
import org.bithon.server.storage.dashboard.DashboardStorageConfig;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseConfig;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseStorageProviderConfiguration;
import org.bithon.server.storage.jdbc.clickhouse.common.SecondaryIndex;
import org.bithon.server.storage.jdbc.clickhouse.common.TableCreator;
import org.bithon.server.storage.jdbc.common.jooq.Tables;
import org.bithon.server.storage.jdbc.dashboard.DashboardJdbcStorage;
import org.jooq.Condition;
import org.jooq.OrderField;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * @author Frank Chen
 * @date 19/8/22 2:16 pm
 */
@JsonTypeName("clickhouse")
public class DashboardStorage extends DashboardJdbcStorage {
    private final ClickHouseConfig config;

    @JsonCreator
    public DashboardStorage(@JacksonInject(useInput = OptBoolean.FALSE) ClickHouseStorageProviderConfiguration configuration,
                            @JacksonInject(useInput = OptBoolean.FALSE) DashboardStorageConfig storageConfig,
                            @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper objectMapper) {
        super(configuration.getDslContext(), storageConfig, objectMapper);
        this.config = configuration.getClickHouseConfig();
    }

    @Override
    public void initialize() {
        if (!this.storageConfig.isCreateTable()) {
            return;
        }

        new TableCreator(config, dslContext).useReplacingMergeTree(Tables.BITHON_WEB_DASHBOARD.LASTMODIFIED.getName())
                                            .partitionByExpression(null)
                                            .secondaryIndex(Tables.BITHON_WEB_DASHBOARD.LASTMODIFIED.getName(), new SecondaryIndex("minmax", 1))
                                            .secondaryIndex(Tables.BITHON_WEB_DASHBOARD.FOLDER.getName(), new SecondaryIndex("bloom_filter(0.001)", 1))
                                            .secondaryIndex(Tables.BITHON_WEB_DASHBOARD.TITLE.getName(), new SecondaryIndex("bloom_filter(0.001)", 1))
                                            .createIfNotExist(Tables.BITHON_WEB_DASHBOARD);
    }

    @Override
    public List<Dashboard> getDashboard(long afterTimestamp) {
        String sql = dslContext.selectFrom(Tables.BITHON_WEB_DASHBOARD)
                               .getSQL() + " FINAL WHERE ";
        sql += dslContext.renderInlined(Tables.BITHON_WEB_DASHBOARD.LASTMODIFIED.ge(new Timestamp(afterTimestamp).toLocalDateTime()));

        return dslContext.fetch(sql)
                         .stream()
                         .map(this::toDashboard)
                         .collect(Collectors.toList());
    }

    @Override
    public String put(String id, String folder, String title, String payload) {
        String signature = HashUtils.sha256Hex(payload);

        Timestamp now = new Timestamp(System.currentTimeMillis());

        dslContext.insertInto(Tables.BITHON_WEB_DASHBOARD)
                  .set(Tables.BITHON_WEB_DASHBOARD.ID, id)
                  .set(Tables.BITHON_WEB_DASHBOARD.FOLDER, folder)
                  .set(Tables.BITHON_WEB_DASHBOARD.TITLE, title)
                  .set(Tables.BITHON_WEB_DASHBOARD.PAYLOAD, payload)
                  .set(Tables.BITHON_WEB_DASHBOARD.SIGNATURE, signature)
                  .set(Tables.BITHON_WEB_DASHBOARD.CREATEDAT, now.toLocalDateTime())
                  .set(Tables.BITHON_WEB_DASHBOARD.LASTMODIFIED, now.toLocalDateTime())
                  .set(Tables.BITHON_WEB_DASHBOARD.DELETED, 0)
                  .execute();

        return signature;
    }

    @Override
    public void putIfNotExist(String id, String folder, String title, String payload) {
        String signature = HashUtils.sha256Hex(payload);

        if (dslContext.fetchCount(Tables.BITHON_WEB_DASHBOARD,
                                  Tables.BITHON_WEB_DASHBOARD.ID.eq(id)) > 0) {
            return;
        }

        Timestamp now = new Timestamp(System.currentTimeMillis());

        dslContext.insertInto(Tables.BITHON_WEB_DASHBOARD)
                  .set(Tables.BITHON_WEB_DASHBOARD.ID, id)
                  .set(Tables.BITHON_WEB_DASHBOARD.FOLDER, folder)
                  .set(Tables.BITHON_WEB_DASHBOARD.TITLE, title)
                  .set(Tables.BITHON_WEB_DASHBOARD.PAYLOAD, payload)
                  .set(Tables.BITHON_WEB_DASHBOARD.SIGNATURE, signature)
                  .set(Tables.BITHON_WEB_DASHBOARD.CREATEDAT, now.toLocalDateTime())
                  .set(Tables.BITHON_WEB_DASHBOARD.LASTMODIFIED, now.toLocalDateTime())
                  .set(Tables.BITHON_WEB_DASHBOARD.DELETED, 0)
                  .execute();
    }

    @Override
    public DashboardListResult getDashboards(DashboardFilter filter) {
        // For ClickHouse, we need to use FINAL for ReplacingMergeTree
        List<Condition> conditions = new ArrayList<>();

        // Exclude deleted dashboards unless explicitly requested
        if (!filter.isIncludeDeleted()) {
            conditions.add(Tables.BITHON_WEB_DASHBOARD.DELETED.eq(0));
        }

        // Apply folder filter
        if (filter.hasFolder()) {
            conditions.add(Tables.BITHON_WEB_DASHBOARD.FOLDER.eq(filter.getTrimmedFolder()));
        } else if (filter.hasFolderPrefix()) {
            conditions.add(Tables.BITHON_WEB_DASHBOARD.FOLDER.like(filter.getTrimmedFolderPrefix() + "%"));
        }

        // Apply search filter
        if (filter.hasSearch()) {
            String searchPattern = "%" + filter.getTrimmedSearch().toLowerCase(Locale.ENGLISH) + "%";
            if (filter.hasFolder()) {
                // Search only in title when folder is specified
                conditions.add(Tables.BITHON_WEB_DASHBOARD.TITLE.likeIgnoreCase(searchPattern));
            } else {
                // Search in both title and folder when no folder specified
                conditions.add(
                    Tables.BITHON_WEB_DASHBOARD.TITLE.likeIgnoreCase(searchPattern)
                                                     .or(Tables.BITHON_WEB_DASHBOARD.FOLDER.likeIgnoreCase(searchPattern))
                );
            }
        }

        // Build condition SQL
        String whereClause = conditions.isEmpty() ? "" : (" WHERE " +
                                                          conditions.stream()
                                                                    .map(dslContext::renderInlined)
                                                                    .reduce((a, b) -> a + " AND " + b)
                                                                    .orElse(""));

        // Count total results
        String countSql = "SELECT count() FROM " + Tables.BITHON_WEB_DASHBOARD.getName() + " FINAL" + whereClause;
        long totalElements = dslContext.fetch(countSql).get(0).get(0, Long.class);

        // Apply sorting
        OrderField<?> orderField = getSortField(filter.getSort(), filter.getOrder());
        String orderClause = " ORDER BY " + dslContext.renderInlined(orderField);

        // Apply pagination
        int validatedSize = filter.getValidatedSize();
        int validatedPage = filter.getValidatedPage();
        String limitClause = " LIMIT " + validatedSize + " OFFSET " + (validatedPage * validatedSize);

        // Execute main query
        String sql = "SELECT * FROM " + Tables.BITHON_WEB_DASHBOARD.getName() + " FINAL" +
                     whereClause + orderClause + limitClause;

        List<Dashboard> dashboards = dslContext.fetch(sql)
                                               .stream()
                                               .map(this::toDashboard)
                                               .collect(Collectors.toList());

        return DashboardListResult.of(dashboards, validatedPage, validatedSize, totalElements);
    }
}
