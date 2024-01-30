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

package org.bithon.server.storage.jdbc.clickhouse.metric;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import lombok.extern.slf4j.Slf4j;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.common.expiration.ExpirationConfig;
import org.bithon.server.storage.common.expiration.IExpirationRunnable;
import org.bithon.server.storage.datasource.ISchema;
import org.bithon.server.storage.datasource.SchemaManager;
import org.bithon.server.storage.datasource.query.IDataSourceReader;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseConfig;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseStorageProviderConfiguration;
import org.bithon.server.storage.jdbc.clickhouse.common.DataCleaner;
import org.bithon.server.storage.jdbc.clickhouse.common.TableCreator;
import org.bithon.server.storage.jdbc.clickhouse.common.exception.RetryableExceptions;
import org.bithon.server.storage.jdbc.common.dialect.ISqlDialect;
import org.bithon.server.storage.jdbc.common.dialect.SqlDialectManager;
import org.bithon.server.storage.jdbc.common.jooq.Tables;
import org.bithon.server.storage.jdbc.metric.MetricJdbcStorage;
import org.bithon.server.storage.jdbc.metric.MetricJdbcStorageCleaner;
import org.bithon.server.storage.jdbc.metric.MetricJdbcWriter;
import org.bithon.server.storage.jdbc.metric.MetricTable;
import org.bithon.server.storage.metrics.IMetricWriter;
import org.bithon.server.storage.metrics.MetricStorageConfig;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/31 1:37 下午
 */
@Slf4j
@JsonTypeName("clickhouse")
public class MetricStorage extends MetricJdbcStorage {

    private final ClickHouseConfig config;

    @JsonCreator
    public MetricStorage(@JacksonInject(useInput = OptBoolean.FALSE) ClickHouseStorageProviderConfiguration providerConfiguration,
                         @JacksonInject(useInput = OptBoolean.FALSE) SchemaManager schemaManager,
                         @JacksonInject(useInput = OptBoolean.FALSE) MetricStorageConfig storageConfig,
                         @JacksonInject(useInput = OptBoolean.FALSE) SqlDialectManager sqlDialectManager) {
        super(providerConfiguration.getDslContext(), schemaManager, storageConfig, sqlDialectManager);
        this.config = providerConfiguration.getClickHouseConfig();
    }

    @Override
    protected void initialize(ISchema dataSource, MetricTable table) {
        if (!this.storageConfig.isCreateTable()) {
            return;
        }
        new TableCreator(config, this.dslContext).createIfNotExist(table);
    }

    @Override
    protected MetricTable toMetricTable(ISchema schema) {
        return new MetricTable(schema, true);
    }

    @Override
    public IExpirationRunnable getExpirationRunnable() {
        return new StorageCleaner(dslContext, schemaManager, this.storageConfig.getTtl(), config, this.sqlDialect);
    }

    static class StorageCleaner extends MetricJdbcStorageCleaner {
        private final ClickHouseConfig config;

        protected StorageCleaner(DSLContext dslContext,
                                 SchemaManager schemaManager,
                                 ExpirationConfig ttlConfig,
                                 ClickHouseConfig config,
                                 ISqlDialect sqlDialect) {
            super(dslContext, schemaManager, ttlConfig, sqlDialect);
            this.config = config;
        }

        @Override
        protected Result<? extends Record> getSkipDateRecordList() {
            String sql = dslContext.select(Tables.BITHON_METRICS_BASELINE.DATE, Tables.BITHON_METRICS_BASELINE.KEEP_DAYS)
                                   .from(Tables.BITHON_METRICS_BASELINE)
                                   .getSQL() + " FINAL ";
            return dslContext.fetch(sql);
        }

        @Override
        protected void expireImpl(ISchema schema, Timestamp before, List<TimeSpan> skipDateList) {
            String table = schema.getDataStoreSpec().getStore();
            new DataCleaner(config, dslContext).deleteFromPartition(table, before, skipDateList);
        }
    }

    @Override
    protected IMetricWriter createWriter(DSLContext dslContext, MetricTable table) {
        if (this.config.isOnDistributedTable()) {
            return new LoadBalancedMetricWriter(this.dslContext, this.config, table);
        } else {
            return new MetricJdbcWriter(dslContext, table, false, RetryableExceptions::isExceptionRetryable);
        }
    }

    @Override
    protected IDataSourceReader createReader(DSLContext dslContext, ISqlDialect sqlDialect) {
        return new JdbcReader(dslContext, sqlDialect);
    }

    @Override
    public void initialize() {
        new TableCreator(config, this.dslContext).useReplacingMergeTree(Tables.BITHON_METRICS_BASELINE.CREATE_TIME.getName())
                                                 // No partition for this table
                                                 .partitionByExpression(null)
                                                 // Add minmax index to timestamp column
                                                 .createIfNotExist(Tables.BITHON_METRICS_BASELINE);
    }

    @Override
    protected Result<? extends Record> getBaselineRecords() {
        String sql = dslContext.select(Tables.BITHON_METRICS_BASELINE.DATE, Tables.BITHON_METRICS_BASELINE.KEEP_DAYS)
                               .from(Tables.BITHON_METRICS_BASELINE)
                               .getSQL() + " FINAL ";
        return dslContext.fetch(sql);
    }

    @Override
    public void saveBaseline(String date, int keepDays) {
        LocalDateTime now = new Timestamp(System.currentTimeMillis()).toLocalDateTime();
        dslContext.insertInto(Tables.BITHON_METRICS_BASELINE)
                  .set(Tables.BITHON_METRICS_BASELINE.DATE, date)
                  .set(Tables.BITHON_METRICS_BASELINE.KEEP_DAYS, keepDays)
                  .set(Tables.BITHON_METRICS_BASELINE.CREATE_TIME, now)
                  .execute();
    }

}
