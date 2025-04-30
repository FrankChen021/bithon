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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.alerting.AlertingStorageConfiguration;
import org.bithon.server.storage.alerting.pojo.AlertStorageObject;
import org.bithon.server.storage.jdbc.alerting.AlertObjectJdbcStorage;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseConfig;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseStorageProviderConfiguration;
import org.bithon.server.storage.jdbc.clickhouse.common.TableCreator;
import org.bithon.server.storage.jdbc.common.dialect.SqlDialectManager;
import org.bithon.server.storage.jdbc.common.jooq.Tables;
import org.jooq.Select;

import java.sql.Timestamp;
import java.util.concurrent.Callable;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/2 3:45 下午
 */
@JsonTypeName("clickhouse")
public class AlertObjectStorage extends AlertObjectJdbcStorage {

    private final ClickHouseConfig config;

    @JsonCreator
    public AlertObjectStorage(@JacksonInject(useInput = OptBoolean.FALSE) ClickHouseStorageProviderConfiguration storageProvider,
                              @JacksonInject(useInput = OptBoolean.FALSE) SqlDialectManager sqlDialectManager,
                              @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper objectMapper,
                              @JacksonInject(useInput = OptBoolean.FALSE) AlertingStorageConfiguration.AlertStorageConfig storageConfig) {
        super(storageProvider.getDslContext(),
              sqlDialectManager.getSqlDialect(storageProvider.getDslContext()),
              Tables.BITHON_ALERT_OBJECT.getName() + " FINAL",
              Tables.BITHON_ALERT_STATE.getName() + " FINAL",
              objectMapper,
              storageConfig);

        this.config = storageProvider.getClickHouseConfig();
    }

    @Override
    protected void createTableIfNotExists() {
        new TableCreator(this.config, this.dslContext).useReplacingMergeTree(Tables.BITHON_ALERT_OBJECT.UPDATED_AT.getName())
                                                      .partitionByExpression(null)
                                                      .createIfNotExist(Tables.BITHON_ALERT_OBJECT);

        new TableCreator(this.config, this.dslContext).partitionByExpression(StringUtils.format("toYYYYMMDD(%s)",
                                                                                                Tables.BITHON_ALERT_CHANGE_LOG.CREATED_AT.getName()))
                                                      .createIfNotExist(Tables.BITHON_ALERT_CHANGE_LOG);
    }

