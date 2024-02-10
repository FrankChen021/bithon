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
import org.bithon.server.storage.jdbc.setting.SettingJdbcWriter;
import org.bithon.server.storage.setting.ISettingReader;
import org.bithon.server.storage.setting.ISettingWriter;
import org.bithon.server.storage.setting.SettingStorageConfig;

import java.sql.Timestamp;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 4/11/21 3:34 pm
 */
@JsonTypeName("clickhouse")
public class SettingStorage extends SettingJdbcStorage {

    private final ClickHouseConfig config;

    @JsonCreator
    public SettingStorage(@JacksonInject(useInput = OptBoolean.FALSE) ClickHouseStorageProviderConfiguration configuration,
                          @JacksonInject(useInput = OptBoolean.FALSE) SettingStorageConfig storageConfig) {
        super(configuration.getDslContext(), storageConfig);
        this.config = configuration.getClickHouseConfig();
    }

    @Override
    public void initialize() {
        if (!storageConfig.isCreateTable()) {
            return;
        }

        // Apply ReplacingMergeTree to this table
        new TableCreator(config, this.dslContext).useReplacingMergeTree(Tables.BITHON_AGENT_SETTING.UPDATEDAT.getName())
                                                 .partitionByExpression(null)
                                                 .createIfNotExist(Tables.BITHON_AGENT_SETTING);
    }

    @Override
    public ISettingReader createReader() {
        return new SettingJdbcReader(this.dslContext) {
            @Override
            public List<SettingEntry> getSettings(String appName, String env, long since) {
                String sql = dslContext.selectFrom(Tables.BITHON_AGENT_SETTING)
                                       .getSQL() + " FINAL WHERE ";

                sql += dslContext.renderInlined(Tables.BITHON_AGENT_SETTING.APPNAME.eq(appName)
                                                                                   .and(Tables.BITHON_AGENT_SETTING.ENVIRONMENT.eq(env).or(Tables.BITHON_AGENT_SETTING.ENVIRONMENT.eq("")))
                                                                                   .and(Tables.BITHON_AGENT_SETTING.UPDATEDAT.ge(new Timestamp(since).toLocalDateTime())));

                return dslContext.fetch(sql).map(this::toSettingEntry);
            }

            @Override
            public SettingEntry getSetting(String appName, String env, String setting) {
                String sql = dslContext.selectFrom(Tables.BITHON_AGENT_SETTING)
                                       .getSQL() + " FINAL WHERE ";

                sql += dslContext.renderInlined(Tables.BITHON_AGENT_SETTING.APPNAME.eq(appName)
                                                                                   .and(Tables.BITHON_AGENT_SETTING.ENVIRONMENT.eq(env).or(Tables.BITHON_AGENT_SETTING.ENVIRONMENT.eq("")))
                                                                                   .and(Tables.BITHON_AGENT_SETTING.SETTINGNAME.eq(setting)));

                return super.toSettingEntry(dslContext.fetchOne(sql));
            }
        };
    }

    @Override
    public ISettingWriter createWriter() {
        return new SettingJdbcWriter(dslContext) {
            @Override
            public void updateSetting(String appName, String env, String name, String value, String format) {
                // For ClickHouse, since the ReplacingMergeTree is used,
                // we just INSERT a new record to overwrite the old one
                super.addSetting(appName, env, name, value, format);
            }
        };
    }
}
