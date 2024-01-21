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
import org.bithon.server.storage.alerting.pojo.AlertStorageObject;
import org.bithon.server.storage.jdbc.alerting.AlertObjectJdbcStorage;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseConfig;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseStorageProviderConfiguration;
import org.bithon.server.storage.jdbc.clickhouse.common.TableCreator;
import org.bithon.server.storage.jdbc.common.dialect.SqlDialectManager;
import org.bithon.server.storage.jdbc.common.jooq.Tables;

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
    public AlertObjectStorage(@JacksonInject(useInput = OptBoolean.FALSE) ClickHouseStorageProviderConfiguration provider,
                              @JacksonInject(useInput = OptBoolean.FALSE)SqlDialectManager sqlDialectManager) {
        super(provider.getDslContext(),
              sqlDialectManager.getSqlDialect(provider.getDslContext()),
              Tables.BITHON_ALERT_OBJECT.getName() + " FINAL",
              Tables.BITHON_ALERT_STATE.getName() + " FINAL");

        this.config = provider.getClickHouseConfig();
    }

    @Override
    public void initialize() {
        new TableCreator(this.config, this.dslContext).useReplacingMergeTree(Tables.BITHON_ALERT_OBJECT.UPDATED_AT.getName())
                                                      .partitionByExpression(null)
                                                      .createIfNotExist(Tables.BITHON_ALERT_OBJECT);

        new TableCreator(this.config, this.dslContext).partitionByExpression(StringUtils.format("toYYYYMMDD(%s)",
                                                                                                Tables.BITHON_ALERT_CHANGE_LOG.CREATED_AT.getName()))
                                                      .createIfNotExist(Tables.BITHON_ALERT_CHANGE_LOG);

        new TableCreator(this.config, this.dslContext).useReplacingMergeTree(Tables.BITHON_ALERT_STATE.LAST_ALERT_AT.getName())
                                                      .partitionByExpression(null)
                                                      .createIfNotExist(Tables.BITHON_ALERT_STATE);
    }

    @Override
    public boolean updateAlert(AlertStorageObject oldObject, AlertStorageObject newObject, String operator) {
        return dslContext.insertInto(Tables.BITHON_ALERT_OBJECT)
                         .set(Tables.BITHON_ALERT_OBJECT.ALERT_NAME, newObject.getAlertName())
                         .set(Tables.BITHON_ALERT_OBJECT.APP_NAME, newObject.getAppName())
                         .set(Tables.BITHON_ALERT_OBJECT.NAMESPACE, newObject.getNamespace())
                         .set(Tables.BITHON_ALERT_OBJECT.DISABLED, newObject.getDisabled())
                         .set(Tables.BITHON_ALERT_OBJECT.PAYLOAD, newObject.getPayload())
                         .set(Tables.BITHON_ALERT_OBJECT.ALERT_ID, newObject.getAlertId())
                         .set(Tables.BITHON_ALERT_OBJECT.LAST_OPERATOR, operator)
                         .set(Tables.BITHON_ALERT_OBJECT.CREATED_AT, oldObject.getCreatedAt().toLocalDateTime())
                         .set(Tables.BITHON_ALERT_OBJECT.UPDATED_AT, new Timestamp(System.currentTimeMillis()).toLocalDateTime())
                         .execute() > 0;
    }

    @Override
    public boolean disableAlert(String alertId, String operator) {
        AlertStorageObject object = this.getAlertById(alertId);
        if (object != null) {
            return dslContext.insertInto(Tables.BITHON_ALERT_OBJECT)
                             .set(Tables.BITHON_ALERT_OBJECT.ALERT_NAME, object.getAlertName())
                             .set(Tables.BITHON_ALERT_OBJECT.APP_NAME, object.getAppName())
                             .set(Tables.BITHON_ALERT_OBJECT.NAMESPACE, object.getNamespace())
                             .set(Tables.BITHON_ALERT_OBJECT.DISABLED, true)
                             .set(Tables.BITHON_ALERT_OBJECT.PAYLOAD, object.getPayload())
                             .set(Tables.BITHON_ALERT_OBJECT.ALERT_ID, object.getAlertId())
                             .set(Tables.BITHON_ALERT_OBJECT.LAST_OPERATOR, operator)
                             .set(Tables.BITHON_ALERT_OBJECT.CREATED_AT, object.getCreatedAt().toLocalDateTime())
                             .set(Tables.BITHON_ALERT_OBJECT.UPDATED_AT, new Timestamp(System.currentTimeMillis()).toLocalDateTime())
                             .execute() > 0;
        }
        return false;
    }

    @Override
    public boolean enableAlert(String alertId, String operator) {
        AlertStorageObject object = this.getAlertById(alertId);
        if (object != null) {
            return dslContext.insertInto(Tables.BITHON_ALERT_OBJECT)
                             .set(Tables.BITHON_ALERT_OBJECT.ALERT_NAME, object.getAlertName())
                             .set(Tables.BITHON_ALERT_OBJECT.APP_NAME, object.getAppName())
                             .set(Tables.BITHON_ALERT_OBJECT.NAMESPACE, object.getNamespace())
                             .set(Tables.BITHON_ALERT_OBJECT.DISABLED, false)
                             .set(Tables.BITHON_ALERT_OBJECT.PAYLOAD, object.getPayload())
                             .set(Tables.BITHON_ALERT_OBJECT.ALERT_ID, object.getAlertId())
                             .set(Tables.BITHON_ALERT_OBJECT.LAST_OPERATOR, operator)
                             .set(Tables.BITHON_ALERT_OBJECT.CREATED_AT, object.getCreatedAt().toLocalDateTime())
                             .set(Tables.BITHON_ALERT_OBJECT.UPDATED_AT, new Timestamp(System.currentTimeMillis()).toLocalDateTime())
                             .execute() > 0;
        }
        return false;
    }

    @Override
    public boolean deleteAlert(String alertId, String operator) {
        AlertStorageObject object = this.getAlertById(alertId);
        if (object != null) {
            return dslContext.insertInto(Tables.BITHON_ALERT_OBJECT)
                             .set(Tables.BITHON_ALERT_OBJECT.ALERT_NAME, object.getAlertName())
                             .set(Tables.BITHON_ALERT_OBJECT.APP_NAME, object.getAppName())
                             .set(Tables.BITHON_ALERT_OBJECT.NAMESPACE, object.getNamespace())
                             .set(Tables.BITHON_ALERT_OBJECT.DISABLED, false)
                             .set(Tables.BITHON_ALERT_OBJECT.DELETED, true)
                             .set(Tables.BITHON_ALERT_OBJECT.PAYLOAD, object.getPayload())
                             .set(Tables.BITHON_ALERT_OBJECT.ALERT_ID, object.getAlertId())
                             .set(Tables.BITHON_ALERT_OBJECT.LAST_OPERATOR, operator)
                             .set(Tables.BITHON_ALERT_OBJECT.CREATED_AT, object.getCreatedAt().toLocalDateTime())
                             .set(Tables.BITHON_ALERT_OBJECT.UPDATED_AT, new Timestamp(System.currentTimeMillis()).toLocalDateTime())
                             .execute() > 0;
        }
        return false;
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
