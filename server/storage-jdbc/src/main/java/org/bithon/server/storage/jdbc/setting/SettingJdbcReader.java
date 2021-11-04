/*
 *    Copyright 2020 bithon.cn
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

import org.bithon.component.db.jooq.Tables;
import org.bithon.server.setting.storage.ISettingReader;
import org.jooq.DSLContext;

import java.sql.Timestamp;
import java.util.Map;

/**
 * @author Frank Chen
 * @date 4/11/21 3:18 pm
 */
public class SettingJdbcReader implements ISettingReader {

    private final DSLContext dslContext;

    public SettingJdbcReader(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    @Override
    public Map<String, String> getSettings(String appName, long since) {
        return dslContext.select(Tables.BITHON_AGENT_SETTING.SETTINGNAME, Tables.BITHON_AGENT_SETTING.SETTING)
                         .from(Tables.BITHON_AGENT_SETTING)
                         .where(Tables.BITHON_AGENT_SETTING.APPNAME.eq(appName))
                         .and(Tables.BITHON_AGENT_SETTING.UPDATEDAT.gt(new Timestamp(since)))
                         .groupBy(Tables.BITHON_AGENT_SETTING.SETTINGNAME, Tables.BITHON_AGENT_SETTING.SETTING)
                         .orderBy(Tables.BITHON_AGENT_SETTING.UPDATEDAT.desc())
                         .limit(1)
                         .fetchMap(Tables.BITHON_AGENT_SETTING.SETTINGNAME, Tables.BITHON_AGENT_SETTING.SETTING);
    }
}
