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

package org.bithon.server.storage.jdbc.clickhouse;


import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.component.db.jooq.Tables;
import org.bithon.server.event.storage.IEventCleaner;
import org.bithon.server.storage.jdbc.event.EventJdbcStorage;
import org.jooq.DSLContext;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/14 4:19 下午
 */
@JsonTypeName("clickhouse")
public class EventStorage extends EventJdbcStorage {

    private final ClickHouseConfig config;
    private final ClickHouseSqlExpressionFormatter formatter;

    @JsonCreator
    public EventStorage(@JacksonInject(useInput = OptBoolean.FALSE) DSLContext dslContext,
                        @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper objectMapper,
                        @JacksonInject(useInput = OptBoolean.FALSE) ClickHouseConfig config,
                        @JacksonInject(useInput = OptBoolean.FALSE) ClickHouseSqlExpressionFormatter formatter) {
        super(dslContext, objectMapper);
        this.config = config;
        this.formatter = formatter;
    }

    @Override
    public void initialize() {
        new TableCreator(config, formatter, dslContext).createIfNotExist(Tables.BITHON_EVENT, "event");
    }

    @Override
    public IEventCleaner createCleaner() {
        return timestamp -> {
        };
    }
}
