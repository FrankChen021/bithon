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

package org.bithon.component.db.dao;

import org.bithon.component.db.jooq.Tables;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/16 7:40 下午
 */
@Component
public class SettingDAO {
    private final DSLContext dsl;

    public SettingDAO(DSLContext dsl) {
        this.dsl = dsl;
        if (dsl.configuration().dialect().equals(SQLDialect.H2)) {
            this.dsl.createTableIfNotExists(Tables.BITHON_AGENT_SETTING)
                .columns(Tables.BITHON_AGENT_SETTING.ID,
                         Tables.BITHON_AGENT_SETTING.SETTING,
                         Tables.BITHON_AGENT_SETTING.SETTING_NAME,
                         Tables.BITHON_AGENT_SETTING.APP_NAME,
                         Tables.BITHON_AGENT_SETTING.UPDATED_AT)
                .execute();
        }
    }

    public Map<String, String> getSettings(String appName, long since) {
        return dsl.select(Tables.BITHON_AGENT_SETTING.SETTING_NAME, Tables.BITHON_AGENT_SETTING.SETTING)
            .from(Tables.BITHON_AGENT_SETTING)
            .where(Tables.BITHON_AGENT_SETTING.APP_NAME.eq(appName))
            .and(Tables.BITHON_AGENT_SETTING.UPDATED_AT.gt(new Timestamp(since)))
            .fetchMap(Tables.BITHON_AGENT_SETTING.SETTING_NAME, Tables.BITHON_AGENT_SETTING.SETTING);
    }
}
