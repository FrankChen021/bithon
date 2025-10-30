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
import org.bithon.component.commons.expression.ComparisonExpression;
import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.component.commons.expression.serialization.IdentifierQuotaStrategy;
import org.bithon.component.commons.tracing.SpanKind;
import org.bithon.component.commons.utils.CloseableIterator;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.datasource.query.IDataSourceReader;
import org.bithon.server.datasource.query.Limit;
import org.bithon.server.datasource.query.OrderBy;
import org.bithon.server.datasource.query.Query;
import org.bithon.server.datasource.query.setting.QuerySettings;
import org.bithon.server.datasource.reader.clickhouse.ClickHouseDataSourceReader;
import org.bithon.server.datasource.reader.clickhouse.ClickHouseMetadataManager;
import org.bithon.server.datasource.reader.jdbc.dialect.SqlDialectManager;
import org.bithon.server.datasource.reader.jdbc.statement.ast.OrderByClause;
import org.bithon.server.datasource.reader.jdbc.statement.ast.SelectStatement;
import org.bithon.server.datasource.reader.jdbc.statement.serializer.Expression2Sql;
import org.bithon.server.storage.common.expiration.ExpirationConfig;
import org.bithon.server.storage.common.expiration.IExpirationRunnable;
import org.bithon.server.storage.datasource.SchemaManager;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseConfig;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseStorageProviderConfiguration;
import org.bithon.server.storage.jdbc.clickhouse.common.DataCleaner;
import org.bithon.server.storage.jdbc.clickhouse.common.SecondaryIndex;
import org.bithon.server.storage.jdbc.clickhouse.common.TableCreator;
import org.bithon.server.storage.jdbc.common.jooq.Tables;
import org.bithon.server.storage.jdbc.tracing.TraceJdbcStorage;
import org.bithon.server.storage.jdbc.tracing.reader.IndexedTagQueryBuilder;
import org.bithon.server.storage.jdbc.tracing.reader.TraceJdbcReader;
import org.bithon.server.storage.tracing.ITraceReader;
import org.bithon.server.storage.tracing.ITraceWriter;
import org.bithon.server.storage.tracing.TraceSpan;
import org.bithon.server.storage.tracing.TraceStorageConfig;
import org.bithon.server.storage.tracing.TraceTableSchema;
import org.jooq.Cursor;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.SelectConditionStep;
import org.jooq.Table;
import org.springframework.context.ApplicationContext;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
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
    private final ClickHouseMetadataManager metadataManager;

    @JsonCreator
    public TraceStorage(@JacksonInject(useInput = OptBoolean.FALSE) ClickHouseStorageProviderConfiguration configuration,
                        @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper objectMapper,
                        @JacksonInject(useInput = OptBoolean.FALSE) TraceStorageConfig storageConfig,
                        @JacksonInject(useInput = OptBoolean.FALSE) SqlDialectManager sqlDialectManager,
                        @JacksonInject(useInput = OptBoolean.FALSE) ApplicationContext applicationContext,
                        @JacksonInject(useInput = OptBoolean.FALSE) QuerySettings querySettings) {
        super(configuration.getDslContext(),
              objectMapper,
              storageConfig,
              sqlDialectManager,
              applicationContext,
              querySettings);
        this.clickHouseConfig = configuration.getClickHouseConfig();
        this.metadataManager = new ClickHouseMetadataManager(this.dslContext);
    }

    @Override
    public void initialize() {
        if (!this.storageConfig.isCreateTable()) {
            return;
        }

        getDefaultTableCreator(Tables.BITHON_TRACE_SPAN_SUMMARY, Tables.BITHON_TRACE_SPAN_SUMMARY.STARTTIMEUS)
            .partitionByExpression(StringUtils.format("toYYYYMMDD(%s)", Tables.BITHON_TRACE_SPAN_SUMMARY.STARTTIMEUS.getName()))
            .secondaryIndex(Tables.BITHON_TRACE_SPAN_SUMMARY.NORMALIZEDURL.getName(), new SecondaryIndex("bloom_filter", 1))
            .secondaryIndex(StringUtils.format("mapKeys(%s)", Tables.BITHON_TRACE_SPAN_SUMMARY.ATTRIBUTES.getName()), new SecondaryIndex("bloom_filter", 1, "idx_attr_keys"))
            .secondaryIndex(StringUtils.format("mapValues(%s)", Tables.BITHON_TRACE_SPAN_SUMMARY.ATTRIBUTES.getName()), new SecondaryIndex("bloom_filter", 1, "idx_attr_vals"))
            .createIfNotExist(Tables.BITHON_TRACE_SPAN_SUMMARY);

        getDefaultTableCreator(Tables.BITHON_TRACE_SPAN, Tables.BITHON_TRACE_SPAN.TIMESTAMP)
            .secondaryIndex(Tables.BITHON_TRACE_SPAN.NORMALIZEDURL.getName(), new SecondaryIndex("bloom_filter", 1))
            .secondaryIndex(StringUtils.format("mapKeys(%s)", Tables.BITHON_TRACE_SPAN.ATTRIBUTES.getName()), new SecondaryIndex("bloom_filter", 1, "idx_attr_keys"))
            .secondaryIndex(StringUtils.format("mapValues(%s)", Tables.BITHON_TRACE_SPAN.ATTRIBUTES.getName()), new SecondaryIndex("bloom_filter", 1, "idx_attr_vals"))
            .createIfNotExist(Tables.BITHON_TRACE_SPAN);

        getDefaultTableCreator(Tables.BITHON_TRACE_MAPPING, Tables.BITHON_TRACE_MAPPING.TIMESTAMP)
            .createIfNotExist(Tables.BITHON_TRACE_MAPPING);

        getDefaultTableCreator(Tables.BITHON_TRACE_SPAN_TAG_INDEX, Tables.BITHON_TRACE_SPAN_TAG_INDEX.TIMESTAMP)
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
                     StringUtils.format("""
                                            SELECT  appName,
                                                    instanceName,
                                                    name,
                                                    clazz,
                                                    method,
                                                    traceId,
                                                    spanId,
                                                    parentSpanId,
                                                    kind,
                                                    costTimeUs,
                                                    fromUnixTimestamp64Micro(startTimeUs) AS startTimeUs,
                                                    endTimeUs,
                                                    tags,
                                                    attributes,
                                                    normalizedUrl,
                                                    status
                                            FROM %s.%s
                                            """, this.clickHouseConfig.getDatabase(), this.clickHouseConfig.getLocalTableName(Tables.BITHON_TRACE_SPAN.getName())) +
                     // See SpanKind.isRootSpan
                     StringUtils.format("WHERE kind in ('%s')",
                                        String.join("', '",
                                                    SpanKind.TIMER.name(),
                                                    SpanKind.SERVER.name(),
                                                    SpanKind.CONSUMER.name()));

        this.dslContext.execute(ddl);
    }

    private TableCreator getDefaultTableCreator(Table<?> table, Field<?> timestampColumn) {
        TableCreator tableCreator = new TableCreator(clickHouseConfig, dslContext);

        ClickHouseConfig.SecondaryPartition partition = clickHouseConfig.getSecondaryPartitions().get(table.getName());
        if (partition != null) {
            tableCreator.partitionByExpression(StringUtils.format("(toYYYYMMDD(%s), cityHash64(%s) %% %d)",
                                                                  timestampColumn.getName(),
                                                                  partition.getColumn(),
                                                                  partition.getCount()));
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
                                   this.applicationContext.getBean(SchemaManager.class).getSchema(TraceTableSchema.TRACE_SPAN_SUMMARY_SCHEMA_NAME),
                                   this.applicationContext.getBean(SchemaManager.class).getSchema(TraceTableSchema.TRACE_SPAN_SCHEMA_NAME),
                                   this.applicationContext.getBean(SchemaManager.class).getSchema(TraceTableSchema.TRACE_SPAN_TAG_INDEX_SCHEMA_NAME),
                                   this.storageConfig,
                                   this.sqlDialectManager.getSqlDialect(this.dslContext),
                                   this.querySettings) {

            @Override
            protected Map<String, String> toTagMap(Object attributes) {
                //noinspection unchecked
                return (Map<String, String>) attributes;
            }

            @Override
            protected IDataSourceReader getDataSourceReader() {
                return new ClickHouseDataSourceReader(this.dslContext, this.sqlDialect, this.querySettings);
            }

            /**
             * Override to apply read in order optimization
             */
            @Override
            public CloseableIterator<TraceSpan> getTraceList(IExpression filter,
                                                             List<IExpression> indexedTagFilter,
                                                             Timestamp start,
                                                             Timestamp end,
                                                             OrderBy orderBy,
                                                             Limit limit) {
                boolean isOnSummaryTable = RootSpanKindFilterAnalyzer.isOnRootSpanOnly(filter);

                Field<LocalDateTime> timestampField = isOnSummaryTable ? Tables.BITHON_TRACE_SPAN_SUMMARY.STARTTIMEUS : Tables.BITHON_TRACE_SPAN.TIMESTAMP;

                IdentifierExpression tsColumn = new IdentifierExpression(timestampField.getName());
                IExpression tsExpression = new LogicalExpression.AND(new ComparisonExpression.GTE(tsColumn, sqlDialect.toISO8601TimestampExpression(start)),
                                                                     new ComparisonExpression.LT(tsColumn, sqlDialect.toISO8601TimestampExpression(end)));

                // NOTE:
                // 1. Here use selectFrom(String) instead of use selectFrom(table) because we want to use the raw objects returned by underlying JDBC
                // 2. If the filters contain a filter that matches the ROOT kind, then the search is built upon the summary table
                SelectConditionStep<Record> listQuery = dslContext.selectFrom(isOnSummaryTable ? Tables.BITHON_TRACE_SPAN_SUMMARY.getUnqualifiedName().quotedName() : Tables.BITHON_TRACE_SPAN.getUnqualifiedName().quotedName())
                                                                  .where(sqlDialect.createSqlSerializer(null).serialize(tsExpression));

                if (filter != null) {
                    listQuery = listQuery.and(Expression2Sql.from((isOnSummaryTable ? Tables.BITHON_TRACE_SPAN_SUMMARY : Tables.BITHON_TRACE_SPAN).getName(),
                                                                  sqlDialect,
                                                                  filter));
                }

                // Build the tag query
                if (CollectionUtils.isNotEmpty(indexedTagFilter)) {
                    SelectConditionStep<Record1<String>> indexedTagQuery = new IndexedTagQueryBuilder(this.sqlDialect)
                        .dslContext(this.dslContext)
                        .start(start.toLocalDateTime())
                        .end(end.toLocalDateTime())
                        .build(indexedTagFilter);

                    if (isOnSummaryTable) {
                        listQuery = listQuery.and(Tables.BITHON_TRACE_SPAN_SUMMARY.TRACEID.in(indexedTagQuery));
                    } else {
                        listQuery = listQuery.and(Tables.BITHON_TRACE_SPAN.TRACEID.in(indexedTagQuery));
                    }
                }

                StringBuilder sqlTextBuilder = new StringBuilder(dslContext.renderInlined(listQuery));
                sqlTextBuilder.append(" ORDER BY ");

                Field<?> orderField;
                if ("costTime".equals(orderBy.getName())) {
                    orderField = isOnSummaryTable ? Tables.BITHON_TRACE_SPAN_SUMMARY.COSTTIMEUS : Tables.BITHON_TRACE_SPAN.COSTTIMEUS;
                } else if ("startTimeUs".equals(orderBy.getName())) {
                    orderField = isOnSummaryTable ? Tables.BITHON_TRACE_SPAN_SUMMARY.STARTTIMEUS : Tables.BITHON_TRACE_SPAN.COSTTIMEUS;
                } else {
                    orderField = Arrays.stream((isOnSummaryTable ? Tables.BITHON_TRACE_SPAN_SUMMARY : Tables.BITHON_TRACE_SPAN).fields())
                                       .filter((f) -> f.getName().equals(orderBy.getName()))
                                       .findFirst()
                                       .orElse(isOnSummaryTable ? Tables.BITHON_TRACE_SPAN_SUMMARY.STARTTIMEUS : Tables.BITHON_TRACE_SPAN.TIMESTAMP);
                }

                sqlTextBuilder.append(orderField.getName());
                sqlTextBuilder.append(' ');
                sqlTextBuilder.append(orderBy.getOrder().name());

                sqlTextBuilder.append(" LIMIT ").append(limit.getLimit());
                sqlTextBuilder.append(" OFFSET ").append(limit.getOffset());

                String sql = decorateSQL(sqlTextBuilder.toString());

                log.info("Get trace list: {}", sql);

                Cursor<?> cursor = dslContext.fetchLazy(sql);
                return CloseableIterator.transform(cursor.iterator(),
                                                   this::toTraceSpan,
                                                   cursor);
            }

            /**
             * Override to apply read in order optimization
             */
            @Override
            protected SelectStatement toSelectStatement(Query query) {
                SelectStatement selectStatement = super.toSelectStatement(query);

                // Apply read-in-order optimization only when the order by is on timestamp column
                List<OrderByClause> orderByClauses = selectStatement.getOrderBy();
                if (orderByClauses.size() == 1) {
                    OrderByClause orderByClause = orderByClauses.get(0);
                    if (orderByClause.getField().equals(query.getSchema().getTimestampSpec().getColumnName())) {
                        applyReadInOrderOptimization(TraceTableSchema.TRACE_SPAN_SUMMARY_SCHEMA_NAME.equals(query.getSchema().getName()),
                                                     new OrderBy(orderByClause.getField(), orderByClause.getOrder()),
                                                     this.traceSpanSummarySchema.getTimestampSpec().getColumnName(),
                                                     orderByClauses);
                    }
                }

                return selectStatement;
            }

            @Override
            protected String decorateSQL(String sql) {
                return sql + " SETTINGS distributed_product_mode = 'global'";
            }
        };
    }

    private void applyReadInOrderOptimization(boolean isOnSummaryTable,
                                              OrderBy orderBy,
                                              String tsColumn,
                                              List<OrderByClause> orderByClauses) {
        if (!querySettings.isEnableReadInOrderOptimization()) {
            return;
        }

        // Use getBean because ClickHouseMetadataManager is not constructed by this storage
        // Fundamentally, this is the dependency issue of current design
        List<IExpression> orderByExpressions = metadataManager.getOrderByExpression(isOnSummaryTable ? Tables.BITHON_TRACE_SPAN_SUMMARY.getName() : Tables.BITHON_TRACE_SPAN.getName());
        for (IExpression orderByExpression : orderByExpressions) {
            if (!(orderByExpression instanceof FunctionExpression functionExpression)) {
                continue;
            }
            boolean hasTimestampColumn = functionExpression.getArgs()
                                                           .stream()
                                                           .anyMatch((expr) -> (expr instanceof IdentifierExpression) && ((IdentifierExpression) expr).getIdentifier().equals(tsColumn));
            if (hasTimestampColumn) {
                orderByClauses.add(0, new OrderByClause(functionExpression.serializeToText(IdentifierQuotaStrategy.NONE), orderBy.getOrder()));
                break;
            }
        }
    }
}
