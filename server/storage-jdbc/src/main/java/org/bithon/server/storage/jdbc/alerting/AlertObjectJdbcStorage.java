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
import org.bithon.server.storage.alerting.IAlertObjectStorage;
import org.bithon.server.storage.alerting.ObjectAction;
import org.bithon.server.storage.alerting.pojo.AlertChangeLogObject;
import org.bithon.server.storage.alerting.pojo.AlertStorageObject;
import org.bithon.server.storage.alerting.pojo.ListAlertDO;
import org.bithon.server.storage.alerting.pojo.ListResult;
import org.bithon.server.storage.datasource.query.Limit;
import org.bithon.server.storage.datasource.query.Order;
import org.bithon.server.storage.datasource.query.OrderBy;
import org.bithon.server.storage.jdbc.JdbcStorageProviderConfiguration;
import org.bithon.server.storage.jdbc.common.dialect.ISqlDialect;
import org.bithon.server.storage.jdbc.common.dialect.SqlDialectManager;
import org.bithon.server.storage.jdbc.common.jooq.Tables;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.SelectConditionStep;
import org.jooq.SortField;
import org.springframework.dao.DuplicateKeyException;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/2 3:45 下午
 */
public class AlertObjectJdbcStorage implements IAlertObjectStorage {

    protected final DSLContext dslContext;
    protected final ISqlDialect sqlDialect;
    private final String quotedObjectTableSelectName;
    private final String quotedStateTableSelectName;

    @JsonCreator
    public AlertObjectJdbcStorage(@JacksonInject(useInput = OptBoolean.FALSE) JdbcStorageProviderConfiguration provider,
                                  @JacksonInject(useInput = OptBoolean.FALSE) SqlDialectManager sqlDialectManager) {
        this(provider.getDslContext(),
             sqlDialectManager.getSqlDialect(provider.getDslContext()),
             sqlDialectManager.getSqlDialect(provider.getDslContext()).quoteIdentifier(Tables.BITHON_ALERT_OBJECT.getName()),
             sqlDialectManager.getSqlDialect(provider.getDslContext()).quoteIdentifier(Tables.BITHON_ALERT_STATE.getName())
            );
    }

    public AlertObjectJdbcStorage(DSLContext dslContext,
                                  ISqlDialect sqlDialect,
                                  String objectTableSelectName,
                                  String stateTableSelectName) {
        this.dslContext = dslContext;
        this.sqlDialect = sqlDialect;
        this.quotedObjectTableSelectName = objectTableSelectName;
        this.quotedStateTableSelectName = stateTableSelectName;
    }

    @Override
    public void initialize() {
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
                         .fetchInto(AlertStorageObject.class);
    }

    @Override
    public boolean existAlert(String alertId) {
        // use fetchCount instead of fetchExists
        // because the former one uses 'exists' sub-clause in the generated SQL,
        // and for ClickHouse, this clause is not recognizable
        return dslContext.fetchCount(dslContext.selectFrom(this.quotedObjectTableSelectName)
                                               .where(Tables.BITHON_ALERT_OBJECT.ALERT_ID.eq(alertId))
                                               .and(Tables.BITHON_ALERT_OBJECT.DELETED.eq(false))) > 0;

    }

    @Override
    public AlertStorageObject getAlertById(String alertId) {
        return dslContext.selectFrom(this.quotedObjectTableSelectName)
                         .where(Tables.BITHON_ALERT_OBJECT.ALERT_ID.eq(alertId))
                         .and(Tables.BITHON_ALERT_OBJECT.DELETED.eq(false))
                         .fetchOneInto(AlertStorageObject.class);
    }

    @Override
    public String createAlert(AlertStorageObject alert, String operator, Timestamp createTimestamp, Timestamp updateTimestamp) {
        if (!StringUtils.hasText(alert.getAlertId())) {
            alert.setAlertId(UUID.randomUUID().toString().replace("-", ""));
        }

        try {
            dslContext.insertInto(Tables.BITHON_ALERT_OBJECT)
                      .set(Tables.BITHON_ALERT_OBJECT.ALERT_NAME, alert.getAlertName())
                      .set(Tables.BITHON_ALERT_OBJECT.APP_NAME, alert.getAppName())
                      .set(Tables.BITHON_ALERT_OBJECT.NAMESPACE, alert.getNamespace())
                      .set(Tables.BITHON_ALERT_OBJECT.DISABLED, alert.getDisabled())
                      .set(Tables.BITHON_ALERT_OBJECT.PAYLOAD, alert.getPayload())
                      .set(Tables.BITHON_ALERT_OBJECT.ALERT_ID, alert.getAlertId())
                      .set(Tables.BITHON_ALERT_OBJECT.LAST_OPERATOR, operator)
                      .set(Tables.BITHON_ALERT_OBJECT.CREATED_AT, createTimestamp.toLocalDateTime())
                      .set(Tables.BITHON_ALERT_OBJECT.UPDATED_AT, updateTimestamp.toLocalDateTime())
                      .execute();
        } catch (DuplicateKeyException e) {
            throw new RuntimeException(StringUtils.format("Alert object with id [%s] already exists.", alert.getAlertId()));
        }

        return alert.getAlertId();
    }

