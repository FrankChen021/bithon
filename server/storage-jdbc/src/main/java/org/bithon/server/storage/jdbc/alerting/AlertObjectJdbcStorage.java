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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.alerting.AlertingStorageConfiguration;
import org.bithon.server.storage.alerting.IAlertObjectStorage;
import org.bithon.server.storage.alerting.ObjectAction;
import org.bithon.server.storage.alerting.pojo.AlertChangeLogObject;
import org.bithon.server.storage.alerting.pojo.AlertStateObject;
import org.bithon.server.storage.alerting.pojo.AlertStatus;
import org.bithon.server.storage.alerting.pojo.AlertStorageObject;
import org.bithon.server.storage.alerting.pojo.AlertStorageObjectPayload;
import org.bithon.server.storage.alerting.pojo.ListAlertDTO;
import org.bithon.server.storage.alerting.pojo.ListResult;
import org.bithon.server.storage.datasource.query.Limit;
import org.bithon.server.storage.datasource.query.Order;
import org.bithon.server.storage.datasource.query.OrderBy;
import org.bithon.server.storage.jdbc.JdbcStorageProviderConfiguration;
import org.bithon.server.storage.jdbc.common.dialect.ISqlDialect;
import org.bithon.server.storage.jdbc.common.dialect.SqlDialectManager;
import org.bithon.server.storage.jdbc.common.jooq.Tables;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Select;
import org.jooq.SelectConditionStep;
import org.jooq.SortField;
import org.springframework.dao.DuplicateKeyException;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/2 3:45 下午
 */
public class AlertObjectJdbcStorage implements IAlertObjectStorage {

    protected final DSLContext dslContext;
    protected final ISqlDialect sqlDialect;
    protected final ObjectMapper objectMapper;
    private final String quotedObjectTableSelectName;
    private final String quotedStateTableSelectName;
    protected final AlertingStorageConfiguration.AlertStorageConfig storageConfig;

    @JsonCreator
    public AlertObjectJdbcStorage(@JacksonInject(useInput = OptBoolean.FALSE) JdbcStorageProviderConfiguration storageProvider,
                                  @JacksonInject(useInput = OptBoolean.FALSE) SqlDialectManager sqlDialectManager,
                                  @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper objectMapper,
                                  @JacksonInject(useInput = OptBoolean.FALSE) AlertingStorageConfiguration.AlertStorageConfig storageConfig) {
        this(storageProvider.getDslContext(),
             sqlDialectManager.getSqlDialect(storageProvider.getDslContext()),
             sqlDialectManager.getSqlDialect(storageProvider.getDslContext()).quoteIdentifier(Tables.BITHON_ALERT_OBJECT.getName()),
             sqlDialectManager.getSqlDialect(storageProvider.getDslContext()).quoteIdentifier(Tables.BITHON_ALERT_STATE.getName()),
             objectMapper,
             storageConfig
        );
    }

    public AlertObjectJdbcStorage(DSLContext dslContext,
                                  ISqlDialect sqlDialect,
                                  String objectTableSelectName,
                                  String stateTableSelectName,
                                  ObjectMapper objectMapper,
                                  AlertingStorageConfiguration.AlertStorageConfig storageConfig) {
        this.dslContext = dslContext;
        this.sqlDialect = sqlDialect;
        this.quotedObjectTableSelectName = objectTableSelectName;
        this.quotedStateTableSelectName = stateTableSelectName;
        this.objectMapper = objectMapper;
        this.storageConfig = storageConfig;
    }

    @Override
    public void initialize() {
        if (!this.storageConfig.isCreateTable()) {
            return;
        }
        this.dslContext.createTableIfNotExists(Tables.BITHON_ALERT_OBJECT)
                       .columns(Tables.BITHON_ALERT_OBJECT.fields())
                       .indexes(Tables.BITHON_ALERT_OBJECT.getIndexes()).execute();

        this.dslContext.createTableIfNotExists(Tables.BITHON_ALERT_CHANGE_LOG)
                       .columns(Tables.BITHON_ALERT_CHANGE_LOG.fields())
                       .indexes(Tables.BITHON_ALERT_CHANGE_LOG.getIndexes())
                       .execute();

        this.dslContext.createTableIfNotExists(Tables.BITHON_ALERT_STATE)
                       .columns(Tables.BITHON_ALERT_STATE.fields())
                       .indexes(Tables.BITHON_ALERT_STATE.getIndexes())
                       .execute();
    }

