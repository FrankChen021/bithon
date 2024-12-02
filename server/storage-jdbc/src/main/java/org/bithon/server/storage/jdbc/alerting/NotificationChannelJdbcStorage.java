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
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.commons.utils.SqlLikeExpression;
import org.bithon.server.storage.alerting.AlertingStorageConfiguration;
import org.bithon.server.storage.alerting.IAlertNotificationChannelStorage;
import org.bithon.server.storage.alerting.pojo.NotificationChannelObject;
import org.bithon.server.storage.datasource.query.Order;
import org.bithon.server.storage.jdbc.JdbcStorageProviderConfiguration;
import org.bithon.server.storage.jdbc.common.dialect.ISqlDialect;
import org.bithon.server.storage.jdbc.common.dialect.SqlDialectManager;
import org.bithon.server.storage.jdbc.common.jooq.Tables;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.SelectConditionStep;
import org.jooq.SortField;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/12/22 17:38
 */
public class NotificationChannelJdbcStorage implements IAlertNotificationChannelStorage {

    protected final AlertingStorageConfiguration.AlertStorageConfig storageConfig;
    private final ISqlDialect sqlDialect;
    protected DSLContext dslContext;

    @JsonCreator
    public NotificationChannelJdbcStorage(@JacksonInject(useInput = OptBoolean.FALSE) JdbcStorageProviderConfiguration storageConfiguration,
                                          @JacksonInject(useInput = OptBoolean.FALSE) SqlDialectManager sqlDialectManager,
                                          @JacksonInject(useInput = OptBoolean.FALSE) AlertingStorageConfiguration.AlertStorageConfig storageConfig) {
        this(storageConfiguration.getDslContext(),
             sqlDialectManager,
             storageConfig);
    }

    protected NotificationChannelJdbcStorage(DSLContext dslContext,
                                             SqlDialectManager sqlDialectManager,
                                             AlertingStorageConfiguration.AlertStorageConfig storageConfig) {
        this.dslContext = dslContext;
        this.storageConfig = storageConfig;
        this.sqlDialect = sqlDialectManager.getSqlDialect(dslContext);
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
        return dslContext.selectFrom(getChanelTableSelectFrom())
                         .where(Tables.BITHON_ALERT_NOTIFICATION_CHANNEL.NAME.eq(name))
                         .fetchOne(this::toChannelObject);
    }

    @Override
    public List<NotificationChannelObject> getChannels(GetChannelRequest request) {
        SelectConditionStep<org.jooq.Record> select = dslContext.selectFrom(getChanelTableSelectFrom())
                                                                .where("1 = 1");

        if (StringUtils.hasText(request.getName())) {
            //noinspection unchecked,rawtypes
            select = ((SelectConditionStep) select).and(Tables.BITHON_ALERT_NOTIFICATION_CHANNEL.NAME.likeIgnoreCase(SqlLikeExpression.toLikePattern(request.getName())));
        }

        if (request.getSince() > 0) {
            //noinspection unchecked,rawtypes
            select = ((SelectConditionStep) select).and(Tables.BITHON_ALERT_NOTIFICATION_CHANNEL.CREATED_AT.ge(new Timestamp(request.getSince()).toLocalDateTime()));
        }

        if (request.getOrderBy() != null && request.getLimit() != null) {
            Field<?> orderBy;
            if ("createdAt".equals(request.getOrderBy().getName())) {
                orderBy = Tables.BITHON_ALERT_NOTIFICATION_CHANNEL.CREATED_AT;
            } else if ("name".equals(request.getOrderBy().getName())) {
                orderBy = Tables.BITHON_ALERT_NOTIFICATION_CHANNEL.NAME;
            } else {
                orderBy = Tables.BITHON_ALERT_NOTIFICATION_CHANNEL.UPDATED_AT;
            }

            SortField<?> sortField;
            if (request.getOrderBy().getOrder().equals(Order.asc)) {
                sortField = orderBy.asc();
            } else {
                sortField = orderBy.desc();
            }
            return select.orderBy(sortField)
                         .limit(request.getLimit().getOffset(), request.getLimit().getLimit())
                         .fetch()
                         .map(this::toChannelObject);
        } else {
            return select.fetch()
                         .map(this::toChannelObject);
        }
    }

    public int getChannelsSize(GetChannelRequest request) {
        Condition condition = null;

        if (!StringUtils.isEmpty(request.getName())) {
            condition = Tables.BITHON_ALERT_NOTIFICATION_CHANNEL.NAME.likeIgnoreCase(request.getName());
        }

        if (request.getSince() > 0) {
            Condition sinceCondition = Tables.BITHON_ALERT_NOTIFICATION_CHANNEL.CREATED_AT.ge(new Timestamp(request.getSince()).toLocalDateTime());
            if (condition == null) {
                condition = sinceCondition;
            } else {
                condition = condition.and(sinceCondition);
            }
        }

        return dslContext.selectCount()
                         .from(getChanelTableSelectFrom())
                         .where(condition)
                         .fetchOne(0, int.class);
    }

    protected String getChanelTableSelectFrom() {
        return this.sqlDialect.quoteIdentifier(Tables.BITHON_ALERT_NOTIFICATION_CHANNEL.getName());
    }

    protected NotificationChannelObject toChannelObject(org.jooq.Record record) {
        NotificationChannelObject channel = new NotificationChannelObject();
        channel.setName(record.get(Tables.BITHON_ALERT_NOTIFICATION_CHANNEL.NAME));
        channel.setType(record.get(Tables.BITHON_ALERT_NOTIFICATION_CHANNEL.TYPE));
        channel.setPayload(record.get(Tables.BITHON_ALERT_NOTIFICATION_CHANNEL.PAYLOAD));

        Object createdAt = record.get(Tables.BITHON_ALERT_NOTIFICATION_CHANNEL.CREATED_AT);
        if (createdAt instanceof Timestamp) {
            channel.setCreatedAt((Timestamp) createdAt);
        } else {
            channel.setCreatedAt(Timestamp.valueOf((LocalDateTime) createdAt));
        }

        Object updatedAt = record.get(Tables.BITHON_ALERT_NOTIFICATION_CHANNEL.UPDATED_AT);
        if (updatedAt instanceof Timestamp) {
            channel.setUpdatedAt((Timestamp) updatedAt);
        } else {
            channel.setUpdatedAt(Timestamp.valueOf((LocalDateTime) updatedAt));
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
