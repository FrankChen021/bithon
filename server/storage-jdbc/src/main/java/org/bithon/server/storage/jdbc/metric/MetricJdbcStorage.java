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

import com.alibaba.druid.pool.DruidDataSource;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.storage.common.ExpirationConfig;
import org.bithon.server.storage.common.IExpirationRunnable;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.DataSourceSchemaManager;
import org.bithon.server.storage.datasource.store.IDataStoreSpec;
import org.bithon.server.storage.jdbc.JdbcJooqContextHolder;
import org.bithon.server.storage.jdbc.utils.ISqlDialect;
import org.bithon.server.storage.jdbc.utils.SqlDialectManager;
import org.bithon.server.storage.metrics.IMetricReader;
import org.bithon.server.storage.metrics.IMetricStorage;
import org.bithon.server.storage.metrics.IMetricWriter;
import org.bithon.server.storage.metrics.MetricStorageConfig;
import org.bithon.server.storage.metrics.ttl.MetricStorageCleaner;
import org.jooq.CreateTableIndexStep;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;
import org.springframework.boot.autoconfigure.jooq.JooqAutoConfiguration;
import org.springframework.boot.autoconfigure.jooq.JooqProperties;

import java.sql.Timestamp;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/31 1:37 下午
 */
@JsonTypeName("jdbc")
public class MetricJdbcStorage implements IMetricStorage {

    protected final DSLContext dslContext;
    protected final MetricStorageConfig storageConfig;
    protected final DataSourceSchemaManager schemaManager;

    /**
     * context per data source
     */
    protected final Map<String, DSLContext> dslContextMap;

    private final SqlDialectManager sqlDialectManager;

    @JsonCreator
    public MetricJdbcStorage(@JacksonInject(useInput = OptBoolean.FALSE) JdbcJooqContextHolder dslContextHolder,
                             @JacksonInject(useInput = OptBoolean.FALSE) DataSourceSchemaManager schemaManager,
                             @JacksonInject(useInput = OptBoolean.FALSE) MetricStorageConfig storageConfig,
                             @JacksonInject(useInput = OptBoolean.FALSE) SqlDialectManager sqlDialectManager) {
        this(dslContextHolder.getDslContext(), schemaManager, storageConfig, sqlDialectManager);
    }

    public MetricJdbcStorage(DSLContext dslContext,
                             DataSourceSchemaManager schemaManager,
                             MetricStorageConfig storageConfig,
                             SqlDialectManager sqlDialectManager) {
        this.dslContext = dslContext;
        this.sqlDialectManager = sqlDialectManager;
        this.schemaManager = schemaManager;
        this.storageConfig = storageConfig;
        this.dslContextMap = new ConcurrentHashMap<>();
        schemaManager.addListener((oldSchema, newSchema) -> {
            if (oldSchema != null && !Objects.equals(oldSchema.getDataStoreSpec(), newSchema.getDataStoreSpec())) {
                DSLContext context = dslContextMap.remove(oldSchema.getName());
                if (context != null) {
                    context.close();
                }
            }
        });
    }

    @Override
    public IMetricWriter createMetricWriter(DataSourceSchema schema) {
        MetricTable table = new MetricTable(schema);
        initialize(schema, table);
        return new MetricJdbcWriter(dslContext, table);
    }

    @Override
    public final IMetricReader createMetricReader(DataSourceSchema schema) {
        DSLContext context = this.dslContextMap.computeIfAbsent(schema.getName(), (name) -> {
            IDataStoreSpec dataStoreSpec = schema.getDataStoreSpec();
            if (dataStoreSpec == null || dataStoreSpec.isInternal()) {
                return dslContext;
            }

            //
            // Create a new DSL Context on the external data source
            //
            DruidDataSource jdbcDataSource = new DruidDataSource();
            jdbcDataSource.setDriverClassName(Preconditions.checkNotNull(dataStoreSpec.getProperty("driverClassName"), "Missing driverClassName property for %s", schema.getName()));
            jdbcDataSource.setUrl(Preconditions.checkNotNull(dataStoreSpec.getProperty("url"), "Missing url property for %s", schema.getName()));
            jdbcDataSource.setUsername(Preconditions.checkNotNull(dataStoreSpec.getProperty("username"), "Missing userName property for %s", schema.getName()));
            jdbcDataSource.setPassword(Preconditions.checkNotNull(dataStoreSpec.getProperty("password"), "Missing password property for %s", schema.getName()));
            jdbcDataSource.setName(schema.getName());

            // Create a new one
            JooqAutoConfiguration autoConfiguration = new JooqAutoConfiguration();
            return DSL.using(new DefaultConfiguration()
                                     .set(autoConfiguration.dataSourceConnectionProvider(jdbcDataSource))
                                     .set(new JooqProperties().determineSqlDialect(jdbcDataSource))
                                     .set(autoConfiguration.jooqExceptionTranslatorExecuteListenerProvider()));
        });

        return this.createReader(context, sqlDialectManager.getSqlDialect(context));
    }

    protected IMetricReader createReader(DSLContext dslContext, ISqlDialect sqlDialect) {
        return new MetricJdbcReader(dslContext, sqlDialect);
    }

    protected void initialize(DataSourceSchema schema, MetricTable table) {
        if (!schema.getDataStoreSpec().isInternal()) {
            return;
        }
        CreateTableIndexStep s = dslContext.createTableIfNotExists(table)
                                           .columns(table.fields())
                                           .indexes(table.getIndexes());
        s.execute();
    }

    @Override
    public IExpirationRunnable getExpirationRunnable() {
        return new MetricStorageCleaner() {
            @Override
            public ExpirationConfig getRule() {
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