    @Override
    public List<AlertStorageObject> getAlertListByTime(Timestamp start, Timestamp end) {
        return dslContext.selectFrom(this.quotedObjectTableSelectName)
                         .where(Tables.BITHON_ALERT_OBJECT.UPDATED_AT.between(start.toLocalDateTime(), end.toLocalDateTime()))
                         .fetch(this::toStorageObject);
    }

    @Override
    public boolean existAlertById(String alertId) {
        // use fetchCount instead of fetchExists
        // because the former one uses 'exists' subclause in the generated SQL,
        // and for ClickHouse, this clause is not recognizable
        return this.fetchCount(this.quotedObjectTableSelectName,
                               Tables.BITHON_ALERT_OBJECT.ALERT_ID.eq(alertId),
                               Tables.BITHON_ALERT_OBJECT.DELETED.eq(0)) > 0;
    }

    @Override
    public boolean existAlertByName(String name) {
        return this.fetchCount(this.quotedObjectTableSelectName,
                               Tables.BITHON_ALERT_OBJECT.ALERT_NAME.eq(name),
                               Tables.BITHON_ALERT_OBJECT.DELETED.eq(0)) > 0;
    }

    @Override
    public AlertStorageObject getAlertById(String alertId) {
        return dslContext.selectFrom(this.quotedObjectTableSelectName)
                         .where(Tables.BITHON_ALERT_OBJECT.ALERT_ID.eq(alertId))
                         .and(Tables.BITHON_ALERT_OBJECT.DELETED.eq(0))
                         .fetchOne(this::toStorageObject);
    }