    @Override
    public boolean updateAlert(AlertStorageObject oldObject, AlertStorageObject newObject, String operator) {
        return dslContext.update(Tables.BITHON_ALERT_OBJECT)
                         .set(Tables.BITHON_ALERT_OBJECT.ALERT_NAME, newObject.getAlertName())
                         .set(Tables.BITHON_ALERT_OBJECT.APP_NAME, newObject.getAppName())
                         .set(Tables.BITHON_ALERT_OBJECT.NAMESPACE, newObject.getNamespace())
                         .set(Tables.BITHON_ALERT_OBJECT.DISABLED, newObject.getDisabled())
                         .set(Tables.BITHON_ALERT_OBJECT.PAYLOAD, newObject.getPayload())
                         .set(Tables.BITHON_ALERT_OBJECT.LAST_OPERATOR, operator)
                         .set(Tables.BITHON_ALERT_OBJECT.UPDATED_AT, new Timestamp(System.currentTimeMillis()).toLocalDateTime())
                         .where(Tables.BITHON_ALERT_OBJECT.ALERT_ID.eq(newObject.getAlertId()))
                         .execute() > 0;
    }

    @Override
    public boolean disableAlert(String alertId, String operator) {
        return dslContext.update(Tables.BITHON_ALERT_OBJECT)
                         .set(Tables.BITHON_ALERT_OBJECT.DISABLED, true)
                         .set(Tables.BITHON_ALERT_OBJECT.LAST_OPERATOR, operator)
                         .where(Tables.BITHON_ALERT_OBJECT.ALERT_ID.eq(alertId))
                         .and(Tables.BITHON_ALERT_OBJECT.DISABLED.eq(false))
                         .execute() > 0;
    }

    @Override
    public boolean enableAlert(String alertId, String operator) {
        return dslContext.update(Tables.BITHON_ALERT_OBJECT)
                         .set(Tables.BITHON_ALERT_OBJECT.DISABLED, false)
                         .set(Tables.BITHON_ALERT_OBJECT.LAST_OPERATOR, operator)
                         .where(Tables.BITHON_ALERT_OBJECT.ALERT_ID.eq(alertId))
                         .and(Tables.BITHON_ALERT_OBJECT.DISABLED.eq(true))
                         .execute() > 0;
    }

    @Override
    public boolean deleteAlert(String alertId, String operator) {
        return dslContext.update(Tables.BITHON_ALERT_OBJECT)
                         .set(Tables.BITHON_ALERT_OBJECT.DELETED, true)
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
    public ListResult<ListAlertDO> getAlertList(String appNames,
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
                                                             Tables.BITHON_ALERT_OBJECT.LAST_OPERATOR)
                                                     .from(this.quotedObjectTableSelectName)
                                                     .leftJoin(StringUtils.format("(SELECT * FROM %s) AS %s", this.quotedStateTableSelectName, sqlDialect.quoteIdentifier(Tables.BITHON_ALERT_STATE.getName())))
                                                     .on(Tables.BITHON_ALERT_OBJECT.ALERT_ID.eq(Tables.BITHON_ALERT_STATE.ALERT_ID))
                                                     .where(Tables.BITHON_ALERT_OBJECT.DELETED.eq(false));

        if (appNames != null) {
            selectSql = selectSql.and(Tables.BITHON_ALERT_OBJECT.APP_NAME.in(appNames));
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

        return new ListResult<>(dslContext.fetchCount(selectSql),
                                selectSql.orderBy(orderByField)
                                         .offset(limit.getOffset())
                                         .limit(limit.getLimit())
                                         .fetchInto(ListAlertDO.class));
    }

    @Override
    public ListResult<AlertChangeLogObject> getChangeLogs(String alertId, Integer pageNumber, Integer pageSize) {
        SelectConditionStep<?> selectSql = dslContext.selectFrom(Tables.BITHON_ALERT_CHANGE_LOG)
                                                     .where(Tables.BITHON_ALERT_CHANGE_LOG.ALERT_ID.eq(alertId));

        return new ListResult<>(dslContext.fetchCount(selectSql),
                                selectSql.orderBy(Tables.BITHON_ALERT_CHANGE_LOG.CREATED_AT.desc())
                                         .offset(pageNumber * pageSize)
                                         .limit(pageSize)
                                         .fetchInto(AlertChangeLogObject.class));
    }

    @Override
    public <T> T executeTransaction(Callable<T> callable) {
        return this.dslContext.transactionResult((configuration) -> callable.call());
    }
}
