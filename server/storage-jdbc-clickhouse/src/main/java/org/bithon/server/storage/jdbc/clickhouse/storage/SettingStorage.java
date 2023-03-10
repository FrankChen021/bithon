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

package org.bithon.server.storage.jdbc.clickhouse.storage;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseConfig;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseJooqContextHolder;
import org.bithon.server.storage.jdbc.jooq.Tables;
import org.bithon.server.storage.jdbc.setting.SettingJdbcStorage;

/**
 * @author frank.chen021@outlook.com
 * @date 4/11/21 3:34 pm
 */
@JsonTypeName("clickhouse")
public class SettingStorage extends SettingJdbcStorage {

    private final ClickHouseConfig config;

    @JsonCreator
    public SettingStorage(@JacksonInject(useInput = OptBoolean.FALSE) ClickHouseJooqContextHolder dslContextHolder,
                          @JacksonInject(useInput = OptBoolean.FALSE) ClickHouseConfig config) {
        super(dslContextHolder.getDslContext());
        this.config = config;
    }

    @Override
    public void initialize() {
        // Apply ReplacingMergeTree to this table
        new TableCreator(config, this.dslContext).useReplacingMergeTree(true)
                                                 .partitionByExpression(null)
                                                 .createIfNotExist(Tables.BITHON_AGENT_SETTING);
    }
}
