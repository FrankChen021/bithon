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
import org.bithon.component.commons.time.DateTime;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.alerting.pojo.AlertRecordObject;
import org.bithon.server.storage.common.IStorageCleaner;
import org.bithon.server.storage.jdbc.alerting.AlertRecordJdbcStorage;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseConfig;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseJooqContextHolder;
import org.bithon.server.storage.jdbc.clickhouse.TableCreator;
import org.bithon.server.storage.jdbc.jooq.Tables;

import java.sql.Timestamp;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/5
 */
@JsonTypeName("clickhouse")
public class AlertRecordStorage extends AlertRecordJdbcStorage {

    private final ClickHouseConfig config;

    @JsonCreator
    public AlertRecordStorage(@JacksonInject(useInput = OptBoolean.FALSE) ClickHouseJooqContextHolder dslContextHolder,
                              @JacksonInject(useInput = OptBoolean.FALSE) ClickHouseConfig config) {
        super(dslContextHolder.getDslContext());

        this.config = config;
    }

    @Override
    public void initialize() {
        new TableCreator(this.config, dslContext).createIfNotExist(Tables.BITHON_ALERT_RECORD);
    }

    @Override
    public IStorageCleaner createCleaner() {
        return beforeTimestamp -> dslContext.execute(StringUtils.format("ALTER TABLE %s.%s %s DELETE WHERE timestamp < '%s'",
                                                                        config.getDatabase(),
                                                                        config.getLocalTableName(Tables.BITHON_ALERT_RECORD.getName()),
                                                                        config.getClusterExpression(),
                                                                        DateTime.toYYYYMMDDhhmmss(beforeTimestamp)));

    }

    @Override
    public Timestamp getLastAlert(String alertId) {
        return dslContext.select(Tables.BITHON_ALERT_STATE.LAST_ALERT_AT)
                         .from(Tables.BITHON_ALERT_STATE.getName() + " FINAL")
                         .where(Tables.BITHON_ALERT_STATE.ALERT_ID.eq(alertId))
                         .fetchOne(Tables.BITHON_ALERT_STATE.LAST_ALERT_AT);
    }

    @Override
    public void addAlertRecord(AlertRecordObject record) {

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());

        this.dslContext.transaction((configuration) -> {
            dslContext.insertInto(Tables.BITHON_ALERT_RECORD)
                      .set(Tables.BITHON_ALERT_RECORD.APP_NAME, record.getAppName())
                      .set(Tables.BITHON_ALERT_RECORD.ALERT_NAME, record.getAlertName())
                      .set(Tables.BITHON_ALERT_RECORD.NAMESPACE, record.getNamespace())
                      .set(Tables.BITHON_ALERT_RECORD.ALERT_ID, record.getAlertId())
                      .set(Tables.BITHON_ALERT_RECORD.PAYLOAD, record.getPayload())
                      .set(Tables.BITHON_ALERT_RECORD.DATA_SOURCE, record.getDataSource())
                      .set(Tables.BITHON_ALERT_RECORD.NOTIFICATION_STATUS, record.getNotificationStatus())
                      .set(Tables.BITHON_ALERT_RECORD.RECORD_ID, record.getRecordId())
                      .set(Tables.BITHON_ALERT_RECORD.TIMESTAMP, timestamp)
                      .execute();

            dslContext.insertInto(Tables.BITHON_ALERT_STATE)
                      .set(Tables.BITHON_ALERT_STATE.ALERT_ID, record.getAlertId())
                      .set(Tables.BITHON_ALERT_STATE.LAST_ALERT_AT, timestamp)
                      .set(Tables.BITHON_ALERT_STATE.LAST_RECORD_ID, record.getRecordId())
                      .execute();
        });
    }
}
