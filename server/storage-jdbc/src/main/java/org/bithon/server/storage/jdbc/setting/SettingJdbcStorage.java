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
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import org.bithon.server.setting.storage.ISettingReader;
import org.bithon.server.setting.storage.ISettingStorage;
import org.bithon.server.storage.jdbc.jooq.Tables;
import org.jooq.DSLContext;

import static org.bithon.server.storage.jdbc.JdbcStorageAutoConfiguration.BITHON_JDBC_DSL;

/**
 * @author Frank Chen
 * @date 4/11/21 3:18 pm
 */
@JsonTypeName("jdbc")
public class SettingJdbcStorage implements ISettingStorage {

    protected final DSLContext dslContext;

    public SettingJdbcStorage(@JacksonInject(value = BITHON_JDBC_DSL, useInput = OptBoolean.FALSE) DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    @Override
    public void initialize() {
        this.dslContext.createTableIfNotExists(Tables.BITHON_AGENT_SETTING)
                       .columns(Tables.BITHON_AGENT_SETTING.fields())
                       .indexes(Tables.BITHON_AGENT_SETTING.getIndexes())
                       .execute();
    }

    @Override
    public ISettingReader createReader() {
        return new SettingJdbcReader(dslContext);
    }
}
