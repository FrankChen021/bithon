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
import org.bithon.server.storage.jdbc.jooq.Tables;
import org.bithon.server.storage.jdbc.meta.MetadataJdbcStorage;
import org.jooq.DSLContext;

import static org.bithon.server.storage.jdbc.clickhouse.ClickHouseStorageAutoConfiguration.BITHON_CLICKHOUSE_DSL;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/10/27 9:56 下午
 */
@JsonTypeName("clickhouse")
public class MetadataStorage extends MetadataJdbcStorage {

    private final ClickHouseConfig config;

    @JsonCreator
    public MetadataStorage(@JacksonInject(value = BITHON_CLICKHOUSE_DSL, useInput = OptBoolean.FALSE) DSLContext dsl,
                           @JacksonInject(useInput = OptBoolean.FALSE) ClickHouseConfig config) {
        super(dsl);
        this.config = config;
    }

    @Override
    public void initialize() {
        new TableCreator(config, this.dslContext).createIfNotExist(Tables.BITHON_APPLICATION_INSTANCE, config.getTtlDays());
    }
}
