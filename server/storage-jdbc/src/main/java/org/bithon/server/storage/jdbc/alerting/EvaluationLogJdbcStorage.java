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
import org.bithon.server.storage.alerting.IEvaluationLogReader;
import org.bithon.server.storage.alerting.IEvaluationLogStorage;
import org.bithon.server.storage.alerting.IEvaluationLogWriter;
import org.bithon.server.storage.common.expiration.ExpirationConfig;
import org.bithon.server.storage.common.expiration.IExpirationRunnable;
import org.bithon.server.storage.jdbc.JdbcStorageProviderConfiguration;
import org.bithon.server.storage.jdbc.common.jooq.Tables;
import org.jooq.DSLContext;

import java.sql.Timestamp;

/**
 * @author Frank Chen
 * @date 19/3/22 12:49 PM
 */
public class EvaluationLogJdbcStorage implements IEvaluationLogStorage {
    protected final DSLContext dslContext;
    protected final AlertingStorageConfiguration.EvaluationLogConfig storageConfig;

    @JsonCreator
    public EvaluationLogJdbcStorage(@JacksonInject(useInput = OptBoolean.FALSE) JdbcStorageProviderConfiguration storageProvider,
                                    @JacksonInject(useInput = OptBoolean.FALSE) AlertingStorageConfiguration.EvaluationLogConfig storageConfig) {
        this(storageProvider.getDslContext(), storageConfig);
    }

    public EvaluationLogJdbcStorage(DSLContext dslContext, AlertingStorageConfiguration.EvaluationLogConfig storageConfig) {
        this.dslContext = dslContext;
        this.storageConfig = storageConfig;
    }

    @Override
    public void initialize() {
        if (!this.storageConfig.isCreateTable()) {
            return;
        }
        dslContext.createTableIfNotExists(Tables.BITHON_ALERT_EVALUATION_LOG)
                  .columns(Tables.BITHON_ALERT_EVALUATION_LOG.fields())
                  .indexes(Tables.BITHON_ALERT_EVALUATION_LOG.getIndexes())
                  .execute();
    }

    @Override
    public IEvaluationLogWriter createWriter() {
        return new JdbcLogWriter(dslContext);
    }

    @Override
    public IEvaluationLogReader createReader() {
        return new JdbcLogReader(dslContext);
    }

    @Override
    public IExpirationRunnable getExpirationRunnable() {
        return new IExpirationRunnable() {
            @Override
            public ExpirationConfig getExpirationConfig() {
                return storageConfig.getTtl();
            }

            @Override
            public void expire(Timestamp before) {
                dslContext.deleteFrom(Tables.BITHON_ALERT_EVALUATION_LOG)
                          .where(Tables.BITHON_ALERT_EVALUATION_LOG.TIMESTAMP.le(before.toLocalDateTime()))
                          .execute();
            }
        };
    }

    @Override
    public String getName() {
        return "alert-evaluation-log";
    }
}
