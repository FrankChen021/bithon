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

package org.bithon.server.storage.jdbc.postgresql.alerting;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.server.datasource.reader.jdbc.dialect.SqlDialectManager;
import org.bithon.server.storage.alerting.AlertingStorageConfiguration;
import org.bithon.server.storage.jdbc.JdbcStorageProviderConfiguration;
import org.bithon.server.storage.jdbc.alerting.AlertStateJdbcStorage;
import org.bithon.server.storage.jdbc.common.jooq.Tables;
import org.bithon.server.storage.jdbc.postgresql.TableCreator;

/**
 * @author frank.chen021@outlook.com
 * @date 2025/1/9 23:03
 */
public class AlertStateStorage extends AlertStateJdbcStorage {

    @JsonCreator
    public AlertStateStorage(@JacksonInject(useInput = OptBoolean.FALSE) JdbcStorageProviderConfiguration storageProvider,
                             @JacksonInject(useInput = OptBoolean.FALSE) SqlDialectManager sqlDialectManager,
                             @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper objectMapper,
                             @JacksonInject(useInput = OptBoolean.FALSE) AlertingStorageConfiguration.AlertStorageConfig storageConfig) {
        super(storageProvider.getDslContext(),
              storageConfig,
              objectMapper,
              sqlDialectManager.getSqlDialect(storageProvider.getDslContext()).quoteIdentifier(Tables.BITHON_ALERT_STATE.getName())
              );
    }

    @Override
    protected void createTableIfNotExists() {
        TableCreator.createTableIfNotExists(this.dslContext, Tables.BITHON_ALERT_STATE);
    }
}
