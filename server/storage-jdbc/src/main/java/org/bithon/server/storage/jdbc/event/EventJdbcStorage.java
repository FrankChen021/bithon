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

package org.bithon.server.storage.jdbc.event;


import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.server.datasource.query.setting.QuerySettings;
import org.bithon.server.datasource.reader.jdbc.dialect.ISqlDialect;
import org.bithon.server.datasource.reader.jdbc.dialect.SqlDialectManager;
import org.bithon.server.storage.common.expiration.ExpirationConfig;
import org.bithon.server.storage.common.expiration.IExpirationRunnable;
import org.bithon.server.storage.event.EventStorageConfig;
import org.bithon.server.storage.event.IEventReader;
import org.bithon.server.storage.event.IEventStorage;
import org.bithon.server.storage.event.IEventWriter;
import org.bithon.server.storage.jdbc.JdbcStorageProviderConfiguration;
import org.bithon.server.storage.jdbc.common.jooq.Tables;
import org.jooq.DSLContext;

import java.sql.Timestamp;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/14 4:19 下午
 */
public class EventJdbcStorage implements IEventStorage {

    protected final DSLContext dslContext;
    protected final ObjectMapper objectMapper;
    protected final EventStorageConfig storageConfig;
    private final ISqlDialect sqlDialect;
    protected final QuerySettings querySettings;

    @JsonCreator
    public EventJdbcStorage(@JacksonInject(useInput = OptBoolean.FALSE) JdbcStorageProviderConfiguration providerConfiguration,
                            @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper objectMapper,
                            @JacksonInject(useInput = OptBoolean.FALSE) EventStorageConfig storageConfig,
                            @JacksonInject(useInput = OptBoolean.FALSE) SqlDialectManager sqlDialectManager,
                            @JacksonInject(useInput = OptBoolean.FALSE) QuerySettings querySettings) {
        this(providerConfiguration.getDslContext(),
             objectMapper,
             storageConfig,
             sqlDialectManager,
             querySettings);
    }

    protected EventJdbcStorage(DSLContext dslContext,
                               ObjectMapper objectMapper,
                               EventStorageConfig storageConfig,
                               SqlDialectManager sqlDialectManager,
                               QuerySettings querySettings) {
        this.dslContext = dslContext;
        this.objectMapper = objectMapper;
        this.storageConfig = storageConfig;
        this.sqlDialect = sqlDialectManager.getSqlDialect(dslContext);
        this.querySettings = querySettings;
    }

    @Override
    public void initialize() {
        if (!storageConfig.isCreateTable()) {
            return;
        }

        this.dslContext.createTableIfNotExists(Tables.BITHON_EVENT)
                       .columns(Tables.BITHON_EVENT.fields())
                       .indexes(Tables.BITHON_EVENT.getIndexes())
                       .execute();
    }

    @Override
    public IEventWriter createWriter() {
        return new EventJdbcWriter(dslContext);
    }

    @Override
    public IEventReader createReader() {
        return new EventJdbcReader(this.dslContext, this.sqlDialect, this.querySettings);
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
                dslContext.deleteFrom(Tables.BITHON_EVENT)
                          .where(Tables.BITHON_EVENT.TIMESTAMP.lt(before.toLocalDateTime()))
                          .execute();
            }
        };
    }
}
