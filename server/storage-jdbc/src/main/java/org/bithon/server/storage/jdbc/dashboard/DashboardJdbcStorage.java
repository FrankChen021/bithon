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

package org.bithon.server.storage.jdbc.dashboard;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.component.commons.utils.HashUtils;
import org.bithon.server.storage.dashboard.Dashboard;
import org.bithon.server.storage.dashboard.DashboardFilter;
import org.bithon.server.storage.dashboard.DashboardListResult;
import org.bithon.server.storage.dashboard.DashboardStorageConfig;
import org.bithon.server.storage.dashboard.FolderInfo;
import org.bithon.server.storage.dashboard.IDashboardStorage;
import org.bithon.server.storage.jdbc.JdbcStorageProviderConfiguration;
import org.bithon.server.storage.jdbc.common.jooq.Tables;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.OrderField;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.dao.DuplicateKeyException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author Frank Chen
 * @date 19/8/22 12:41 pm
 */
public class DashboardJdbcStorage implements IDashboardStorage {

    protected final DSLContext dslContext;
    protected final DashboardStorageConfig storageConfig;
    protected final ObjectMapper objectMapper;

    @JsonCreator
    public DashboardJdbcStorage(@JacksonInject(useInput = OptBoolean.FALSE) JdbcStorageProviderConfiguration providerConfiguration,
                                @JacksonInject(useInput = OptBoolean.FALSE) DashboardStorageConfig storageConfig,
                                @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper objectMapper) {
        this(providerConfiguration.getDslContext(), storageConfig, objectMapper);
    }

    public DashboardJdbcStorage(DSLContext dslContext, DashboardStorageConfig storageConfig) {
        this(dslContext, storageConfig, new ObjectMapper());
    }