    @Override
    public boolean updateRule(AlertStorageObject oldObject, AlertStorageObject newObject, String operator) {
        try {
            return dslContext.insertInto(Tables.BITHON_ALERT_OBJECT)
                             .set(Tables.BITHON_ALERT_OBJECT.ALERT_NAME, newObject.getName())
                             .set(Tables.BITHON_ALERT_OBJECT.APP_NAME, newObject.getAppName())
                             .set(Tables.BITHON_ALERT_OBJECT.NAMESPACE, newObject.getNamespace())
                             .set(Tables.BITHON_ALERT_OBJECT.DISABLED, newObject.isDisabled() ? 1 : 0)
                             .set(Tables.BITHON_ALERT_OBJECT.PAYLOAD, objectMapper.writeValueAsString(newObject.getPayload()))
                             .set(Tables.BITHON_ALERT_OBJECT.ALERT_ID, newObject.getId())
                             .set(Tables.BITHON_ALERT_OBJECT.LAST_OPERATOR, operator)
                             .set(Tables.BITHON_ALERT_OBJECT.CREATED_AT, oldObject.getCreatedAt().toLocalDateTime())
                             .set(Tables.BITHON_ALERT_OBJECT.UPDATED_AT, new Timestamp(System.currentTimeMillis()).toLocalDateTime())
                             .execute() > 0;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean disableRule(String alertId, String operator) {
        AlertStorageObject object = this.getRuleById(alertId);
        if (object != null) {
            try {
                return dslContext.insertInto(Tables.BITHON_ALERT_OBJECT)
                                 .set(Tables.BITHON_ALERT_OBJECT.ALERT_NAME, object.getName())
                                 .set(Tables.BITHON_ALERT_OBJECT.APP_NAME, object.getAppName())
                                 .set(Tables.BITHON_ALERT_OBJECT.NAMESPACE, object.getNamespace())
                                 .set(Tables.BITHON_ALERT_OBJECT.DISABLED, 1)
                                 .set(Tables.BITHON_ALERT_OBJECT.DELETED, object.isDeleted() ? 1 : 0)
                                 .set(Tables.BITHON_ALERT_OBJECT.PAYLOAD, objectMapper.writeValueAsString(object.getPayload()))
                                 .set(Tables.BITHON_ALERT_OBJECT.ALERT_ID, object.getId())
                                 .set(Tables.BITHON_ALERT_OBJECT.LAST_OPERATOR, operator)
                                 .set(Tables.BITHON_ALERT_OBJECT.CREATED_AT, object.getCreatedAt().toLocalDateTime())
                                 .set(Tables.BITHON_ALERT_OBJECT.UPDATED_AT, new Timestamp(System.currentTimeMillis()).toLocalDateTime())
                                 .execute() > 0;
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    @Override
    public boolean enableRule(String alertId, String operator) {
        AlertStorageObject object = this.getRuleById(alertId);
        if (object != null) {
            try {
                return dslContext.insertInto(Tables.BITHON_ALERT_OBJECT)
                                 .set(Tables.BITHON_ALERT_OBJECT.ALERT_NAME, object.getName())
                                 .set(Tables.BITHON_ALERT_OBJECT.APP_NAME, object.getAppName())
                                 .set(Tables.BITHON_ALERT_OBJECT.NAMESPACE, object.getNamespace())
                                 .set(Tables.BITHON_ALERT_OBJECT.DISABLED, 0)
                                 .set(Tables.BITHON_ALERT_OBJECT.PAYLOAD, objectMapper.writeValueAsString(object.getPayload()))
                                 .set(Tables.BITHON_ALERT_OBJECT.ALERT_ID, object.getId())
                                 .set(Tables.BITHON_ALERT_OBJECT.LAST_OPERATOR, operator)
                                 .set(Tables.BITHON_ALERT_OBJECT.CREATED_AT, object.getCreatedAt().toLocalDateTime())
                                 .set(Tables.BITHON_ALERT_OBJECT.UPDATED_AT, new Timestamp(System.currentTimeMillis()).toLocalDateTime())
                                 .execute() > 0;
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    @Override
    public boolean deleteRule(String alertId, String operator) {
        AlertStorageObject object = this.getRuleById(alertId);
        if (object != null) {
            try {
                return dslContext.insertInto(Tables.BITHON_ALERT_OBJECT)
                                 .set(Tables.BITHON_ALERT_OBJECT.ALERT_NAME, object.getName())
                                 .set(Tables.BITHON_ALERT_OBJECT.APP_NAME, object.getAppName())
                                 .set(Tables.BITHON_ALERT_OBJECT.NAMESPACE, object.getNamespace())
                                 .set(Tables.BITHON_ALERT_OBJECT.DISABLED, 0)
                                 .set(Tables.BITHON_ALERT_OBJECT.DELETED, 1)
                                 .set(Tables.BITHON_ALERT_OBJECT.PAYLOAD, objectMapper.writeValueAsString(object.getPayload()))
                                 .set(Tables.BITHON_ALERT_OBJECT.ALERT_ID, object.getId())
                                 .set(Tables.BITHON_ALERT_OBJECT.LAST_OPERATOR, operator)
                                 .set(Tables.BITHON_ALERT_OBJECT.CREATED_AT, object.getCreatedAt().toLocalDateTime())
                                 .set(Tables.BITHON_ALERT_OBJECT.UPDATED_AT, new Timestamp(System.currentTimeMillis()).toLocalDateTime())
                                 .execute() > 0;
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    @Override
    protected String getAlertListSql(Select<?> selectQuery) {
        // Make sure the un-joined rows are filled with NULL or the default timestamp might be wrong
        return dslContext.renderInlined(selectQuery) + " SETTINGS join_use_nulls = 1";
    }

    @Override
    public <T> T executeTransaction(Callable<T> callable) {
        // ClickHouse does not support transaction
        try {
            return callable.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
