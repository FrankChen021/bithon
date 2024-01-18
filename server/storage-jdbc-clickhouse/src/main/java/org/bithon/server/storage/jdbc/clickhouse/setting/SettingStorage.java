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

package org.bithon.server.storage.jdbc.clickhouse.setting;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseConfig;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseStorageProviderConfiguration;
import org.bithon.server.storage.jdbc.clickhouse.common.TableCreator;
import org.bithon.server.storage.jdbc.common.jooq.Tables;
import org.bithon.server.storage.jdbc.setting.SettingJdbcReader;
import org.bithon.server.storage.jdbc.setting.SettingJdbcStorage;
import org.bithon.server.storage.setting.ISettingReader;

import java.sql.Timestamp;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 4/11/21 3:34 pm
 */
@JsonTypeName("clickhouse")
public class SettingStorage extends SettingJdbcStorage {

    private final ClickHouseConfig config;

    @JsonCreator
    public SettingStorage(@JacksonInject(useInput = OptBoolean.FALSE) ClickHouseStorageProviderConfiguration configuration) {
        super(configuration.getDslContext());
        this.config = configuration.getClickHouseConfig();
    }

    @Override
    public void initialize() {
        // Apply ReplacingMergeTree to this table
        new TableCreator(config, this.dslContext).useReplacingMergeTree(Tables.BITHON_AGENT_SETTING.UPDATEDAT.getName())
                                                 .partitionByExpression(null)
                                                 .createIfNotExist(Tables.BITHON_AGENT_SETTING);
    }

    @Override
    public ISettingReader createReader() {
        return new SettingJdbcReader(this.dslContext) {
            @Override
            public Map<String, String> getSettings(String appName, long since) {
                String sql = dslContext.select(Tables.BITHON_AGENT_SETTING.SETTINGNAME, Tables.BITHON_AGENT_SETTING.SETTING)
                                       .from(Tables.BITHON_AGENT_SETTING)
                                       .getSQL() + " FINAL WHERE ";

                sql += dslContext.renderInlined(Tables.BITHON_AGENT_SETTING.APPNAME.eq(appName)
                                                                                   .and(Tables.BITHON_AGENT_SETTING.UPDATEDAT.ge(new Timestamp(since).toLocalDateTime())));

                return dslContext.fetch(sql).intoMap(Tables.BITHON_AGENT_SETTING.SETTINGNAME, Tables.BITHON_AGENT_SETTING.SETTING);
            }
        };
    }
}
