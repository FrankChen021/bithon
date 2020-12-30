package com.sbss.bithon.component.db.dao;

import com.sbss.bithon.component.db.jooq.Tables;
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
