package com.sbss.bithon.component.db.dao;

import com.sbss.bithon.component.db.jooq.Tables;
import com.sbss.bithon.component.db.jooq.tables.records.BithonMetadataRecord;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.springframework.dao.DuplicateKeyException;

import java.sql.Timestamp;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/11 10:40 下午
 */
public class MetadataDAO {
    private final DSLContext dsl;

    public MetadataDAO(DSLContext dsl) {
        this.dsl = dsl;
        if (dsl.configuration().dialect().equals(SQLDialect.H2)) {
            this.dsl.createTableIfNotExists(Tables.BITHON_METADATA)
                .columns(Tables.BITHON_METADATA.ID,
                         Tables.BITHON_METADATA.NAME,
                         Tables.BITHON_METADATA.TYPE,
                         Tables.BITHON_METADATA.PARENT_ID)
                .execute();
        }
    }

    public Long insertMetadata(String name, String type, long parent) {
        try {
            BithonMetadataRecord meta = dsl.insertInto(Tables.BITHON_METADATA)
                .set(Tables.BITHON_METADATA.NAME, name)
                .set(Tables.BITHON_METADATA.TYPE, type)
                .set(Tables.BITHON_METADATA.PARENT_ID, parent)
                .returning(Tables.BITHON_METADATA.ID)
                .fetchOne();
            return meta.getId();
        } catch (DuplicateKeyException e) {
            long id = dsl.select(Tables.BITHON_METADATA.ID).from(Tables.BITHON_METADATA).where(Tables.BITHON_METADATA.NAME.eq(name))
                .and(Tables.BITHON_METADATA.TYPE.eq(type))
                .and(Tables.BITHON_METADATA.PARENT_ID.eq(parent))
                .fetchOne(Tables.BITHON_METADATA.ID);

            dsl.update(Tables.BITHON_METADATA)
                .set(Tables.BITHON_METADATA.UPDATED_AT, new Timestamp(System.currentTimeMillis()))
                .where(Tables.BITHON_METADATA.ID.eq(id))
                .execute();
            return id;
        }
    }
}
