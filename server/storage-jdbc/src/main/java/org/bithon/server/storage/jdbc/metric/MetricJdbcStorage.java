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
import com.fasterxml.jackson.annotation.OptBoolean;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.datasource.ISchema;
import org.bithon.server.datasource.query.IDataSourceReader;
import org.bithon.server.datasource.query.setting.QuerySettings;
import org.bithon.server.datasource.reader.jdbc.JdbcDataSourceReader;
import org.bithon.server.datasource.reader.jdbc.dialect.ISqlDialect;
import org.bithon.server.datasource.reader.jdbc.dialect.SqlDialectManager;
import org.bithon.server.storage.common.expiration.IExpirationRunnable;
import org.bithon.server.storage.datasource.SchemaManager;
import org.bithon.server.storage.jdbc.JdbcStorageProviderConfiguration;
import org.bithon.server.storage.jdbc.common.jooq.Tables;
import org.bithon.server.storage.metrics.IMetricStorage;
import org.bithon.server.storage.metrics.IMetricWriter;
import org.bithon.server.storage.metrics.MetricStorageConfig;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/31 1:37 下午
 */
public class MetricJdbcStorage implements IMetricStorage {

    protected final DSLContext dslContext;
    protected final MetricStorageConfig storageConfig;
    protected final SchemaManager schemaManager;
    protected final ISqlDialect sqlDialect;
    protected final QuerySettings querySettings;
    private final Map<String, Boolean> schemaInitialized = new HashMap<>();

    @JsonCreator
    public MetricJdbcStorage(@JacksonInject(useInput = OptBoolean.FALSE) JdbcStorageProviderConfiguration providerConfiguration,
                             @JacksonInject(useInput = OptBoolean.FALSE) SchemaManager schemaManager,
                             @JacksonInject(useInput = OptBoolean.FALSE) MetricStorageConfig storageConfig,
                             @JacksonInject(useInput = OptBoolean.FALSE) SqlDialectManager sqlDialectManager,
                             @JacksonInject(useInput = OptBoolean.FALSE) QuerySettings querySettings) {
        this(providerConfiguration.getDslContext(), schemaManager, storageConfig, sqlDialectManager, querySettings);
    }

    public MetricJdbcStorage(DSLContext dslContext,
                             SchemaManager schemaManager,
                             MetricStorageConfig storageConfig,
                             SqlDialectManager sqlDialectManager,
                             QuerySettings querySettings) {
        this.dslContext = dslContext;
        this.sqlDialect = sqlDialectManager.getSqlDialect(dslContext);
        this.schemaManager = schemaManager;
        this.storageConfig = storageConfig;
        this.querySettings = querySettings;
    }

    @Override
    public final IMetricWriter createMetricWriter(ISchema schema) {
        MetricTable table = toMetricTable(schema);

        initializeMetricTableIfNecessary(schema, table);

        return createWriter(dslContext, table);
    }

    @Override
    public final IDataSourceReader createMetricReader(ISchema schema) {
        initializeMetricTableIfNecessary(schema, toMetricTable(schema));

        return this.createReader(this.dslContext, this.sqlDialect, this.querySettings);
    }

    private void initializeMetricTableIfNecessary(ISchema schema, MetricTable table) {
        if (!schema.getDataStoreSpec().isInternal()) {
            return;
        }

        boolean initialized = schemaInitialized.getOrDefault(schema.getName(), false);
        if (initialized) {
            return;
        }

        synchronized (schemaInitialized) {
            initialized = schemaInitialized.getOrDefault(schema.getName(), false);
            if (initialized) {
                return;
            }
            this.initialize(schema, table);
            schemaInitialized.put(schema.getName(), true);
        }
    }

    @Override
    public final List<String> getBaselineDates() {
        return getBaselineRecords().stream()
                                   .map((record) -> {
                                       try {
                                           String date = record.get(Tables.BITHON_METRICS_BASELINE.DATE);
                                           TimeSpan startTimestamp = TimeSpan.fromMilliseconds(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).parse(date + " 00:00:00").getTime());

                                           int keepDays = record.get(Tables.BITHON_METRICS_BASELINE.KEEP_DAYS);
                                           if (keepDays > 0) {
                                               if (startTimestamp.after(keepDays, TimeUnit.DAYS).getMilliseconds() > System.currentTimeMillis()) {
                                                   return startTimestamp.format("yyyy-MM-dd");
                                               } else {
                                                   // Will be ignored
                                                   return null;
                                               }
                                           } else {
                                               return startTimestamp.format("yyyy-MM-dd");
                                           }
                                       } catch (ParseException e) {
                                           return null;
                                       }
                                   })
                                   .filter(Objects::nonNull)
                                   .sorted()
                                   .collect(Collectors.toList());
    }

    @Override
    public void saveBaseline(String date, int keepDays) {
        LocalDateTime now = new Timestamp(System.currentTimeMillis()).toLocalDateTime();
        dslContext.insertInto(Tables.BITHON_METRICS_BASELINE)
                  .set(Tables.BITHON_METRICS_BASELINE.DATE, date)
                  .set(Tables.BITHON_METRICS_BASELINE.KEEP_DAYS, keepDays)
                  .set(Tables.BITHON_METRICS_BASELINE.CREATE_TIME, now)
                  .onDuplicateKeyUpdate()
                  .set(Tables.BITHON_METRICS_BASELINE.DATE, date)
                  .set(Tables.BITHON_METRICS_BASELINE.KEEP_DAYS, keepDays)
                  .set(Tables.BITHON_METRICS_BASELINE.CREATE_TIME, now)
                  .execute();
    }

    protected MetricTable toMetricTable(ISchema schema) {
        return new MetricTable(schema, false);
    }

    protected IMetricWriter createWriter(DSLContext dslContext, MetricTable table) {
        return new MetricJdbcWriter(dslContext, table, true, null);
    }

    protected IDataSourceReader createReader(DSLContext dslContext, ISqlDialect sqlDialect, QuerySettings querySettings) {
        return new JdbcDataSourceReader(dslContext, sqlDialect, querySettings);
    }

    protected void initialize(ISchema dataSource, MetricTable table) {
        if (!this.storageConfig.isCreateTable()) {
            return;
        }

        dslContext.createTableIfNotExists(table)
                  .columns(table.fields())
                  .indexes(table.getIndexes())
                  .execute();
    }

    @Override
    public IExpirationRunnable getExpirationRunnable() {
        return new MetricJdbcStorageCleaner(dslContext, schemaManager, this.storageConfig.getTtl(), this.sqlDialect);
    }

    @Override
    public void initialize() {
        this.dslContext.createTableIfNotExists(Tables.BITHON_METRICS_BASELINE)
                       .columns(Tables.BITHON_METRICS_BASELINE.fields())
                       .indexes(Tables.BITHON_METRICS_BASELINE.getIndexes())
                       .execute();
    }

    protected Result<? extends Record> getBaselineRecords() {
        return dslContext.selectFrom(Tables.BITHON_METRICS_BASELINE)
                         .fetch();
    }
}
