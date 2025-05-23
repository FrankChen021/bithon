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

package org.bithon.server.storage.jdbc.clickhouse.event;


import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.server.datasource.query.setting.QuerySettings;
import org.bithon.server.datasource.reader.jdbc.dialect.SqlDialectManager;
import org.bithon.server.storage.common.expiration.ExpirationConfig;
import org.bithon.server.storage.common.expiration.IExpirationRunnable;
import org.bithon.server.storage.event.EventStorageConfig;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseConfig;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseStorageProviderConfiguration;
import org.bithon.server.storage.jdbc.clickhouse.common.DataCleaner;
import org.bithon.server.storage.jdbc.clickhouse.common.TableCreator;
import org.bithon.server.storage.jdbc.common.jooq.Tables;
import org.bithon.server.storage.jdbc.event.EventJdbcStorage;

import java.sql.Timestamp;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/14 4:19 下午
 */
@JsonTypeName("clickhouse")
public class EventStorage extends EventJdbcStorage {

    private final ClickHouseConfig config;

    @JsonCreator
    public EventStorage(@JacksonInject(useInput = OptBoolean.FALSE) ClickHouseStorageProviderConfiguration storageConfiguration,
                        @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper objectMapper,
                        @JacksonInject(useInput = OptBoolean.FALSE) EventStorageConfig storageConfig,
                        @JacksonInject(useInput = OptBoolean.FALSE) SqlDialectManager sqlDialectManager,
                        @JacksonInject(useInput = OptBoolean.FALSE) QuerySettings querySettings) {
        super(storageConfiguration.getDslContext(), objectMapper, storageConfig, sqlDialectManager, querySettings);
        this.config = storageConfiguration.getClickHouseConfig();
    }

    @Override
    public void initialize() {
        if (storageConfig.isCreateTable()) {
            new TableCreator(config, dslContext).createIfNotExist(Tables.BITHON_EVENT);
        }
    }

    /**
     * Since TTL expression does not allow DateTime64 type, we have to clean the data by ourselves.
     * The data is partitioned by days, so we only clear the data before the day of given timestamp
     */
    @Override
    public IExpirationRunnable getExpirationRunnable() {
        return new IExpirationRunnable() {
            @Override
            public ExpirationConfig getExpirationConfig() {
                return storageConfig.getTtl();
            }

            @Override
            public void expire(Timestamp before) {
                new DataCleaner(config, dslContext).deletePartition(Tables.BITHON_EVENT.getName(), before);
            }
        };
    }
}
