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

package org.bithon.server.storage.jdbc.clickhouse.storage;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.common.ExpirationConfig;
import org.bithon.server.storage.common.IExpirationRunnable;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.DataSourceSchemaManager;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseConfig;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseStorageConfiguration;
import org.bithon.server.storage.jdbc.jooq.Tables;
import org.bithon.server.storage.jdbc.metric.MetricJdbcReader;
import org.bithon.server.storage.jdbc.metric.MetricJdbcStorage;
import org.bithon.server.storage.jdbc.metric.MetricJdbcWriter;
import org.bithon.server.storage.jdbc.metric.MetricTable;
import org.bithon.server.storage.jdbc.utils.Expression2Sql;
import org.bithon.server.storage.jdbc.utils.ISqlDialect;
import org.bithon.server.storage.jdbc.utils.SqlDialectManager;
import org.bithon.server.storage.metrics.IMetricReader;
import org.bithon.server.storage.metrics.IMetricWriter;
import org.bithon.server.storage.metrics.MetricStorageConfig;
import org.bithon.server.storage.metrics.ttl.MetricStorageCleaner;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/31 1:37 下午
 */
@Slf4j
@JsonTypeName("clickhouse")
public class MetricStorage extends MetricJdbcStorage {

    private final ClickHouseConfig config;

    @JsonCreator
    public MetricStorage(@JacksonInject(useInput = OptBoolean.FALSE) ClickHouseStorageConfiguration storageConfiguration,
                         @JacksonInject(useInput = OptBoolean.FALSE) DataSourceSchemaManager schemaManager,
                         @JacksonInject(useInput = OptBoolean.FALSE) MetricStorageConfig storageConfig,
                         @JacksonInject(useInput = OptBoolean.FALSE) SqlDialectManager sqlDialectManager) {
        super(storageConfiguration.getDslContext(), schemaManager, storageConfig, sqlDialectManager);
        this.config = storageConfiguration.getClickHouseConfig();
    }

    @Override
    protected void initialize(DataSourceSchema schema, MetricTable table) {
        if (schema.getDataStoreSpec().isInternal()) {
            new TableCreator(config, this.dslContext).createIfNotExist(table);
        }
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
            protected List<TimeSpan> getSkipDateList() {
                String sql = dslContext.select(Tables.BITHON_METRICS_BASELINE.DATE, Tables.BITHON_METRICS_BASELINE.KEEP_DAYS)
                                       .from(Tables.BITHON_METRICS_BASELINE)
                                       .getSQL() + " FINAL ";

                return dslContext.fetch(sql)
                                 .stream()
                                 .map((record) -> {
                                     try {
                                         String date = record.get(Tables.BITHON_METRICS_BASELINE.DATE);

                                         TimeSpan startTimestamp = TimeSpan.of(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(date + " 00:00:00").getTime());
                                         int keepDays = record.get(Tables.BITHON_METRICS_BASELINE.KEEP_DAYS);
                                         if (keepDays > 0) {
                                             if (startTimestamp.after(keepDays, TimeUnit.DAYS).getMilliseconds() > System.currentTimeMillis()) {
                                                 return startTimestamp;
                                             } else {
                                                 // Will be ignored
                                                 return null;
                                             }
                                         } else {
                                             return startTimestamp;
                                         }
                                     } catch (ParseException e) {
                                         return null;
                                     }
                                 }).filter(Objects::nonNull)
                                 .collect(Collectors.toList());
            }

            @Override
            protected void expireImpl(DataSourceSchema schema, Timestamp before, List<TimeSpan> skipDateList) {
                String table = schema.getDataStoreSpec().getStore();
                new DataCleaner(config, dslContext).deleteFromPartition(table, before, skipDateList);
            }
        };
    }

    @Override
    protected IMetricWriter createWriter(DSLContext dslContext, MetricTable table) {
        return new MetricJdbcWriter(dslContext, table) {
            /**
             * No length constraint in ClickHouse
             */
            @Override
            protected String getOrTruncateDimension(Field<?> dimensionField, String value) {
                return value;
            }
        };
    }

    @Override
    protected IMetricReader createReader(DSLContext dslContext, ISqlDialect sqlDialect) {
        return new MetricJdbcReader(dslContext, sqlDialect) {
            /**
             * Rewrite the SQL to use group-by instead of distinct so that we can leverage PROJECTIONS defined at the underlying table to speed up queries
             */
            @Override
            public List<Map<String, String>> getDistinctValues(TimeSpan start,
                                                               TimeSpan end,
                                                               DataSourceSchema dataSourceSchema,
                                                               IExpression filter,
                                                               String dimension) {
                start = start.floor(Duration.ofMinutes(1));
                end = end.ceil(Duration.ofMinutes(1));

                String condition = filter == null ? "" : Expression2Sql.from(dataSourceSchema, sqlDialect, filter) + " AND ";

                String sql = StringUtils.format(
                    "SELECT \"%s\" FROM \"%s\" WHERE %s toStartOfMinute(\"timestamp\") >= %s AND toStartOfMinute(\"timestamp\") < %s GROUP BY \"%s\" ORDER BY \"%s\"",
                    dimension,
                    dataSourceSchema.getDataStoreSpec().getStore(),
                    condition,
                    sqlDialect.formatTimestamp(start),
                    sqlDialect.formatTimestamp(end),
                    dimension,
                    dimension);

                log.info("Executing {}", sql);
                List<Record> records = dsl.fetch(sql);
                return records.stream().map(record -> {
                    Map<String, String> mapObject = new HashMap<>();
                    mapObject.put("value", record.get(0).toString());
                    return mapObject;
                }).collect(Collectors.toList());
            }
        };
    }

    @Override
    public void initialize() {
        new TableCreator(config, this.dslContext).useReplacingMergeTree(Tables.BITHON_METRICS_BASELINE.CREATE_TIME.getName())
                                                 // No partition for this table
                                                 .partitionByExpression(null)
                                                 // Add minmax index to timestamp column
                                                 .createIfNotExist(Tables.BITHON_METRICS_BASELINE);
    }
}
