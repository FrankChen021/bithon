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
import org.bithon.server.storage.alerting.AlertingStorageConfiguration;
import org.bithon.server.storage.alerting.IAlertStateStorage;
import org.bithon.server.storage.alerting.pojo.AlertState;
import org.bithon.server.storage.alerting.pojo.AlertStatus;
import org.bithon.server.storage.jdbc.JdbcStorageProviderConfiguration;
import org.bithon.server.storage.jdbc.common.dialect.SqlDialectManager;
import org.bithon.server.storage.jdbc.common.jooq.Tables;
import org.jooq.DSLContext;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2025/1/9 22:46
 */
public class AlertStateJdbcStorage implements IAlertStateStorage {

    protected final DSLContext dslContext;
    protected final AlertingStorageConfiguration.AlertStorageConfig storageConfig;
    protected final ObjectMapper objectMapper;
    protected final String stateTableSelectName;

    @JsonCreator
    public AlertStateJdbcStorage(@JacksonInject(useInput = OptBoolean.FALSE) JdbcStorageProviderConfiguration storageProvider,
                                 @JacksonInject(useInput = OptBoolean.FALSE) AlertingStorageConfiguration.AlertStorageConfig storageConfig,
                                 @JacksonInject(useInput = OptBoolean.FALSE) SqlDialectManager sqlDialectManager,
                                 @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper objectMapper) {
        this(storageProvider.getDslContext(),
             storageConfig,
             objectMapper,
             sqlDialectManager.getSqlDialect(storageProvider.getDslContext()).quoteIdentifier(Tables.BITHON_ALERT_STATE.getName())
        );
    }

    public AlertStateJdbcStorage(DSLContext dslContext,
                                 AlertingStorageConfiguration.AlertStorageConfig storageConfig,
                                 ObjectMapper objectMapper,
                                 String stateTableSelectName) {
        this.dslContext = dslContext;
        this.storageConfig = storageConfig;
        this.objectMapper = objectMapper;
        this.stateTableSelectName = stateTableSelectName;
    }

    public final void initialize() {
        if (!this.storageConfig.isCreateTable()) {
            return;
        }

        createTableIfNotExists();
    }

    protected void createTableIfNotExists() {
        this.dslContext.createTableIfNotExists(Tables.BITHON_ALERT_STATE)
                       .columns(Tables.BITHON_ALERT_STATE.fields())
                       .indexes(Tables.BITHON_ALERT_STATE.getIndexes())
                       .execute();
    }

    @Override
    public Map<String, AlertState> getAlertStates() {
        return dslContext.selectFrom(this.stateTableSelectName)
                         .fetchMap(Tables.BITHON_ALERT_STATE.ALERT_ID, (record) -> {
                             AlertState obj = new AlertState();
                             obj.setStatus(AlertStatus.fromCode(record.get(Tables.BITHON_ALERT_STATE.ALERT_STATUS)));

                             String payload = record.get(Tables.BITHON_ALERT_STATE.PAYLOAD);
                             if (payload != null && !payload.isEmpty()) {
                                 try {
                                     obj.setPayload(objectMapper.readValue(payload, AlertState.Payload.class));
                                 } catch (JsonProcessingException e) {
                                     throw new RuntimeException(e);
                                 }
                             }

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

    @Override
    public void updateAlertStates(Map<String, AlertState> states) {
        for (Map.Entry<String, AlertState> entry : states.entrySet()) {
            String ruleId = entry.getKey();
            AlertState state = entry.getValue();
            String payloadString;
            try {
                payloadString = objectMapper.writeValueAsString(state.getPayload());
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            dslContext.insertInto(Tables.BITHON_ALERT_STATE)
                      .set(Tables.BITHON_ALERT_STATE.ALERT_ID, ruleId)
                      .set(Tables.BITHON_ALERT_STATE.LAST_ALERT_AT, state.getLastAlertAt() == null ? new Timestamp(0).toLocalDateTime() : state.getLastAlertAt())
                      .set(Tables.BITHON_ALERT_STATE.LAST_RECORD_ID, state.getLastRecordId() == null ? "" : state.getLastRecordId())
                      .set(Tables.BITHON_ALERT_STATE.UPDATE_AT, new Timestamp(System.currentTimeMillis()).toLocalDateTime())
                      .set(Tables.BITHON_ALERT_STATE.PAYLOAD, payloadString)
                      .set(Tables.BITHON_ALERT_STATE.ALERT_STATUS, state.getStatus().statusCode())
                      .onDuplicateKeyUpdate()
                      .set(Tables.BITHON_ALERT_STATE.UPDATE_AT, new Timestamp(System.currentTimeMillis()).toLocalDateTime())
                      .set(Tables.BITHON_ALERT_STATE.ALERT_STATUS, state.getStatus().statusCode())
                      .set(Tables.BITHON_ALERT_STATE.PAYLOAD, payloadString)
                      .execute();
        }
    }
}
