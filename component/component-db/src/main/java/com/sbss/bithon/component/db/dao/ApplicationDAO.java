package com.sbss.bithon.component.db.dao;

import com.sbss.bithon.component.db.jooq.Tables;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.springframework.stereotype.Component;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/11 10:40 下午
 */
@Component
public class ApplicationDAO {

    private final DSLContext dsl;

    public ApplicationDAO(DSLContext dsl) {
        this.dsl = dsl;
        if (dsl.configuration().dialect().equals(SQLDialect.H2)) {
            this.dsl.createTableIfNotExists(Tables.BITHON_APPLICATION).execute();
        }
    }
}
