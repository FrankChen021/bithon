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
import org.bithon.server.storage.alerting.IAlertNotificationProviderStorage;
import org.bithon.server.storage.alerting.pojo.NotificationProviderObject;
import org.bithon.server.storage.jdbc.JdbcStorageProviderConfiguration;
import org.bithon.server.storage.jdbc.common.jooq.Tables;
import org.jooq.DSLContext;

import java.sql.Timestamp;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/12/22 17:38
 */
public class NotificationProviderJdbcStorage implements IAlertNotificationProviderStorage {

    protected DSLContext dslContext;

    @JsonCreator
    public NotificationProviderJdbcStorage(@JacksonInject(useInput = OptBoolean.FALSE) JdbcStorageProviderConfiguration storageConfiguration) {
        this(storageConfiguration.getDslContext());
    }

    protected NotificationProviderJdbcStorage(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    @Override
    public void creatProvider(String id,
                              String name,
                              String type,
                              String props) {
        dslContext.insertInto(Tables.BITHON_ALERT_NOTIFICATION_PROVIDER)
                  .set(Tables.BITHON_ALERT_NOTIFICATION_PROVIDER.PROVIDER_ID, id)
                  .set(Tables.BITHON_ALERT_NOTIFICATION_PROVIDER.NAME, name)
                  .set(Tables.BITHON_ALERT_NOTIFICATION_PROVIDER.TYPE, type)
                  .set(Tables.BITHON_ALERT_NOTIFICATION_PROVIDER.PAYLOAD, props)
                  .set(Tables.BITHON_ALERT_NOTIFICATION_PROVIDER.CREATED_AT, new Timestamp(System.currentTimeMillis()).toLocalDateTime())
                  .execute();
    }

    @Override
    public void deleteProvider(String id) {
        dslContext.deleteFrom(Tables.BITHON_ALERT_NOTIFICATION_PROVIDER)
                  .where(Tables.BITHON_ALERT_NOTIFICATION_PROVIDER.PROVIDER_ID.eq(id))
                  .execute();
    }

    @Override
    public boolean exists(String id) {
        return dslContext.fetchExists(dslContext.selectFrom(Tables.BITHON_ALERT_NOTIFICATION_PROVIDER)
                                                .where(Tables.BITHON_ALERT_NOTIFICATION_PROVIDER.PROVIDER_ID.eq(id)));
    }

    @Override
    public List<NotificationProviderObject> loadProviders(long since) {
        return dslContext.selectFrom(Tables.BITHON_ALERT_NOTIFICATION_PROVIDER)
                         .fetch()
                         .map((record) -> {
                             NotificationProviderObject obj = new NotificationProviderObject();
                             obj.setProviderId(record.getProviderId());
                             obj.setName(record.getName());
                             obj.setType(record.getType());
                             obj.setPayload(record.getPayload());
                             return obj;
                         });
    }

    @Override
    public void initialize() {
        this.dslContext.createTableIfNotExists(Tables.BITHON_ALERT_NOTIFICATION_PROVIDER)
                       .columns(Tables.BITHON_ALERT_NOTIFICATION_PROVIDER.fields())
                       .indexes(Tables.BITHON_ALERT_NOTIFICATION_PROVIDER.getIndexes())
                       .execute();
    }
}
