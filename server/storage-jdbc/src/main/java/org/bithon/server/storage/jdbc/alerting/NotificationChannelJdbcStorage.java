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
import org.bithon.server.storage.alerting.AlertingStorageConfiguration;
import org.bithon.server.storage.alerting.IAlertNotificationChannelStorage;
import org.bithon.server.storage.alerting.pojo.NotificationChannelObject;
import org.bithon.server.storage.jdbc.JdbcStorageProviderConfiguration;
import org.bithon.server.storage.jdbc.common.jooq.Tables;
import org.bithon.server.storage.jdbc.common.jooq.tables.records.BithonAlertNotificationChannelRecord;
import org.jooq.DSLContext;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/12/22 17:38
 */
public class NotificationChannelJdbcStorage implements IAlertNotificationChannelStorage {

    protected final AlertingStorageConfiguration.AlertStorageConfig storageConfig;
    protected DSLContext dslContext;

    @JsonCreator
    public NotificationChannelJdbcStorage(@JacksonInject(useInput = OptBoolean.FALSE) JdbcStorageProviderConfiguration storageConfiguration,
                                          @JacksonInject(useInput = OptBoolean.FALSE) AlertingStorageConfiguration.AlertStorageConfig storageConfig) {
        this(storageConfiguration.getDslContext(),
             storageConfig);
    }

    protected NotificationChannelJdbcStorage(DSLContext dslContext, AlertingStorageConfiguration.AlertStorageConfig storageConfig) {
        this.dslContext = dslContext;
        this.storageConfig = storageConfig;
    }

    @Override
    public void createChannel(String type,
                              String name,
                              String props) {
        LocalDateTime now = new Timestamp(System.currentTimeMillis()).toLocalDateTime();
        dslContext.insertInto(Tables.BITHON_ALERT_NOTIFICATION_CHANNEL)
                  .set(Tables.BITHON_ALERT_NOTIFICATION_CHANNEL.TYPE, type)
                  .set(Tables.BITHON_ALERT_NOTIFICATION_CHANNEL.NAME, name)
                  .set(Tables.BITHON_ALERT_NOTIFICATION_CHANNEL.PAYLOAD, props)
                  .set(Tables.BITHON_ALERT_NOTIFICATION_CHANNEL.CREATED_AT, now)
                  .set(Tables.BITHON_ALERT_NOTIFICATION_CHANNEL.UPDATED_AT, now)
                  .execute();
    }

    @Override
    public boolean updateChannel(NotificationChannelObject old, String props) {
        return dslContext.update(Tables.BITHON_ALERT_NOTIFICATION_CHANNEL)
                         .set(Tables.BITHON_ALERT_NOTIFICATION_CHANNEL.PAYLOAD, props)
                         .set(Tables.BITHON_ALERT_NOTIFICATION_CHANNEL.UPDATED_AT, new Timestamp(System.currentTimeMillis()).toLocalDateTime())
                         .where(Tables.BITHON_ALERT_NOTIFICATION_CHANNEL.NAME.eq(old.getName()))
                         .and(Tables.BITHON_ALERT_NOTIFICATION_CHANNEL.TYPE.eq(old.getType()))
                         .execute() > 0;
    }

    @Override
    public void deleteChannel(String name) {
        dslContext.deleteFrom(Tables.BITHON_ALERT_NOTIFICATION_CHANNEL)
                  .where(Tables.BITHON_ALERT_NOTIFICATION_CHANNEL.NAME.eq(name))
                  .execute();
    }

    @Override
    public boolean exists(String name) {
        return dslContext.fetchCount(Tables.BITHON_ALERT_NOTIFICATION_CHANNEL, Tables.BITHON_ALERT_NOTIFICATION_CHANNEL.NAME.eq(name)) > 0;
    }

    @Override
    public NotificationChannelObject getChannel(String name) {
        return dslContext.selectFrom(Tables.BITHON_ALERT_NOTIFICATION_CHANNEL)
                         .where(Tables.BITHON_ALERT_NOTIFICATION_CHANNEL.NAME.eq(name))
                         .fetchOne(this::toChannelObject);
    }

    @Override
    public List<NotificationChannelObject> getChannels(long since) {
        return dslContext.selectFrom(Tables.BITHON_ALERT_NOTIFICATION_CHANNEL)
                         .fetch()
                         .map(this::toChannelObject);
    }

    protected NotificationChannelObject toChannelObject(BithonAlertNotificationChannelRecord record) {
        NotificationChannelObject channel = new NotificationChannelObject();
        channel.setName(record.getName());
        channel.setType(record.getType());
        channel.setPayload(record.getPayload());
        channel.setCreatedAt(Timestamp.valueOf(record.getCreatedAt()));
        if (record.getUpdatedAt() == null) {
            channel.setUpdatedAt(channel.getCreatedAt());
        } else {
            channel.setUpdatedAt(Timestamp.valueOf(record.getUpdatedAt()));
        }
        return channel;
    }

    @Override
    public void initialize() {
        if (!this.storageConfig.isCreateTable()) {
            return;
        }
        this.dslContext.createTableIfNotExists(Tables.BITHON_ALERT_NOTIFICATION_CHANNEL)
                       .columns(Tables.BITHON_ALERT_NOTIFICATION_CHANNEL.fields())
                       .indexes(Tables.BITHON_ALERT_NOTIFICATION_CHANNEL.getIndexes())
                       .execute();
    }
}