    public DashboardJdbcStorage(DSLContext dslContext, DashboardStorageConfig storageConfig, ObjectMapper objectMapper) {
        this.dslContext = dslContext;
        this.storageConfig = storageConfig;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<Dashboard> getDashboard(long afterTimestamp) {
        return dslContext.selectFrom(Tables.BITHON_WEB_DASHBOARD)
                         .where(Tables.BITHON_WEB_DASHBOARD.LASTMODIFIED.ge(new Timestamp(afterTimestamp).toLocalDateTime()))
                         .fetch(this::toDashboard);
    }

    @Override
    public String put(String id, String folder, String title, String payload) {
        String signature = HashUtils.sha256Hex(payload);

        Timestamp now = new Timestamp(System.currentTimeMillis());

        // onDuplicateKeyIgnore is not supported on all DB
        // use try-catch instead
        try {
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
        } catch (DuplicateKeyException ignored) {
            // try to update if duplicated
            dslContext.update(Tables.BITHON_WEB_DASHBOARD)
                      .set(Tables.BITHON_WEB_DASHBOARD.FOLDER, folder)
                      .set(Tables.BITHON_WEB_DASHBOARD.TITLE, title)
                      .set(Tables.BITHON_WEB_DASHBOARD.PAYLOAD, payload)
                      .set(Tables.BITHON_WEB_DASHBOARD.SIGNATURE, signature)
                      .set(Tables.BITHON_WEB_DASHBOARD.LASTMODIFIED, now.toLocalDateTime())
                      .set(Tables.BITHON_WEB_DASHBOARD.DELETED, 0)
                      .where(Tables.BITHON_WEB_DASHBOARD.ID.eq(id))
                      .execute();
        }
        return signature;
    }

    @Override
    public void putIfNotExist(String id, String folder, String title, String payload) {
        String signature = HashUtils.sha256Hex(payload);

        Timestamp now = new Timestamp(System.currentTimeMillis());

        // onDuplicateKeyIgnore is not supported on all DB
        // use try-catch insteadØ
        try {
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
        } catch (DuplicateKeyException ignored) {
        }
    }

    @Override
    public DashboardListResult getDashboards(DashboardFilter filter) {
        // Build where conditions
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

        // Count total results
        Long totalElements = dslContext.selectCount()
                                       .from(Tables.BITHON_WEB_DASHBOARD)
                                       .where(conditions)
                                       .fetchOne(0, long.class);

        // Apply sorting
        OrderField<?> orderField = getSortField(filter.getSort(), filter.getOrder());

        // Apply pagination
        int validatedSize = filter.getValidatedSize();
        int validatedPage = filter.getValidatedPage();

        // Execute query with pagination and sorting
        List<Dashboard> dashboards = dslContext.selectFrom(Tables.BITHON_WEB_DASHBOARD)
                                               .where(conditions)
                                               .orderBy(orderField)
                                               .limit(validatedSize)
                                               .offset(validatedPage * validatedSize)
                                               .fetch(this::toDashboard);

        return DashboardListResult.of(dashboards, validatedPage, validatedSize, totalElements == null ? 0 : totalElements);
    }

    @Override
    public List<FolderInfo> getFolderStructure(int maxDepth) {
        // Get all unique folders with their dashboard counts
        var folderRecords = dslContext.select(
                                          Tables.BITHON_WEB_DASHBOARD.FOLDER,
                                          DSL.count().as("count")
                                      )
                                      .from(Tables.BITHON_WEB_DASHBOARD)
                                      .where(Tables.BITHON_WEB_DASHBOARD.DELETED.eq(0))
                                      .and(Tables.BITHON_WEB_DASHBOARD.FOLDER.isNotNull())
                                      .and(Tables.BITHON_WEB_DASHBOARD.FOLDER.ne(""))
                                      .groupBy(Tables.BITHON_WEB_DASHBOARD.FOLDER)
                                      .fetch();

        // Build folder tree structure
        Map<String, FolderInfo> folderMap = new HashMap<>();
        List<FolderInfo> rootFolders = new ArrayList<>();

        for (var record : folderRecords) {
            String folderPath = record.get(Tables.BITHON_WEB_DASHBOARD.FOLDER);
            long count = record.get("count", Long.class);

            if (folderPath == null || folderPath.trim().isEmpty()) {
                continue;
            }

            String[] pathParts = folderPath.split("/");
            if (pathParts.length > maxDepth) {
                continue;
            }

            // Build folder hierarchy
            StringBuilder currentPath = new StringBuilder();
            FolderInfo parent = null;

            for (int i = 0; i < pathParts.length; i++) {
                if (i > 0) {
                    currentPath.append("/");
                }
                currentPath.append(pathParts[i]);

                String fullPath = currentPath.toString();
                FolderInfo folder = folderMap.get(fullPath);

                if (folder == null) {
                    folder = FolderInfo.builder()
                                       .path(fullPath)
                                       .name(pathParts[i])
                                       .depth(i)
                                       .parentPath(parent != null ? parent.getPath() : null)
                                       .children(new ArrayList<>())
                                       .dashboardCount(0)
                                       .build();

                    folderMap.put(fullPath, folder);

                    if (parent == null) {
                        rootFolders.add(folder);
                    } else {
                        parent.getChildren().add(folder);
                    }
                }

                // Add count only to the leaf folder
                if (i == pathParts.length - 1) {
                    folder.setDashboardCount(count);
                }

                parent = folder;
            }
        }

        return rootFolders;
    }

    @Override
    public void initialize() {
        if (!this.storageConfig.isCreateTable()) {
            return;
        }

        this.dslContext.createTableIfNotExists(Tables.BITHON_WEB_DASHBOARD)
                       .columns(Tables.BITHON_WEB_DASHBOARD.fields())
                       .indexes(Tables.BITHON_WEB_DASHBOARD.getIndexes())
                       .execute();
    }

    protected Dashboard toDashboard(Record record) {
        Dashboard dashboard = new Dashboard();
        dashboard.setId(record.get(Tables.BITHON_WEB_DASHBOARD.ID));
        dashboard.setPayload(record.get(Tables.BITHON_WEB_DASHBOARD.PAYLOAD));
        dashboard.setCreatedAt(Timestamp.valueOf(record.get(Tables.BITHON_WEB_DASHBOARD.CREATEDAT)));
        dashboard.setSignature(record.get(Tables.BITHON_WEB_DASHBOARD.SIGNATURE));
        dashboard.setDeleted(record.get(Tables.BITHON_WEB_DASHBOARD.DELETED) == 1);
        dashboard.setTitle(record.get(Tables.BITHON_WEB_DASHBOARD.TITLE));
        dashboard.setFolder(record.get(Tables.BITHON_WEB_DASHBOARD.FOLDER));

        var lastModified = record.get(Tables.BITHON_WEB_DASHBOARD.LASTMODIFIED);
        if (lastModified != null) {
            dashboard.setLastModified(Timestamp.valueOf(lastModified));
        }

        return dashboard;
    }

    protected OrderField<?> getSortField(String sort, String order) {
        Field<?> field = switch (sort.toLowerCase(Locale.ENGLISH)) {
            case "folder" -> Tables.BITHON_WEB_DASHBOARD.FOLDER;
            case "lastmodified", "last_modified" -> Tables.BITHON_WEB_DASHBOARD.LASTMODIFIED;
            case "id" -> Tables.BITHON_WEB_DASHBOARD.ID;
            default -> Tables.BITHON_WEB_DASHBOARD.TITLE;
        };

        return "desc".equalsIgnoreCase(order) ? field.desc() : field.asc();
    }
}
