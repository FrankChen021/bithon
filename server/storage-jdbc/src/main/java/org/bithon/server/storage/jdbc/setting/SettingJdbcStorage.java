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

package org.bithon.server.storage.jdbc.setting;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.OptBoolean;
import org.bithon.server.storage.jdbc.JdbcStorageProviderConfiguration;
import org.bithon.server.storage.jdbc.common.jooq.Tables;
import org.bithon.server.storage.setting.ISettingReader;
import org.bithon.server.storage.setting.ISettingStorage;
import org.bithon.server.storage.setting.ISettingWriter;
import org.bithon.server.storage.setting.SettingStorageConfig;
import org.jooq.DSLContext;

/**
 * @author frank.chen021@outlook.com
 * @date 4/11/21 3:18 pm
 */
public class SettingJdbcStorage implements ISettingStorage {

    protected final DSLContext dslContext;
    protected final SettingStorageConfig storageConfig;

    @JsonCreator
    public SettingJdbcStorage(@JacksonInject(useInput = OptBoolean.FALSE) JdbcStorageProviderConfiguration providerConfiguration,
                              @JacksonInject(useInput = OptBoolean.FALSE) SettingStorageConfig storageConfig) {
        this(providerConfiguration.getDslContext(), storageConfig);
    }

    public SettingJdbcStorage(DSLContext dslContext, SettingStorageConfig storageConfig) {
        this.dslContext = dslContext;
        this.storageConfig = storageConfig;
    }

    @Override
    public void initialize() {
        if (!storageConfig.isCreateTable()) {
            return;
        }
        this.dslContext.createTableIfNotExists(Tables.BITHON_AGENT_SETTING)
                       .columns(Tables.BITHON_AGENT_SETTING.fields())
                       .indexes(Tables.BITHON_AGENT_SETTING.getIndexes())
                       .execute();
    }

    @Override
    public ISettingReader createReader() {
        return new SettingJdbcReader(dslContext);
    }

    @Override
    public ISettingWriter createWriter() {
        return new SettingJdbcWriter(dslContext);
    }
}