    protected AlertStorageObject toStorageObject(Record record) {
        AlertStorageObject obj = new AlertStorageObject();
        obj.setId(record.get(Tables.BITHON_ALERT_OBJECT.ALERT_ID));
        obj.setName(record.get(Tables.BITHON_ALERT_OBJECT.ALERT_NAME));
        obj.setDeleted(record.get(Tables.BITHON_ALERT_OBJECT.DELETED) != 0);
        obj.setDisabled(record.get(Tables.BITHON_ALERT_OBJECT.DISABLED) != 0);

        // It's strange that at runtime, the CREATE_AT is type of Timestamp
        Object ts = record.get(Tables.BITHON_ALERT_OBJECT.CREATED_AT);
        obj.setCreatedAt(ts instanceof Timestamp ? (Timestamp) ts : Timestamp.valueOf((LocalDateTime) ts));

        ts = record.get(Tables.BITHON_ALERT_OBJECT.UPDATED_AT);
        obj.setUpdatedAt(ts instanceof Timestamp ? (Timestamp) ts : Timestamp.valueOf((LocalDateTime) ts));

        obj.setAppName(record.get(Tables.BITHON_ALERT_OBJECT.APP_NAME));
        obj.setLastOperator(record.get(Tables.BITHON_ALERT_OBJECT.LAST_OPERATOR));
        obj.setNamespace(record.get(Tables.BITHON_ALERT_OBJECT.NAMESPACE));
        try {
            obj.setPayload(objectMapper.readValue(record.get(Tables.BITHON_ALERT_OBJECT.PAYLOAD), AlertStorageObjectPayload.class));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return obj;
    }

    @Override
    public void createAlert(AlertStorageObject alert, String operator, Timestamp createTimestamp, Timestamp updateTimestamp) {
        try {
            dslContext.insertInto(Tables.BITHON_ALERT_OBJECT)
                      .set(Tables.BITHON_ALERT_OBJECT.ALERT_NAME, alert.getName())
                      .set(Tables.BITHON_ALERT_OBJECT.APP_NAME, alert.getAppName())
                      .set(Tables.BITHON_ALERT_OBJECT.NAMESPACE, alert.getNamespace())
                      .set(Tables.BITHON_ALERT_OBJECT.DISABLED, alert.isDisabled() ? 1 : 0)
                      .set(Tables.BITHON_ALERT_OBJECT.PAYLOAD, objectMapper.writeValueAsString(alert.getPayload()))
                      .set(Tables.BITHON_ALERT_OBJECT.ALERT_ID, alert.getId())
                      .set(Tables.BITHON_ALERT_OBJECT.LAST_OPERATOR, operator)
                      .set(Tables.BITHON_ALERT_OBJECT.DELETED, 0)
                      .set(Tables.BITHON_ALERT_OBJECT.CREATED_AT, createTimestamp.toLocalDateTime())
                      .set(Tables.BITHON_ALERT_OBJECT.UPDATED_AT, updateTimestamp.toLocalDateTime())
                      .execute();
        } catch (DuplicateKeyException e) {
            throw new RuntimeException(StringUtils.format("Alert rule with id [%s] already exists.", alert.getId()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean updateAlert(AlertStorageObject oldObject, AlertStorageObject newObject, String operator) {
        try {
            return dslContext.update(Tables.BITHON_ALERT_OBJECT)
                             .set(Tables.BITHON_ALERT_OBJECT.ALERT_NAME, newObject.getName())
                             .set(Tables.BITHON_ALERT_OBJECT.APP_NAME, newObject.getAppName())
                             .set(Tables.BITHON_ALERT_OBJECT.NAMESPACE, newObject.getNamespace())
                             .set(Tables.BITHON_ALERT_OBJECT.DISABLED, newObject.isDisabled() ? 1 : 0)
                             .set(Tables.BITHON_ALERT_OBJECT.PAYLOAD, objectMapper.writeValueAsString(newObject.getPayload()))
                             .set(Tables.BITHON_ALERT_OBJECT.LAST_OPERATOR, operator)
                             .set(Tables.BITHON_ALERT_OBJECT.UPDATED_AT, new Timestamp(System.currentTimeMillis()).toLocalDateTime())
                             .where(Tables.BITHON_ALERT_OBJECT.ALERT_ID.eq(newObject.getId()))
                             .execute() > 0;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean disableAlert(String alertId, String operator) {
        return dslContext.update(Tables.BITHON_ALERT_OBJECT)
                         .set(Tables.BITHON_ALERT_OBJECT.DISABLED, 1)
                         .set(Tables.BITHON_ALERT_OBJECT.LAST_OPERATOR, operator)
                         .where(Tables.BITHON_ALERT_OBJECT.ALERT_ID.eq(alertId))
                         .and(Tables.BITHON_ALERT_OBJECT.DISABLED.eq(0))
                         .execute() > 0;
    }

    @Override
    public boolean enableAlert(String alertId, String operator) {
        return dslContext.update(Tables.BITHON_ALERT_OBJECT)
                         .set(Tables.BITHON_ALERT_OBJECT.DISABLED, 0)
                         .set(Tables.BITHON_ALERT_OBJECT.LAST_OPERATOR, operator)
                         .where(Tables.BITHON_ALERT_OBJECT.ALERT_ID.eq(alertId))
                         .and(Tables.BITHON_ALERT_OBJECT.DISABLED.eq(1))
                         .execute() > 0;
    }

    @Override
    public boolean deleteAlert(String alertId, String operator) {
        return dslContext.update(Tables.BITHON_ALERT_OBJECT)
                         .set(Tables.BITHON_ALERT_OBJECT.DELETED, 1)
                         .where(Tables.BITHON_ALERT_OBJECT.ALERT_ID.eq(alertId))
                         .execute() > 0;
    }

    @Override
    public void addChangelog(String alertId, ObjectAction action, String operator, String before, String after) {
        dslContext.insertInto(Tables.BITHON_ALERT_CHANGE_LOG)
                  .set(Tables.BITHON_ALERT_CHANGE_LOG.ALERT_ID, alertId)
                  .set(Tables.BITHON_ALERT_CHANGE_LOG.ACTION, action.toString())
                  .set(Tables.BITHON_ALERT_CHANGE_LOG.EDITOR, operator)
                  .set(Tables.BITHON_ALERT_CHANGE_LOG.PAYLOAD_BEFORE, before)
                  .set(Tables.BITHON_ALERT_CHANGE_LOG.PAYLOAD_AFTER, after)
                  .set(Tables.BITHON_ALERT_CHANGE_LOG.CREATED_AT, new Timestamp(System.currentTimeMillis()).toLocalDateTime())
                  .execute();
    }

    @Override
    public int getAlertListSize(String appName, String alertName) {
        Condition condition = Tables.BITHON_ALERT_OBJECT.DELETED.eq(0);

        if (StringUtils.hasText(appName)) {
            condition = condition.and(Tables.BITHON_ALERT_OBJECT.APP_NAME.eq(appName));
        }
        if (StringUtils.hasText(alertName)) {
            condition = condition.and(Tables.BITHON_ALERT_OBJECT.ALERT_NAME.like("%" + alertName + "%"));
        }

        return this.fetchCount(this.quotedObjectTableSelectName, condition);
    }

    @Override
    public List<ListAlertDTO> getAlertList(String appName,
                                           String alertName,
                                           OrderBy orderBy,
                                           Limit limit) {
        SelectConditionStep<?> selectSql = dslContext.select(Tables.BITHON_ALERT_OBJECT.ALERT_ID,
                                                             Tables.BITHON_ALERT_OBJECT.ALERT_NAME,
                                                             Tables.BITHON_ALERT_OBJECT.DISABLED,
                                                             Tables.BITHON_ALERT_OBJECT.APP_NAME,
                                                             Tables.BITHON_ALERT_OBJECT.CREATED_AT,
                                                             Tables.BITHON_ALERT_OBJECT.UPDATED_AT,
                                                             Tables.BITHON_ALERT_STATE.LAST_ALERT_AT,
                                                             Tables.BITHON_ALERT_STATE.LAST_RECORD_ID,
                                                             Tables.BITHON_ALERT_STATE.ALERT_STATUS,
                                                             Tables.BITHON_ALERT_OBJECT.LAST_OPERATOR)
                                                     .from(this.quotedObjectTableSelectName)
                                                     .leftJoin(StringUtils.format("(SELECT * FROM %s) AS %s", this.quotedStateTableSelectName, sqlDialect.quoteIdentifier(Tables.BITHON_ALERT_STATE.getName())))
                                                     .on(Tables.BITHON_ALERT_OBJECT.ALERT_ID.eq(Tables.BITHON_ALERT_STATE.ALERT_ID))
                                                     .where(Tables.BITHON_ALERT_OBJECT.DELETED.eq(0));

        if (StringUtils.hasText(appName)) {
            selectSql = selectSql.and(Tables.BITHON_ALERT_OBJECT.APP_NAME.eq(appName));
        }
        if (StringUtils.hasText(alertName)) {
            selectSql = selectSql.and(Tables.BITHON_ALERT_OBJECT.ALERT_NAME.like("%" + alertName + "%"));
        }

        SortField<?> orderByField = Tables.BITHON_ALERT_OBJECT.UPDATED_AT.desc();
        Field<?>[] orderByFields = new Field[]{
            Tables.BITHON_ALERT_OBJECT.UPDATED_AT,
            Tables.BITHON_ALERT_STATE.LAST_ALERT_AT
        };
        for (Field<?> field : orderByFields) {
            if (field.getName().equals(orderBy.getName())) {
                if (Order.desc.equals(orderBy.getOrder())) {
                    orderByField = field.desc();
                } else {
                    orderByField = field.asc();
                }
            }
        }

        return dslContext.fetch(getAlertListSql(selectSql.orderBy(orderByField)
                                                         .offset(limit.getOffset())
                                                         .limit(limit.getLimit())))
                         .map((record) -> {
                             ListAlertDTO obj = new ListAlertDTO();
                             obj.setAlertId(record.get(Tables.BITHON_ALERT_OBJECT.ALERT_ID));
                             obj.setAlertName(record.get(Tables.BITHON_ALERT_OBJECT.ALERT_NAME));
                             obj.setDisabled(record.get(Tables.BITHON_ALERT_OBJECT.DISABLED) != 0);

                             obj.setAppName(record.get(Tables.BITHON_ALERT_OBJECT.APP_NAME));

                             // It's very strange that under H2,
                             // the returned object is a type of Timestamp instead of LocalDateTime
                             Object timestamp = record.get(Tables.BITHON_ALERT_OBJECT.CREATED_AT);
                             obj.setCreatedAt(timestamp instanceof Timestamp ? (Timestamp) timestamp : Timestamp.valueOf((LocalDateTime) timestamp));

                             timestamp = record.get(Tables.BITHON_ALERT_OBJECT.UPDATED_AT);
                             obj.setUpdatedAt(timestamp instanceof Timestamp ? (Timestamp) timestamp : Timestamp.valueOf((LocalDateTime) timestamp));
                             obj.setLastOperator(record.get(Tables.BITHON_ALERT_OBJECT.LAST_OPERATOR));

                             // The lastAlertAt can be NULL
                             timestamp = record.get(Tables.BITHON_ALERT_STATE.LAST_ALERT_AT);
                             if (timestamp != null) {
                                 obj.setLastAlertAt(timestamp instanceof Timestamp ? (Timestamp) timestamp : Timestamp.valueOf((LocalDateTime) timestamp));
                             }
                             obj.setLastRecordId(record.get(Tables.BITHON_ALERT_STATE.LAST_RECORD_ID));

                             Integer status = record.get(Tables.BITHON_ALERT_STATE.ALERT_STATUS);
                             obj.setAlertStatus(status == null ? AlertStatus.PENDING : AlertStatus.fromCode(status));
                             return obj;
                         });
    }

    @Override
    public Map<String, AlertStateObject> getAlertStates() {
        return dslContext.selectFrom(this.quotedStateTableSelectName)
                         .fetchMap(Tables.BITHON_ALERT_STATE.ALERT_ID, (record) -> {
                             AlertStateObject obj = new AlertStateObject();
                             obj.setStatus(AlertStatus.fromCode(record.get(Tables.BITHON_ALERT_STATE.ALERT_STATUS)));

                             Object timestamp = record.get(Tables.BITHON_ALERT_STATE.LAST_ALERT_AT);
                             // It's strange that the returned object is typeof Timestamp under H2
                             if (timestamp instanceof Timestamp) {
                                 obj.setLastAlertAt(((Timestamp) timestamp).toLocalDateTime());
                             } else {
                                 obj.setLastAlertAt((LocalDateTime) timestamp);
                             }
                             obj.setLastRecordId(record.get(Tables.BITHON_ALERT_STATE.LAST_RECORD_ID));
                             return obj;
                         });
    }

    protected String getAlertListSql(Select<?> selectQuery) {
        return dslContext.renderInlined(selectQuery);
    }

    @Override
    public ListResult<AlertChangeLogObject> getChangeLogs(String alertId, Integer pageNumber, Integer pageSize) {
        return new ListResult<>(dslContext.fetchCount(Tables.BITHON_ALERT_CHANGE_LOG, Tables.BITHON_ALERT_CHANGE_LOG.ALERT_ID.eq(alertId)),
                                dslContext.selectFrom(Tables.BITHON_ALERT_CHANGE_LOG)
                                          .where(Tables.BITHON_ALERT_CHANGE_LOG.ALERT_ID.eq(alertId))
                                          .orderBy(Tables.BITHON_ALERT_CHANGE_LOG.CREATED_AT.desc())
                                          .offset(pageNumber * pageSize)
                                          .limit(pageSize)
                                          .fetchInto(AlertChangeLogObject.class));
    }

    @Override
    public <T> T executeTransaction(Callable<T> callable) {
        return this.dslContext.transactionResult((configuration) -> callable.call());
    }

    private int fetchCount(String table, Condition... conditions) {
        //noinspection DataFlowIssue
        return dslContext.selectCount()
                         .from(table)
                         .where(conditions)
                         .fetchOne(0, int.class);
    }
}
