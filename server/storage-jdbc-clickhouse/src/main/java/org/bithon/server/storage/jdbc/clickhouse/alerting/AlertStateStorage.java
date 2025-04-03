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
import org.bithon.server.storage.alerting.AlertingStorageConfiguration;
import org.bithon.server.storage.alerting.pojo.AlertState;
import org.bithon.server.storage.jdbc.alerting.AlertStateJdbcStorage;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseConfig;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseStorageProviderConfiguration;
import org.bithon.server.storage.jdbc.clickhouse.common.TableCreator;
import org.bithon.server.storage.jdbc.common.jooq.Tables;
import org.jooq.BatchBindStep;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2025/1/9 22:51
 */
@JsonTypeName("clickhouse")
public class AlertStateStorage extends AlertStateJdbcStorage {
    private final ClickHouseConfig config;

    @JsonCreator
    public AlertStateStorage(@JacksonInject(useInput = OptBoolean.FALSE) ClickHouseStorageProviderConfiguration storageProvider,
                             @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper objectMapper,
                             @JacksonInject(useInput = OptBoolean.FALSE) AlertingStorageConfiguration.AlertStorageConfig storageConfig) {
        super(storageProvider.getDslContext(),
              storageConfig,
              objectMapper,
              Tables.BITHON_ALERT_STATE.getName() + " FINAL");

        this.config = storageProvider.getClickHouseConfig();
    }

    @Override
    protected void createTableIfNotExists() {
        new TableCreator(this.config, this.dslContext).useReplacingMergeTree(Tables.BITHON_ALERT_STATE.UPDATE_AT.getName())
                                                      .partitionByExpression(null)
                                                      .createIfNotExist(Tables.BITHON_ALERT_STATE);
    }

    @Override
    public void updateAlertStates(Map<String, AlertState> states) {
        BatchBindStep step = dslContext.batch(dslContext.insertInto(Tables.BITHON_ALERT_STATE,
                                                                    Tables.BITHON_ALERT_STATE.ALERT_ID,
                                                                    Tables.BITHON_ALERT_STATE.LAST_ALERT_AT,
                                                                    Tables.BITHON_ALERT_STATE.LAST_RECORD_ID,
                                                                    Tables.BITHON_ALERT_STATE.UPDATE_AT,
                                                                    Tables.BITHON_ALERT_STATE.PAYLOAD,
                                                                    Tables.BITHON_ALERT_STATE.ALERT_STATUS,
                                                                    Tables.BITHON_ALERT_STATE.LAST_EVALUATED_AT
                                                        )
                                                        .values((String) null,
                                                                null,
                                                                null,
                                                                null,
                                                                null,
                                                                null,
                                                                null
                                                        ));

        for (Map.Entry<String, AlertState> entry : states.entrySet()) {
            String ruleId = entry.getKey();
            AlertState state = entry.getValue();
            String payloadString;
            try {
                payloadString = objectMapper.writeValueAsString(state.getPayload());
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            LocalDateTime now = new Timestamp(System.currentTimeMillis()).toLocalDateTime();
            step = step.bind(ruleId,
                             state.getLastAlertAt() == null ? new Timestamp(0).toLocalDateTime() : state.getLastAlertAt(),
                             state.getLastRecordId() == null ? "" : state.getLastRecordId(),
                             now,
                             payloadString,
                             state.getStatus().statusCode(),
                             now);
        }
        step.execute();
    }
}
