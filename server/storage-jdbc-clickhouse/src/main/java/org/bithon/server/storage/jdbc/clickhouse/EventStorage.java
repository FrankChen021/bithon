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
import org.bithon.component.commons.time.DateTime;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.event.storage.IEventCleaner;
import org.bithon.server.storage.jdbc.event.EventJdbcStorage;
import org.bithon.server.storage.jdbc.jooq.Tables;
import org.jooq.DSLContext;

import static org.bithon.server.storage.jdbc.clickhouse.ClickHouseStorageAutoConfiguration.BITHON_CLICKHOUSE_DSL;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/14 4:19 下午
 */
@JsonTypeName("clickhouse")
public class EventStorage extends EventJdbcStorage {

    private final ClickHouseConfig config;

    @JsonCreator
    public EventStorage(@JacksonInject(value = BITHON_CLICKHOUSE_DSL, useInput = OptBoolean.FALSE) DSLContext dslContext,
                        @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper objectMapper,
                        @JacksonInject(useInput = OptBoolean.FALSE) ClickHouseConfig config) {
        super(dslContext, objectMapper);
        this.config = config;
    }

    @Override
    public void initialize() {
        new TableCreator(config, dslContext).createIfNotExist(Tables.BITHON_EVENT, config.getTtlDays());
    }

    /**
     * Since TTL expression does not allow DateTime64 type, we have to clean the data by ourselves.
     * The data is partitioned by days, so we only clear the data before the day of given timestamp
     */
    @Override
    public IEventCleaner createCleaner() {
        return beforeTimestamp -> dslContext.execute(StringUtils.format("ALTER TABLE %s.%s %s DELETE WHERE timestamp < '%s'",
                                                                        config.getDatabase(),
                                                                        config.getLocalTableName(Tables.BITHON_EVENT.getName()),
                                                                        config.getClusterExpression(),
                                                                        DateTime.toYYYYMMDDhhmmss(beforeTimestamp)));
    }
}
