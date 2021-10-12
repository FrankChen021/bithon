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

import org.bithon.component.db.jooq.DefaultSchema;
import org.bithon.component.db.jooq.Tables;
import org.bithon.component.db.jooq.tables.pojos.BithonMetadata;
import org.bithon.component.db.jooq.tables.pojos.BithonMetricDimension;
import org.bithon.component.db.jooq.tables.records.BithonApplicationTopoRecord;
import org.bithon.component.db.jooq.tables.records.BithonMetadataRecord;
import org.bithon.component.db.jooq.tables.records.BithonMetricDimensionRecord;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.springframework.dao.DuplicateKeyException;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

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

    public long upsertDimension(String dataSource,
                                String dimensionName,
                                String dimensionValue,
                                long timestamp) {
        try {
            BithonMetricDimensionRecord meta = dsl.insertInto(Tables.BITHON_METRIC_DIMENSION)
                                                  .set(Tables.BITHON_METRIC_DIMENSION.DATA_SOURCE, dataSource)
                                                  .set(Tables.BITHON_METRIC_DIMENSION.DIMENSION_NAME, dimensionName)
                                                  .set(Tables.BITHON_METRIC_DIMENSION.DIMENSION_VALUE, dimensionValue)
                                                  .returning(Tables.BITHON_METRIC_DIMENSION.ID)
                                                  .fetchOne();
            return meta.getId();
        } catch (DuplicateKeyException e) {
            long id = dsl.select(Tables.BITHON_METRIC_DIMENSION.ID).from(Tables.BITHON_METRIC_DIMENSION)
                .where(Tables.BITHON_METRIC_DIMENSION.DATA_SOURCE.eq(dataSource))
                .and(Tables.BITHON_METRIC_DIMENSION.DIMENSION_NAME.eq(dimensionName))
                .and(Tables.BITHON_METRIC_DIMENSION.DIMENSION_VALUE.eq(dimensionValue))
                .fetchOne(Tables.BITHON_METRIC_DIMENSION.ID);

            dsl.update(Tables.BITHON_METRIC_DIMENSION)
                .set(Tables.BITHON_METRIC_DIMENSION.UPDATED_AT, new Timestamp(timestamp))
                .where(Tables.BITHON_METRIC_DIMENSION.ID.eq(id))
                .execute();
            return id;
        }
    }

    public long upsertTopo(EndPointType srcEndpointType,
                           String srcEndpoint,
                           EndPointType dstEndpointType,
                           String dstEndpoint) {
        try {
            BithonApplicationTopoRecord topo = dsl.insertInto(Tables.BITHON_APPLICATION_TOPO)
                                                  .set(Tables.BITHON_APPLICATION_TOPO.SRC_ENDPOINT_TYPE, srcEndpointType.name())
                                                  .set(Tables.BITHON_APPLICATION_TOPO.SRC_ENDPOINT, srcEndpoint)
                                                  .set(Tables.BITHON_APPLICATION_TOPO.DST_ENDPOINT_TYPE, dstEndpointType.name())
                                                  .set(Tables.BITHON_APPLICATION_TOPO.DST_ENDPOINT, dstEndpoint)
                                                  .returning(Tables.BITHON_APPLICATION_TOPO.ID)
                                                  .fetchOne();
            return topo.getId();
        } catch (DuplicateKeyException e) {
            long id = dsl.select(Tables.BITHON_APPLICATION_TOPO.ID).from(Tables.BITHON_APPLICATION_TOPO)
                .where(Tables.BITHON_APPLICATION_TOPO.SRC_ENDPOINT_TYPE.eq(srcEndpointType.name()))
                .and(Tables.BITHON_APPLICATION_TOPO.SRC_ENDPOINT.eq(srcEndpoint))
                .and(Tables.BITHON_APPLICATION_TOPO.DST_ENDPOINT_TYPE.eq(dstEndpointType.name()))
                .and(Tables.BITHON_APPLICATION_TOPO.DST_ENDPOINT.eq(dstEndpoint))
                .fetchOne(Tables.BITHON_APPLICATION_TOPO.ID);

            dsl.update(Tables.BITHON_APPLICATION_TOPO)
                .set(Tables.BITHON_APPLICATION_TOPO.UPDATED_AT, new Timestamp(System.currentTimeMillis()))
                .where(Tables.BITHON_APPLICATION_TOPO.ID.eq(id))
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

    public List<BithonMetricDimension> getMetricDimensions(String dataSourceName,
                                                           String dimensionName,
                                                           long startTimestamp,
                                                           long endTimestamp) {
        return dsl.select(Tables.BITHON_METRIC_DIMENSION.ID,
                          Tables.BITHON_METRIC_DIMENSION.DIMENSION_NAME)
            .from(Tables.BITHON_METRIC_DIMENSION)
            .where(Tables.BITHON_METRIC_DIMENSION.DATA_SOURCE.eq(dataSourceName))
            .and(Tables.BITHON_METRIC_DIMENSION.DIMENSION_NAME.eq(dimensionName))
            .and(Tables.BITHON_METRIC_DIMENSION.CREATED_AT.between(new Timestamp(startTimestamp), new Timestamp(endTimestamp)))
            .fetchInto(BithonMetricDimension.class);
    }

    public Collection<BithonMetadata> getMetadata(String type) {
        return dsl.selectFrom(Tables.BITHON_METADATA)
            .where(Tables.BITHON_METADATA.TYPE.eq(type))
            .orderBy(Tables.BITHON_METADATA.UPDATED_AT.desc())
            .limit(10)
            .fetchInto(BithonMetadata.class);
    }
}
