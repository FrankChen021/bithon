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

import org.bithon.server.storage.jdbc.common.jooq.Tables;
import org.bithon.server.storage.setting.ISettingWriter;
import org.jooq.DSLContext;

import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * @author Frank Chen
 * @date 26/1/24 2:22 pm
 */
public class SettingJdbcWriter implements ISettingWriter {
    protected final DSLContext dslContext;

    public SettingJdbcWriter(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    @Override
    public void addSetting(String app, String name, String value, String format) {
        LocalDateTime now = new Timestamp(System.currentTimeMillis()).toLocalDateTime();
        dslContext.insertInto(Tables.BITHON_AGENT_SETTING)
                  .set(Tables.BITHON_AGENT_SETTING.APPNAME, app)
                  .set(Tables.BITHON_AGENT_SETTING.SETTINGNAME, name)
                  .set(Tables.BITHON_AGENT_SETTING.SETTING, value)
                  .set(Tables.BITHON_AGENT_SETTING.FORMAT, format)
                  .set(Tables.BITHON_AGENT_SETTING.TIMESTAMP, now)
                  .set(Tables.BITHON_AGENT_SETTING.UPDATEDAT, now)
                  .execute();
    }

    @Override
    public void deleteSetting(String app, String name) {
        dslContext.deleteFrom(Tables.BITHON_AGENT_SETTING)
                  .where(Tables.BITHON_AGENT_SETTING.SETTINGNAME.eq(name))
                  .execute();
    }
}
