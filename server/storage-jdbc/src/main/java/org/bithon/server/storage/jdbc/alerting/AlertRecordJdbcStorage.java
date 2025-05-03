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

package org.bithon.server.storage.jdbc.alerting;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.server.datasource.query.Interval;
import org.bithon.server.datasource.query.Limit;
import org.bithon.server.storage.alerting.AlertingStorageConfiguration;
import org.bithon.server.storage.alerting.IAlertRecordStorage;
import org.bithon.server.storage.alerting.pojo.AlertRecordObject;
import org.bithon.server.storage.alerting.pojo.ListResult;
import org.bithon.server.storage.common.expiration.ExpirationConfig;
import org.bithon.server.storage.common.expiration.IExpirationRunnable;
import org.bithon.server.storage.jdbc.JdbcStorageProviderConfiguration;
import org.bithon.server.storage.jdbc.common.jooq.Tables;
import org.jooq.DSLContext;
import org.jooq.Select;
import org.jooq.SelectConditionStep;
import org.jooq.SelectWhereStep;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/5
 */
public class AlertRecordJdbcStorage implements IAlertRecordStorage {

    protected final DSLContext dslContext;
    protected final AlertingStorageConfiguration.AlertStorageConfig storageConfig;
    protected final ObjectMapper objectMapper;

    @JsonCreator
    public AlertRecordJdbcStorage(@JacksonInject(useInput = OptBoolean.FALSE) JdbcStorageProviderConfiguration storageProvider,
                                  @JacksonInject(useInput = OptBoolean.FALSE) AlertingStorageConfiguration.AlertStorageConfig storageConfig,
                                  @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper objectMapper) {
        this(storageProvider.getDslContext(), storageConfig, objectMapper);
    }

    public AlertRecordJdbcStorage(DSLContext dslContext,
                                  AlertingStorageConfiguration.AlertStorageConfig storageConfig,
                                  ObjectMapper objectMapper) {
        this.dslContext = dslContext;
        this.storageConfig = storageConfig;
        this.objectMapper = objectMapper;
    }

    public void initialize() {
        if (!this.storageConfig.isCreateTable()) {
            return;
        }
        this.dslContext.createTableIfNotExists(Tables.BITHON_ALERT_RECORD)
                       .columns(Tables.BITHON_ALERT_RECORD.fields())
                       .indexes(Tables.BITHON_ALERT_RECORD.getIndexes())
                       .execute();
    }

    @Override
    public Timestamp getLastAlert(String alertId) {
        LocalDateTime lastAlertAt = dslContext.select(Tables.BITHON_ALERT_STATE.LAST_ALERT_AT)
                                              .from(Tables.BITHON_ALERT_STATE)
                                              .where(Tables.BITHON_ALERT_STATE.ALERT_ID.eq(alertId))
                                              .fetchOne(Tables.BITHON_ALERT_STATE.LAST_ALERT_AT);
        return lastAlertAt == null ? null : Timestamp.valueOf(lastAlertAt);
    }

    @Override
    public void addAlertRecord(AlertRecordObject record) {
        dslContext.insertInto(Tables.BITHON_ALERT_RECORD)
                  .set(Tables.BITHON_ALERT_RECORD.APP_NAME, record.getAppName())
                  .set(Tables.BITHON_ALERT_RECORD.ALERT_NAME, record.getAlertName())
                  .set(Tables.BITHON_ALERT_RECORD.NAMESPACE, record.getNamespace())
                  .set(Tables.BITHON_ALERT_RECORD.ALERT_ID, record.getAlertId())
                  .set(Tables.BITHON_ALERT_RECORD.PAYLOAD, record.getPayload())
                  .set(Tables.BITHON_ALERT_RECORD.DATA_SOURCE, record.getDataSource())
                  .set(Tables.BITHON_ALERT_RECORD.NOTIFICATION_STATUS, record.getNotificationStatus())
                  .set(Tables.BITHON_ALERT_RECORD.RECORD_ID, record.getRecordId())
                  .set(Tables.BITHON_ALERT_RECORD.CREATED_AT, record.getCreatedAt().toLocalDateTime())
                  .execute();
    }

    @Override
    public ListResult<AlertRecordObject> getAlertRecords(String alertId, Interval interval, Limit limit) {
        Select<?> sql = dslContext.selectFrom(Tables.BITHON_ALERT_RECORD);
        if (alertId != null) {
            sql = ((SelectWhereStep<?>) sql).where(Tables.BITHON_ALERT_RECORD.ALERT_ID.eq(alertId));
        }

        if (interval != null) {
            sql = ((SelectWhereStep<?>) sql).where(Tables.BITHON_ALERT_RECORD.CREATED_AT.ge(interval.getStartTime().toTimestamp().toLocalDateTime()))
                                            .and(Tables.BITHON_ALERT_RECORD.CREATED_AT.lt(interval.getEndTime().toTimestamp().toLocalDateTime()));
        }

        int total = dslContext.selectCount().from(sql).fetchOne(0, int.class);

        if (limit != null) {
            sql = ((SelectConditionStep<?>) sql).orderBy(Tables.BITHON_ALERT_RECORD.CREATED_AT.desc())
                                                .limit(limit.getLimit())
                                                .offset(limit.getOffset());
        }

        return new ListResult<>(total, sql.fetchInto(AlertRecordObject.class));
    }

    @Override
    public AlertRecordObject getAlertRecord(String id) {
        return dslContext.selectFrom(Tables.BITHON_ALERT_RECORD).where(Tables.BITHON_ALERT_RECORD.RECORD_ID.eq(id)).fetchOneInto(AlertRecordObject.class);
    }

    @Override
    public List<AlertRecordObject> getRecordsByNotificationStatus(int statusCode) {
        return dslContext.selectFrom(Tables.BITHON_ALERT_RECORD)
                         .where(Tables.BITHON_ALERT_RECORD.NOTIFICATION_STATUS.eq(statusCode))
                         .fetchInto(AlertRecordObject.class);
    }

    @Override
    public void setNotificationResult(String id, int statusCode, String status) {
        dslContext.update(Tables.BITHON_ALERT_RECORD)
                  .set(Tables.BITHON_ALERT_RECORD.NOTIFICATION_STATUS, statusCode)
                  .set(Tables.BITHON_ALERT_RECORD.NOTIFICATION_RESULT, status)
                  .where(Tables.BITHON_ALERT_RECORD.RECORD_ID.eq(id))
                  .execute();
    }

    @Override
    public IExpirationRunnable getExpirationRunnable() {
        return new IExpirationRunnable() {
            @Override
            public ExpirationConfig getExpirationConfig() {
                return storageConfig.getTtl();
            }

            @Override
            public void expire(Timestamp before) {
                dslContext.deleteFrom(Tables.BITHON_ALERT_RECORD)
                          .where(Tables.BITHON_ALERT_RECORD.CREATED_AT.le(before.toLocalDateTime()))
                          .execute();
            }
        };
    }

    @Override
    public String getName() {
        return "alert-record";
    }
}
