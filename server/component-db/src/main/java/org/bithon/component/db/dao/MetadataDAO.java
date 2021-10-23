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

package org.bithon.component.db.dao;

import org.bithon.component.db.jooq.DefaultSchema;
import org.bithon.component.db.jooq.Tables;
import org.bithon.component.db.jooq.tables.pojos.BithonMetadata;
import org.bithon.component.db.jooq.tables.records.BithonMetadataRecord;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.springframework.dao.DuplicateKeyException;

import java.sql.Timestamp;
import java.util.Collection;

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
                             Tables.BITHON_METADATA.PARENT_ID,
                             Tables.BITHON_METADATA.CREATED_AT,
                             Tables.BITHON_METADATA.UPDATED_AT)
                    .indexes(DefaultSchema.DEFAULT_SCHEMA.BITHON_METADATA.getIndexes())
                    .execute();

            this.dsl.createTableIfNotExists(Tables.BITHON_METRIC_DIMENSION)
                    .columns(Tables.BITHON_METRIC_DIMENSION.ID,
                             Tables.BITHON_METRIC_DIMENSION.DATA_SOURCE,
                             Tables.BITHON_METRIC_DIMENSION.DIMENSION_NAME,
                             Tables.BITHON_METRIC_DIMENSION.DIMENSION_VALUE,
                             Tables.BITHON_METRIC_DIMENSION.CREATED_AT,
                             Tables.BITHON_METRIC_DIMENSION.UPDATED_AT)
                    .execute();

            this.dsl.createTableIfNotExists(Tables.BITHON_APPLICATION_TOPO)
                    .columns(Tables.BITHON_APPLICATION_TOPO.ID,
                             Tables.BITHON_APPLICATION_TOPO.SRC_ENDPOINT_TYPE,
                             Tables.BITHON_APPLICATION_TOPO.SRC_ENDPOINT,
                             Tables.BITHON_APPLICATION_TOPO.DST_ENDPOINT_TYPE,
                             Tables.BITHON_APPLICATION_TOPO.DST_ENDPOINT,
                             Tables.BITHON_APPLICATION_TOPO.CREATED_AT,
                             Tables.BITHON_APPLICATION_TOPO.UPDATED_AT)
                    .execute();
        }
    }

    public Long upsertMetadata(String name, String type, long parent) {
        try {
            BithonMetadataRecord meta = dsl.insertInto(Tables.BITHON_METADATA)
                                           .set(Tables.BITHON_METADATA.NAME, name)
                                           .set(Tables.BITHON_METADATA.TYPE, type)
                                           .set(Tables.BITHON_METADATA.PARENT_ID, parent)
                                           .returning(Tables.BITHON_METADATA.ID)
                                           .fetchOne();
            return meta.getId();
        } catch (DuplicateKeyException e) {
            long id = dsl.select(Tables.BITHON_METADATA.ID)
                         .from(Tables.BITHON_METADATA)
                         .where(Tables.BITHON_METADATA.NAME.eq(name))
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

    public BithonMetadataRecord getMetaByNameAndType(String instanceName,
                                                     String type) {
        return dsl.selectFrom(Tables.BITHON_METADATA)
                  .where(Tables.BITHON_METADATA.NAME.eq(instanceName))
                  .and(Tables.BITHON_METADATA.TYPE.eq(type))
                  .orderBy(Tables.BITHON_METADATA.UPDATED_AT.desc())
                  .limit(1)
                  .fetchOne();
    }

    public Collection<BithonMetadata> getMetadata(String type) {
        return dsl.selectFrom(Tables.BITHON_METADATA)
                  .where(Tables.BITHON_METADATA.TYPE.eq(type))
                  .orderBy(Tables.BITHON_METADATA.UPDATED_AT.desc())
                  .limit(10)
                  .fetchInto(BithonMetadata.class);
    }
}
