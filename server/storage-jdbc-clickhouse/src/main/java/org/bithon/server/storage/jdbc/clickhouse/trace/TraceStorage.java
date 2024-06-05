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

package org.bithon.server.storage.jdbc.clickhouse.trace;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.tracing.SpanKind;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.common.expiration.ExpirationConfig;
import org.bithon.server.storage.common.expiration.IExpirationRunnable;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseConfig;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseStorageProviderConfiguration;
import org.bithon.server.storage.jdbc.clickhouse.common.DataCleaner;
import org.bithon.server.storage.jdbc.clickhouse.common.SecondaryIndex;
import org.bithon.server.storage.jdbc.clickhouse.common.TableCreator;
import org.bithon.server.storage.jdbc.common.dialect.SqlDialectManager;
import org.bithon.server.storage.jdbc.common.jooq.Tables;
import org.bithon.server.storage.jdbc.tracing.TraceJdbcStorage;
import org.bithon.server.storage.jdbc.tracing.reader.TraceJdbcReader;
import org.bithon.server.storage.tracing.ITraceReader;
import org.bithon.server.storage.tracing.ITraceWriter;
import org.bithon.server.storage.tracing.TraceStorageConfig;
import org.jooq.Table;

import java.sql.Timestamp;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/10/27 9:34 下午
 */
@Slf4j
@JsonTypeName("clickhouse")
public class TraceStorage extends TraceJdbcStorage {

    @Getter
    private final ClickHouseConfig clickHouseConfig;

    @JsonCreator
    public TraceStorage(@JacksonInject(useInput = OptBoolean.FALSE) ClickHouseStorageProviderConfiguration configuration,
                        @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper objectMapper,
                        @JacksonInject(useInput = OptBoolean.FALSE) TraceStorageConfig storageConfig,
                        @JacksonInject(useInput = OptBoolean.FALSE) SqlDialectManager sqlDialectManager) {
        super(configuration.getDslContext(),
              objectMapper,
              storageConfig,
              sqlDialectManager);
        this.clickHouseConfig = configuration.getClickHouseConfig();
    }

    @Override
    public void initialize() {
        if (!this.storageConfig.isCreateTable()) {
            return;
        }


        getDefaultTableCreator(Tables.BITHON_TRACE_SPAN_SUMMARY)
            .secondaryIndex(Tables.BITHON_TRACE_SPAN_SUMMARY.NORMALIZEDURL.getName(), new SecondaryIndex("bloom_filter", 1))
            .secondaryIndex(StringUtils.format("mapKeys(%s)", Tables.BITHON_TRACE_SPAN_SUMMARY.ATTRIBUTES.getName()), new SecondaryIndex("bloom_filter", 1, "idx_attr_keys"))
            .secondaryIndex(StringUtils.format("mapValues(%s)", Tables.BITHON_TRACE_SPAN_SUMMARY.ATTRIBUTES.getName()), new SecondaryIndex("bloom_filter", 1, "idx_attr_vals"))
            .createIfNotExist(Tables.BITHON_TRACE_SPAN_SUMMARY);

        getDefaultTableCreator(Tables.BITHON_TRACE_SPAN)
            .secondaryIndex(Tables.BITHON_TRACE_SPAN.NORMALIZEDURL.getName(), new SecondaryIndex("bloom_filter", 1))
            .secondaryIndex(StringUtils.format("mapKeys(%s)", Tables.BITHON_TRACE_SPAN.ATTRIBUTES.getName()), new SecondaryIndex("bloom_filter", 1, "idx_attr_keys"))
            .secondaryIndex(StringUtils.format("mapValues(%s)", Tables.BITHON_TRACE_SPAN.ATTRIBUTES.getName()), new SecondaryIndex("bloom_filter", 1, "idx_attr_vals"))
            .createIfNotExist(Tables.BITHON_TRACE_SPAN);

        getDefaultTableCreator(Tables.BITHON_TRACE_MAPPING)
            .createIfNotExist(Tables.BITHON_TRACE_MAPPING);

        getDefaultTableCreator(Tables.BITHON_TRACE_SPAN_TAG_INDEX)
            .secondaryIndex(Tables.BITHON_TRACE_SPAN_TAG_INDEX.F1.getName(), new SecondaryIndex("bloom_filter", 1))
            .secondaryIndex(Tables.BITHON_TRACE_SPAN_TAG_INDEX.F2.getName(), new SecondaryIndex("bloom_filter", 1))
            .secondaryIndex(Tables.BITHON_TRACE_SPAN_TAG_INDEX.F3.getName(), new SecondaryIndex("bloom_filter", 1))
            .secondaryIndex(Tables.BITHON_TRACE_SPAN_TAG_INDEX.F4.getName(), new SecondaryIndex("bloom_filter", 1))
            .secondaryIndex(Tables.BITHON_TRACE_SPAN_TAG_INDEX.F5.getName(), new SecondaryIndex("bloom_filter", 1))
            .createIfNotExist(Tables.BITHON_TRACE_SPAN_TAG_INDEX);

        createMaterializedView();
    }

