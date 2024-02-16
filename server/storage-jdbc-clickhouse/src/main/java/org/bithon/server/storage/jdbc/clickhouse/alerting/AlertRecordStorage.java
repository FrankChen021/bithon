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

package org.bithon.server.storage.jdbc.clickhouse.alerting;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.alerting.AlertingStorageConfiguration;
import org.bithon.server.storage.alerting.pojo.AlertRecordObject;
import org.bithon.server.storage.common.expiration.ExpirationConfig;
import org.bithon.server.storage.common.expiration.IExpirationRunnable;
import org.bithon.server.storage.jdbc.alerting.AlertRecordJdbcStorage;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseConfig;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseStorageProviderConfiguration;
import org.bithon.server.storage.jdbc.clickhouse.common.DataCleaner;
import org.bithon.server.storage.jdbc.clickhouse.common.TableCreator;
import org.bithon.server.storage.jdbc.common.jooq.Tables;

import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/5
 */
@JsonTypeName("clickhouse")
public class AlertRecordStorage extends AlertRecordJdbcStorage {

    private final ClickHouseConfig clickHouseConfig;

    @JsonCreator
    public AlertRecordStorage(@JacksonInject(useInput = OptBoolean.FALSE) ClickHouseStorageProviderConfiguration storageProvider,
                              @JacksonInject(useInput = OptBoolean.FALSE) AlertingStorageConfiguration.AlertStorageConfig storageConfig) {
        super(storageProvider.getDslContext(), storageConfig);
        this.clickHouseConfig = storageProvider.getClickHouseConfig();
    }

    @Override
    public void initialize() {
        if (!this.storageConfig.isCreateTable()) {
            return;
        }
        new TableCreator(this.clickHouseConfig, dslContext)
            .partitionByExpression(StringUtils.format("toYYYYMMDD(%s)", Tables.BITHON_ALERT_RECORD.CREATED_AT.getName()))
            .createIfNotExist(Tables.BITHON_ALERT_RECORD);
    }

    @Override
    public Timestamp getLastAlert(String alertId) {
        LocalDateTime timestamp = dslContext.select(Tables.BITHON_ALERT_STATE.LAST_ALERT_AT)
                                            .from(Tables.BITHON_ALERT_STATE.getName() + " FINAL")
                                            .where(Tables.BITHON_ALERT_STATE.ALERT_ID.eq(alertId))
                                            .fetchOne(Tables.BITHON_ALERT_STATE.LAST_ALERT_AT);
        return timestamp == null ? null : Timestamp.valueOf(timestamp);
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

        dslContext.insertInto(Tables.BITHON_ALERT_STATE)
                  .set(Tables.BITHON_ALERT_STATE.ALERT_ID, record.getAlertId())
                  .set(Tables.BITHON_ALERT_STATE.LAST_ALERT_AT, record.getCreatedAt().toLocalDateTime())
                  .set(Tables.BITHON_ALERT_STATE.LAST_RECORD_ID, record.getRecordId())
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
                new DataCleaner(clickHouseConfig, dslContext)
                    .deleteFromPartition(Tables.BITHON_ALERT_RECORD.getName(), before);
            }
        };
    }
}
