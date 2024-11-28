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
import org.bithon.server.storage.alerting.AlertingStorageConfiguration;
import org.bithon.server.storage.alerting.pojo.NotificationChannelObject;
import org.bithon.server.storage.jdbc.alerting.NotificationChannelJdbcStorage;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseConfig;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseStorageProviderConfiguration;
import org.bithon.server.storage.jdbc.clickhouse.common.DataCleaner;
import org.bithon.server.storage.jdbc.clickhouse.common.TableCreator;
import org.bithon.server.storage.jdbc.common.dialect.SqlDialectManager;
import org.bithon.server.storage.jdbc.common.jooq.Tables;

import java.sql.Timestamp;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/12/22 17:48
 */
@JsonTypeName("clickhouse")
public class NotificationChannelStorage extends NotificationChannelJdbcStorage {

    private final ClickHouseConfig clickHouseConfig;

    @JsonCreator
    public NotificationChannelStorage(@JacksonInject(useInput = OptBoolean.FALSE) ClickHouseStorageProviderConfiguration storageProvider,
                                      @JacksonInject(useInput = OptBoolean.FALSE) SqlDialectManager sqlDialectManager,
                                      @JacksonInject(useInput = OptBoolean.FALSE) AlertingStorageConfiguration.AlertStorageConfig storageConfig) {
        super(storageProvider.getDslContext(), sqlDialectManager, storageConfig);
        this.clickHouseConfig = storageProvider.getClickHouseConfig();
    }

    @Override
    public void deleteChannel(String name) {
        new DataCleaner(clickHouseConfig, dslContext).deleteByCondition(Tables.BITHON_ALERT_NOTIFICATION_CHANNEL,
                                                                        Tables.BITHON_ALERT_NOTIFICATION_CHANNEL.NAME.eq(name));
    }

    @Override
    public boolean updateChannel(NotificationChannelObject old, String props) {
        return dslContext.insertInto(Tables.BITHON_ALERT_NOTIFICATION_CHANNEL)
                         .set(Tables.BITHON_ALERT_NOTIFICATION_CHANNEL.TYPE, old.getType())
                         .set(Tables.BITHON_ALERT_NOTIFICATION_CHANNEL.NAME, old.getName())
                         .set(Tables.BITHON_ALERT_NOTIFICATION_CHANNEL.PAYLOAD, props)
                         .set(Tables.BITHON_ALERT_NOTIFICATION_CHANNEL.CREATED_AT, new Timestamp(System.currentTimeMillis()).toLocalDateTime())
                         .set(Tables.BITHON_ALERT_NOTIFICATION_CHANNEL.UPDATED_AT, new Timestamp(System.currentTimeMillis()).toLocalDateTime())
                         .execute() > 0;
    }

    public String getChanelTableSelectFrom() {
        return Tables.BITHON_ALERT_NOTIFICATION_CHANNEL.getName() + " FINAL ";
    }

    @Override
    public void initialize() {
        if (!this.storageConfig.isCreateTable()) {
            return;
        }

        new TableCreator(this.clickHouseConfig, this.dslContext)
            .partitionByExpression(null)
            .useReplacingMergeTree(Tables.BITHON_ALERT_NOTIFICATION_CHANNEL.CREATED_AT.getName())
            .createIfNotExist(Tables.BITHON_ALERT_NOTIFICATION_CHANNEL);
    }
}