    private void createMaterializedView() {
        String ddl = StringUtils.format("CREATE MATERIALIZED VIEW IF NOT EXISTS %s.%s %s TO %s.%s AS\n",
                                        this.clickHouseConfig.getDatabase(),
                                        Tables.BITHON_TRACE_SPAN_SUMMARY.getName() + "_mv",
                                        this.clickHouseConfig.getOnClusterExpression(),
                                        this.clickHouseConfig.getDatabase(),
                                        this.clickHouseConfig.getLocalTableName(Tables.BITHON_TRACE_SPAN_SUMMARY.getName())) +
                     StringUtils.format("SELECT * FROM %s.%s\n", this.clickHouseConfig.getDatabase(), this.clickHouseConfig.getLocalTableName(Tables.BITHON_TRACE_SPAN.getName())) +
                     // See SpanKind.isRootSpan
                     StringUtils.format("WHERE kind in ('%s')",
                                        String.join("', '",
                                                    SpanKind.TIMER.name(),
                                                    SpanKind.SERVER.name(),
                                                    SpanKind.CONSUMER.name()));

        this.dslContext.execute(ddl);
    }

    private TableCreator getDefaultTableCreator(Table<?> table) {
        TableCreator tableCreator = new TableCreator(clickHouseConfig, dslContext);

        ClickHouseConfig.SecondaryPartition partition = clickHouseConfig.getSecondaryPartitions().get(table.getName());
        if (partition != null) {
            tableCreator.partitionByExpression(StringUtils.format("(toYYYYMMDD(timestamp), cityHash64(%s) %% %d)", partition.getColumn(), partition.getCount()));
        }

        return tableCreator;
    }

    @Override
    public IExpirationRunnable getExpirationRunnable() {
        return new IExpirationRunnable() {
            @Override
            public ExpirationConfig getExpirationConfig() {
                return storageConfig.getTtl();
            }

            @Override
            public void expire(Timestamp before) {
                DataCleaner cleaner = new DataCleaner(clickHouseConfig, dslContext);
                cleaner.deletePartition(Tables.BITHON_TRACE_SPAN.getName(), before);
                cleaner.deletePartition(Tables.BITHON_TRACE_SPAN_SUMMARY.getName(), before);
                cleaner.deletePartition(Tables.BITHON_TRACE_MAPPING.getName(), before);
                cleaner.deletePartition(Tables.BITHON_TRACE_SPAN_TAG_INDEX.getName(), before);
            }
        };
    }

    @Override
    public ITraceWriter createWriter() {
        if (this.clickHouseConfig.isOnDistributedTable()) {
            return new LoadBalancedTraceWriter(this.clickHouseConfig, this.storageConfig, this.dslContext);
        } else {
            return new TraceWriter(this.storageConfig, this.dslContext);
        }
    }

    @Override
    public ITraceReader createReader() {
        return new TraceJdbcReader(this.dslContext,
                                   this.objectMapper,
                                   this.traceSpanSchema,
                                   this.traceTagIndexSchema,
                                   this.storageConfig,
                                   this.sqlDialectManager.getSqlDialect(this.dslContext)) {

            @Override
            protected Map<String, String> toTagMap(Object attributes) {
                //noinspection unchecked
                return (Map<String, String>) attributes;
            }

            @Override
            protected String getSQL(String sql) {
                return sql + " SETTINGS distributed_product_mode = 'global'";
            }
        };
    }

}
