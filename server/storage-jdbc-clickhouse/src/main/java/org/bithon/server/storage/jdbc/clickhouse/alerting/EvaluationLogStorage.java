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
import org.bithon.server.storage.alerting.AlertingStorageConfiguration;
import org.bithon.server.storage.common.expiration.ExpirationConfig;
import org.bithon.server.storage.common.expiration.IExpirationRunnable;
import org.bithon.server.storage.jdbc.alerting.EvaluationLogJdbcStorage;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseConfig;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseStorageProviderConfiguration;
import org.bithon.server.storage.jdbc.clickhouse.common.DataCleaner;
import org.bithon.server.storage.jdbc.clickhouse.common.TableCreator;
import org.bithon.server.storage.jdbc.common.jooq.Tables;

import java.sql.Timestamp;

/**
 * @author Frank Chen
 * @date 19/3/22 12:49 PM
 */
@JsonTypeName("clickhouse")
public class EvaluationLogStorage extends EvaluationLogJdbcStorage {

    private final ClickHouseConfig clickHouseConfig;

    @JsonCreator
    public EvaluationLogStorage(@JacksonInject(useInput = OptBoolean.FALSE) ClickHouseStorageProviderConfiguration storageProvider,
                                @JacksonInject(useInput = OptBoolean.FALSE) AlertingStorageConfiguration.EvaluationLogConfig storageConfig) {
        super(storageProvider.getDslContext(), storageConfig);

        this.clickHouseConfig = storageProvider.getClickHouseConfig();
    }

    @Override
    public void initialize() {
        if (!this.storageConfig.isCreateTable()) {
            return;
        }
        new TableCreator(this.clickHouseConfig, this.dslContext)
            .partitionByExpression(StringUtils.format("toYYYYMMDD(%s)", Tables.BITHON_ALERT_EVALUATION_LOG.TIMESTAMP.getName()))
            .createIfNotExist(Tables.BITHON_ALERT_EVALUATION_LOG);
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
                new DataCleaner(clickHouseConfig, dslContext).deleteFromPartition(Tables.BITHON_ALERT_EVALUATION_LOG.getName(), before);
            }
        };
    }
}
