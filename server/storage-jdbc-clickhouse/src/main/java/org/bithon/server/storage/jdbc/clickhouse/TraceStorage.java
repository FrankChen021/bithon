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
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.jdbc.jooq.Tables;
import org.bithon.server.storage.jdbc.tracing.TraceJdbcStorage;
import org.bithon.server.tracing.storage.ITraceCleaner;
import org.jooq.DSLContext;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/10/27 9:34 下午
 */
@Slf4j
@JsonTypeName("clickhouse")
public class TraceStorage extends TraceJdbcStorage {

    private final ClickHouseConfig config;

    @JsonCreator
    public TraceStorage(@JacksonInject(useInput = OptBoolean.FALSE) DSLContext dslContext,
                        @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper objectMapper,
                        @JacksonInject(useInput = OptBoolean.FALSE) ClickHouseConfig config) {
        super(dslContext, objectMapper);
        this.config = config;
    }

    @Override
    public void initialize() {
        new TableCreator(config, dslContext).createIfNotExist(Tables.BITHON_TRACE_SPAN, config.getTtlDays());
    }

    @Override
    public ITraceCleaner createCleaner() {
        return beforeTimestamp -> dslContext.execute(StringUtils.format("ALTER TABLE %s.%s %s DELETE WHERE timestamp < '%s'",
                                                                        config.getDatabase(),
                                                                        config.getLocalTableName(Tables.BITHON_TRACE_SPAN.getName()),
                                                                        config.getClusterExpression(),
                                                                        StringUtils.formatDateTime("yyyy-MM-dd HH:mm:ss", beforeTimestamp)));
    }
}
