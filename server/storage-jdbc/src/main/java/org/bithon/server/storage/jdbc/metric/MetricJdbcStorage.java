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

package org.bithon.server.storage.jdbc.metric;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import org.bithon.server.storage.common.IStorageCleaner;
import org.bithon.server.storage.common.TTLConfig;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.DataSourceSchemaManager;
import org.bithon.server.storage.jdbc.JdbcJooqContextHolder;
import org.bithon.server.storage.metrics.IMetricReader;
import org.bithon.server.storage.metrics.IMetricStorage;
import org.bithon.server.storage.metrics.IMetricWriter;
import org.bithon.server.storage.metrics.MetricStorageConfig;
import org.bithon.server.storage.metrics.ttl.MetricStorageCleaner;
import org.jooq.CreateTableIndexStep;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;

import java.sql.Timestamp;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/31 1:37 下午
 */
@JsonTypeName("jdbc")
public class MetricJdbcStorage implements IMetricStorage {

    protected final DSLContext dslContext;
    private final Map<String, Boolean> initializedSchemas = new ConcurrentHashMap<>();
    protected final MetricStorageConfig storageConfig;
    protected final DataSourceSchemaManager schemaManager;

    @JsonCreator
    public MetricJdbcStorage(@JacksonInject(useInput = OptBoolean.FALSE) JdbcJooqContextHolder dslContextHolder,
                             @JacksonInject(useInput = OptBoolean.FALSE) DataSourceSchemaManager schemaManager,
                             @JacksonInject(useInput = OptBoolean.FALSE) MetricStorageConfig storageConfig) {
        this(dslContextHolder.getDslContext(), schemaManager, storageConfig);
    }

    public MetricJdbcStorage(DSLContext dslContext,
                             DataSourceSchemaManager schemaManager,
                             MetricStorageConfig storageConfig) {
        this.dslContext = dslContext;
        this.schemaManager = schemaManager;
        this.storageConfig = storageConfig;
    }

    @Override
    public IMetricWriter createMetricWriter(DataSourceSchema schema) {
        MetricTable table = new MetricTable(schema);
        initialize(schema, table);
        return new MetricJdbcWriter(dslContext, table);
    }

    @Override
    public IMetricReader createMetricReader(DataSourceSchema schema) {
        return new MetricJdbcReader(dslContext, getSqlDialect());
    }

    protected ISqlDialect getSqlDialect() {
        if (dslContext.dialect() == SQLDialect.H2) {
            return MetricJdbcReader.H2SqlDialect.INSTANCE;
        } else {
            return MetricJdbcReader.DefaultSqlDialect.INSTANCE;
        }
    }

    protected void initialize(DataSourceSchema schema, MetricTable table) {
        CreateTableIndexStep s = dslContext.createTableIfNotExists(table)
                                           .columns(table.fields())
                                           .indexes(table.getIndexes());
        s.execute();
    }

    @Override
    public IStorageCleaner getCleaner() {
        return new MetricStorageCleaner() {
            @Override
            public TTLConfig getTTLConfig() {
                return storageConfig.getTtl();
            }

            @Override
            protected DataSourceSchemaManager getSchemaManager() {
                return schemaManager;
            }

            @Override
            protected void expireImpl(DataSourceSchema schema, Timestamp before) {
                final MetricTable table = new MetricTable(schema);
                dslContext.deleteFrom(table)
                          .where(table.getTimestampField().le(before))
                          .execute();
            }
        };
    }
}
