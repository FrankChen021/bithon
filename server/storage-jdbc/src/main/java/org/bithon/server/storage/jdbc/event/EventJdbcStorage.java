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
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.server.storage.common.IStorageCleaner;
import org.bithon.server.storage.common.TTLConfig;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.DataSourceSchemaManager;
import org.bithon.server.storage.event.EventStorageConfig;
import org.bithon.server.storage.event.IEventReader;
import org.bithon.server.storage.event.IEventStorage;
import org.bithon.server.storage.event.IEventWriter;
import org.bithon.server.storage.jdbc.jooq.Tables;
import org.jooq.DSLContext;

import java.sql.Timestamp;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/14 4:19 下午
 */
@JsonTypeName("jdbc")
public class EventJdbcStorage implements IEventStorage {

    protected final DSLContext dslContext;
    protected final ObjectMapper objectMapper;
    protected final DataSourceSchema eventTableSchema;
    protected final EventStorageConfig storageConfig;

    @JsonCreator
    public EventJdbcStorage(@JacksonInject(useInput = OptBoolean.FALSE) DSLContext dslContext,
                            @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper objectMapper,
                            @JacksonInject(useInput = OptBoolean.FALSE) EventStorageConfig storageConfig,
                            @JacksonInject(useInput = OptBoolean.FALSE) DataSourceSchemaManager schemaManager) {
        this.dslContext = dslContext;
        this.objectMapper = objectMapper;
        this.storageConfig = storageConfig;
        this.eventTableSchema = schemaManager.getDataSourceSchema("event");
    }

    @Override
    public void initialize() {
        this.dslContext.createTableIfNotExists(Tables.BITHON_EVENT).columns(Tables.BITHON_EVENT.fields()).indexes(Tables.BITHON_EVENT.getIndexes()).execute();
    }

    @Override
    public IEventWriter createWriter() {
        return new EventJdbcWriter(dslContext);
    }

    @Override
    public IEventReader createReader() {
        return new EventJdbcReader(dslContext, eventTableSchema);
    }

    @Override
    public IStorageCleaner getCleaner() {
        return new IStorageCleaner() {
            @Override
            public TTLConfig getTTLConfig() {
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
