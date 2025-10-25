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
import org.bithon.server.storage.dashboard.DashboardStorageConfig;
import org.bithon.server.storage.dashboard.IDashboardStorage;
import org.bithon.server.storage.jdbc.JdbcStorageProviderConfiguration;
import org.bithon.server.storage.jdbc.common.jooq.Tables;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.dao.DuplicateKeyException;

import java.sql.Timestamp;
import java.util.List;

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
                         // NOTE: DELETED is not checked here because the application side will use the flag to sync data in memory
                         .where(Tables.BITHON_WEB_DASHBOARD.LASTMODIFIED.ge(new Timestamp(afterTimestamp).toLocalDateTime()))
                         .fetch(this::toDashboard);
    }

    @Override
    public String put(String id, String folder, String title, boolean visible, String payload) {
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
                      .set(Tables.BITHON_WEB_DASHBOARD.VISIBLE, visible ? 1 : 0)
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
                      .set(Tables.BITHON_WEB_DASHBOARD.VISIBLE, visible ? 1 : 0)
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
                      .set(Tables.BITHON_WEB_DASHBOARD.VISIBLE, 1)
                      .execute();
        } catch (DuplicateKeyException ignored) {
        }
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
        dashboard.setVisible(record.get(Tables.BITHON_WEB_DASHBOARD.VISIBLE) == 1);

        var lastModified = record.get(Tables.BITHON_WEB_DASHBOARD.LASTMODIFIED);
        if (lastModified != null) {
            dashboard.setLastModified(Timestamp.valueOf(lastModified));
        }

        return dashboard;
    }
}
