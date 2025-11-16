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

package org.bithon.server.storage.jdbc.tracing.reader;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.expression.ComparisonExpression;
import org.bithon.component.commons.expression.ConditionalExpression;
import org.bithon.component.commons.expression.ExpressionList;
import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.expression.IEvaluationContext;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IExpressionInDepthVisitor;
import org.bithon.component.commons.expression.IExpressionVisitor;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.component.commons.expression.function.builtin.AggregateFunction;
import org.bithon.component.commons.expression.optimzer.ExpressionOptimizer;
import org.bithon.component.commons.expression.serialization.ExpressionSerializer;
import org.bithon.component.commons.tracing.SpanKind;
import org.bithon.component.commons.utils.CloseableIterator;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.datasource.ISchema;
import org.bithon.server.datasource.query.DataRow;
import org.bithon.server.datasource.query.IDataSourceReader;
import org.bithon.server.datasource.query.Interval;
import org.bithon.server.datasource.query.Limit;
import org.bithon.server.datasource.query.Order;
import org.bithon.server.datasource.query.OrderBy;
import org.bithon.server.datasource.query.Query;
import org.bithon.server.datasource.query.ReadResponse;
import org.bithon.server.datasource.query.ResultFormat;
import org.bithon.server.datasource.query.ast.Column;
import org.bithon.server.datasource.query.ast.ExpressionNode;
import org.bithon.server.datasource.query.ast.Selector;
import org.bithon.server.datasource.query.pipeline.ColumnarTable;
import org.bithon.server.datasource.query.pipeline.IQueryStep;
import org.bithon.server.datasource.query.setting.QuerySettings;
import org.bithon.server.datasource.reader.jdbc.JdbcDataSourceReader;
import org.bithon.server.datasource.reader.jdbc.dialect.ISqlDialect;
import org.bithon.server.datasource.reader.jdbc.pipeline.JdbcPipelineBuilder;
import org.bithon.server.datasource.reader.jdbc.statement.ast.OrderByClause;
import org.bithon.server.datasource.reader.jdbc.statement.ast.SelectStatement;
import org.bithon.server.datasource.reader.jdbc.statement.ast.TableIdentifier;
import org.bithon.server.datasource.reader.jdbc.statement.builder.SelectStatementBuilder;
import org.bithon.server.datasource.reader.jdbc.statement.serializer.Expression2Sql;
import org.bithon.server.storage.jdbc.common.jooq.Tables;
import org.bithon.server.storage.tracing.ITraceReader;
import org.bithon.server.storage.tracing.TraceSpan;
import org.bithon.server.storage.tracing.TraceStorageConfig;
import org.bithon.server.storage.tracing.mapping.TraceIdMapping;
import org.bithon.server.storage.tracing.reader.TraceFilterSplitter;
import org.jooq.Condition;
import org.jooq.Cursor;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Select;
import org.jooq.SelectConditionStep;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * @author frank.chen021@outlook.com
 * @date 30/12/20
 */
@Slf4j
public class TraceJdbcReader implements ITraceReader {

    private static final TypeReference<TreeMap<String, String>> TAG_TYPE = new TypeReference<>() {
    };

    protected final DSLContext dslContext;
    protected final ObjectMapper objectMapper;
    protected final TraceStorageConfig traceStorageConfig;
    protected final ISchema traceSpanSchema;
    protected final ISchema traceSpanSummarySchema;
    protected final ISchema traceTagIndexSchema;
    protected final ISqlDialect sqlDialect;
    protected final QuerySettings querySettings;

    public TraceJdbcReader(DSLContext dslContext,
                           ObjectMapper objectMapper,
                           ISchema traceSpanSummarySchema,
                           ISchema traceSpanSchema,
                           ISchema traceTagIndexSchema,
                           TraceStorageConfig traceStorageConfig,
                           ISqlDialect sqlDialect,
                           QuerySettings querySettings) {
        this.dslContext = dslContext;
        this.objectMapper = objectMapper;
        this.traceStorageConfig = traceStorageConfig;
        this.traceSpanSchema = traceSpanSchema;
        this.traceSpanSummarySchema = traceSpanSummarySchema;
        this.traceTagIndexSchema = traceTagIndexSchema;
        this.sqlDialect = sqlDialect;
        this.querySettings = querySettings;
    }

    @Override
    public CloseableIterator<TraceSpan> getTraceByTraceId(String traceId,
                                                          IExpression filter,
                                                          TimeSpan start,
                                                          TimeSpan end) {
        SelectConditionStep<Record> sql = dslContext.selectFrom(Tables.BITHON_TRACE_SPAN.getUnqualifiedName().quotedName())
                                                    .where(Tables.BITHON_TRACE_SPAN.TRACEID.eq(traceId));
        if (start != null) {
            // NOTE: we don't use Tables.BITHON_TRACE_SPAN.TIMESTAMP.ge(start) because the generated SQL might turn the start into a date time string which might cause time zone issues
            IExpression expr = new ComparisonExpression.GTE(
                new IdentifierExpression(Tables.BITHON_TRACE_SPAN.TIMESTAMP.getName()),
                sqlDialect.toISO8601TimestampExpression(start)
            );
            sql = sql.and(sqlDialect.createSqlSerializer(null).serialize(expr));
        }
        if (end != null) {
            IExpression expr = new ComparisonExpression.LT(
                new IdentifierExpression(Tables.BITHON_TRACE_SPAN.TIMESTAMP.getName()),
                sqlDialect.toISO8601TimestampExpression(end)
            );
            sql = sql.and(sqlDialect.createSqlSerializer(null).serialize(expr));
        }

        if (filter != null) {
            sql = sql.and(Expression2Sql.from(Tables.BITHON_TRACE_SPAN.getName(), sqlDialect, filter));
        }

        // For spans coming from the same application instance, sort them by the start time
        Cursor<Record> cursor = sql.orderBy(Tables.BITHON_TRACE_SPAN.TIMESTAMP.asc(),
                                            Tables.BITHON_TRACE_SPAN.INSTANCENAME,
                                            Tables.BITHON_TRACE_SPAN.STARTTIMEUS)
                                   .fetchLazy();

        return CloseableIterator.transform(cursor.iterator(),
                                           (record) -> toTraceSpan(record, TraceSpanRecordAccessor.TABLE_RECORD_ACCESSOR),
                                           cursor);
    }

    @Override
    public int getTraceSpanCount(String traceId,
                                 IExpression filter,
                                 TimeSpan start,
                                 TimeSpan end) {
        SelectConditionStep<Record1<Integer>> sql = dslContext.selectCount()
                                                              .from(Tables.BITHON_TRACE_SPAN.getUnqualifiedName().quotedName())
                                                              .where(Tables.BITHON_TRACE_SPAN.TRACEID.eq(traceId));
        if (start != null) {
            // NOTE: we don't use Tables.BITHON_TRACE_SPAN.TIMESTAMP.ge(start) because the generated SQL might turn the start into a date time string which might cause time zone issues
            IExpression expr = new ComparisonExpression.GTE(
                new IdentifierExpression(Tables.BITHON_TRACE_SPAN.TIMESTAMP.getName()),
                sqlDialect.toISO8601TimestampExpression(start)
            );
            sql = sql.and(sqlDialect.createSqlSerializer(null).serialize(expr));
        }
        if (end != null) {
            IExpression expr = new ComparisonExpression.LT(
                new IdentifierExpression(Tables.BITHON_TRACE_SPAN.TIMESTAMP.getName()),
                sqlDialect.toISO8601TimestampExpression(end)
            );
            sql = sql.and(sqlDialect.createSqlSerializer(null).serialize(expr));
        }

        if (filter != null) {
            sql = sql.and(Expression2Sql.from(Tables.BITHON_TRACE_SPAN.getName(), sqlDialect, filter));
        }

        Record1<Integer> record = sql.fetchOne();
        return record == null ? 0 : record.get(0, Integer.class);
    }

    @Override
    public CloseableIterator<TraceSpan> getTraceList(IExpression filter,
                                                     List<IExpression> indexedTagFilter,
                                                     Timestamp start,
                                                     Timestamp end,
                                                     OrderBy orderBy,
                                                     Limit limit) {
        throw new UnsupportedOperationException("This API has been deprecated. Please use /api/datasource/query or /api/datasource/query/stream");
    }

    @SuppressWarnings("rawtypes")
    private String toSQL(Select selectQuery) {
        return decorateSQL(dslContext.renderInlined(selectQuery));
    }

    protected String decorateSQL(String sql) {
        return sql;
    }

    @Override
    public List<TraceSpan> getTraceByParentSpanId(String parentSpanId) {
        return dslContext.selectFrom(Tables.BITHON_TRACE_SPAN.getUnqualifiedName().quotedName())
                         .where(Tables.BITHON_TRACE_SPAN.PARENTSPANID.eq(parentSpanId))
                         // For spans coming from the same application instance, sort them by the start time
                         .orderBy(Tables.BITHON_TRACE_SPAN.TIMESTAMP.asc(),
                                  Tables.BITHON_TRACE_SPAN.INSTANCENAME,
                                  Tables.BITHON_TRACE_SPAN.STARTTIMEUS)
                         .fetch((record) -> toTraceSpan(record, TraceSpanRecordAccessor.TABLE_RECORD_ACCESSOR));
    }

    @Override
    public TraceIdMapping getTraceIdByMapping(String userId) {
        return dslContext.select(Tables.BITHON_TRACE_MAPPING.TRACE_ID, Tables.BITHON_TRACE_MAPPING.TIMESTAMP)
                         .from(Tables.BITHON_TRACE_MAPPING)
                         .where(Tables.BITHON_TRACE_MAPPING.USER_TX_ID.eq(userId))
                         .orderBy(Tables.BITHON_TRACE_MAPPING.TIMESTAMP.desc())
                         .limit(1)
                         .fetchOne((v) -> {
                             TraceIdMapping mapping = new TraceIdMapping();
                             mapping.setTraceId(v.getValue(0, String.class));
                             mapping.setTimestamp(v.getValue(1, Timestamp.class).getTime());
                             mapping.setUserId(userId);
                             return mapping;
                         });
    }

    @Override
    public List<Map<String, Object>> getTraceSpanDistribution(String traceId,
                                                              IExpression filter,
                                                              TimeSpan start,
                                                              TimeSpan end,
                                                              Collection<String> groups) {
        List<IExpression> where = new ArrayList<>();
        where.add(new ComparisonExpression.EQ(
            new IdentifierExpression(Tables.BITHON_TRACE_SPAN.TRACEID.getName()),
            new LiteralExpression.StringLiteral(traceId)
        ));

        if (start != null) {
            // NOTE: we don't use Tables.BITHON_TRACE_SPAN.TIMESTAMP.ge(start) because the generated SQL might turn the start into a date time string which might cause time zone issues
            IExpression expr = new ComparisonExpression.GTE(
                new IdentifierExpression(Tables.BITHON_TRACE_SPAN.TIMESTAMP.getName()),
                sqlDialect.toISO8601TimestampExpression(start)
            );
            where.add(expr);
        }
        if (end != null) {
            IExpression expr = new ComparisonExpression.LT(
                new IdentifierExpression(Tables.BITHON_TRACE_SPAN.TIMESTAMP.getName()),
                sqlDialect.toISO8601TimestampExpression(end)
            );
            where.add(expr);
        }

        if (filter != null) {
            where.add(filter);
        }

        SelectStatement selectStatement = new SelectStatement();
        for (String group : groups) {
            selectStatement.getSelectorList().add(new org.bithon.server.datasource.query.ast.Column(group), IDataType.STRING);
        }

        IExpression countExpression = new FunctionExpression(AggregateFunction.Count.INSTANCE, new LiteralExpression.LongLiteral(1));
        selectStatement.getSelectorList().add(new ExpressionNode(countExpression), "count", IDataType.LONG);
        selectStatement.getFrom().setExpression(new TableIdentifier(Tables.BITHON_TRACE_SPAN.getUnqualifiedName().last()));
        selectStatement.getWhere().and(where);
        selectStatement.getGroupBy().addFields(groups);
        selectStatement.setOrderBy(new OrderByClause("count", Order.desc));

        String sql = selectStatement.toSQL(this.sqlDialect);
        log.info("Get trace span distribution: {}", sql);
        return dslContext.fetch(sql)
                         .map(Record::intoMap)
                         .stream()
                         .toList();
    }

    protected TraceSpan toTraceSpan(Record record, TraceSpanRecordAccessor recordAccessor) {
        TraceSpan span = new TraceSpan();
        span.appName = recordAccessor.getAppName(record);
        span.instanceName = recordAccessor.getInstanceName(record);
        span.traceId = recordAccessor.getTraceId(record);
        span.spanId = recordAccessor.getSpanId(record);
        span.parentSpanId = recordAccessor.getParentSpanId(record);
        span.startTime = recordAccessor.getStartTime(record);
        span.costTime = recordAccessor.getCostTime(record);
        span.endTime = recordAccessor.getEndTime(record);
        span.name = recordAccessor.getName(record);
        span.kind = recordAccessor.getKind(record);
        span.method = recordAccessor.getMethod(record);
        span.clazz = recordAccessor.getClazz(record);
        span.status = recordAccessor.getStatus(record);
        span.normalizedUri = recordAccessor.getNormalizedUrl(record);
        if (StringUtils.hasText(recordAccessor.getTags(record))) {
            // Compatible with old data
            try {
                span.tags = objectMapper.readValue(recordAccessor.getTags(record), TAG_TYPE);
            } catch (JsonProcessingException ignored) {
            }
        } else {
            span.tags = toTagMap(recordAccessor.getAttributes(record));
        }
        return span;
    }

    protected Map<String, String> toTagMap(Object attributes) {
        try {
            return objectMapper.readValue((String) attributes, TAG_TYPE);
        } catch (JsonProcessingException ignored) {
            return Collections.emptyMap();
        }
    }

    protected IDataSourceReader getDataSourceReader() {
        return new JdbcDataSourceReader(this.dslContext, this.sqlDialect, this.querySettings);
    }

    @Override
    public ColumnarTable timeseries(Query query) {
        boolean isOnRootTable = RootSpanKindFilterAnalyzer.analyze(query.getFilter()).isRootSpan();
        ISchema schema = isOnRootTable ? this.traceSpanSummarySchema : this.traceSpanSchema;

        TraceFilterSplitter splitter = new TraceFilterSplitter(schema, this.traceTagIndexSchema);
        splitter.split(query.getFilter());

        SelectStatement selectStatement = SelectStatementBuilder.builder()
                                                                .schema(schema)
                                                                .fields(query.getSelectors())
                                                                .filter(splitter.getExpression())
                                                                .interval(query.getInterval().with(schema.getTimestampSpec().getColumnName()))
                                                                .groupBy(query.getGroupBy())
                                                                .orderBy(query.getOrderBy())
                                                                .offset(query.getOffset())
                                                                .sqlDialect(this.sqlDialect)
                                                                .querySettings(query.getSettings())
                                                                .build();

        // Build the indexed tag sub query
        List<IExpression> indexedTagFilter = splitter.getIndexedTagFilters();
        if (CollectionUtils.isNotEmpty(indexedTagFilter)) {
            SelectConditionStep<Record1<String>> indexedTagQuery = new IndexedTagQueryBuilder(this.sqlDialect).dslContext(this.dslContext)
                                                                                                              .start(query.getInterval().getStartTime().toTimestamp().toLocalDateTime())
                                                                                                              .end(query.getInterval().getEndTime().toTimestamp().toLocalDateTime())
                                                                                                              .build(indexedTagFilter);
            Condition subQuery = isOnRootTable ? Tables.BITHON_TRACE_SPAN_SUMMARY.as(this.traceSpanSummarySchema.getDataStoreSpec().getStore()).TRACEID.in(indexedTagQuery)
                                               : Tables.BITHON_TRACE_SPAN.as(this.traceSpanSchema.getDataStoreSpec().getStore()).TRACEID.in(indexedTagQuery);
            String subQueryText = dslContext.renderInlined(subQuery);

            selectStatement.getWhere().and(new IExpression() {
                @Override
                public IDataType getDataType() {
                    return null;
                }

                @Override
                public String getType() {
                    return "";
                }

                @Override
                public Object evaluate(IEvaluationContext context) {
                    return null;
                }

                @Override
                public void accept(IExpressionInDepthVisitor visitor) {

                }

                @Override
                public <T> T accept(IExpressionVisitor<T> visitor) {
                    return null;
                }

                @Override
                public void serializeToText(ExpressionSerializer serializer) {
                    serializer.append(subQueryText);
                }
            });
        }

        Interval interval = query.getInterval();
        IQueryStep queryStep = JdbcPipelineBuilder.builder()
                                                  .dslContext(dslContext)
                                                  .dialect(this.sqlDialect)
                                                  .selectStatement(selectStatement)
                                                  .interval(Interval.of(interval.getStartTime().floor(query.getInterval().getStep()),
                                                                        interval.getEndTime(),
                                                                        interval.getStep(),
                                                                        null,
                                                                        new IdentifierExpression(schema.getTimestampSpec().getColumnName())))
                                                  .build();

        try {
            return queryStep.execute()
                            .get()
                            .getTable();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<?> select(Query query) {
        TraceFilterSplitter splitter = new TraceFilterSplitter(this.traceSpanSchema, this.traceTagIndexSchema);
        splitter.split(query.getFilter());

        CloseableIterator<TraceSpan> iterator = getTraceList(splitter.getExpression(),
                                                             splitter.getIndexedTagFilters(),
                                                             query.getInterval().getStartTime().toTimestamp(),
                                                             query.getInterval().getEndTime().toTimestamp(),
                                                             query.getOrderBy(),
                                                             query.getLimit());
        return iterator.toList();
    }

    @Override
    public ReadResponse query(Query query) {
        query = chooseSchema(query);

        if (query.isAggregateQuery()) {
            return getDataSourceReader().query(query);
        } else {
            SelectStatement selectStatement = toSelectStatement(query);
            String sql = selectStatement.toSQL(this.sqlDialect);
            log.info("Get trace list: {}", sql);

            int tagFieldIndex = -1;
            int startTimeFieldIndex = -1;
            List<Selector> selectors = selectStatement.getSelectorList().getSelectors();
            for (int i = 0; i < selectors.size(); i++) {
                Selector selector = selectors.get(i);
                if ("attributes".equals(((Column) selector.getSelectExpression()).getName())) {
                    tagFieldIndex = i;
                } else if ("startTimeUs".equals(((Column) selector.getSelectExpression()).getName())) {
                    startTimeFieldIndex = i;
                }
            }

            Function<Record, ?> mapper = createTraceSpanMapper(query.getResultFormat(), startTimeFieldIndex, tagFieldIndex);
            Cursor<Record> cursor = dslContext.fetchLazy(sql);
            CloseableIterator<DataRow<Object>> iterator = CloseableIterator.transform(cursor.iterator(),
                                                                                      (record) -> DataRow.data(mapper.apply(record)),
                                                                                      cursor);

            return ReadResponse.builder()
                               .meta(DataRow.Meta.of(query.getSelectors()
                                                          .stream()
                                                          .map(Selector::toColumnMetadata)
                                                          .toList()))
                               .data(iterator)
                               .build();
        }
    }

    protected SelectStatement toSelectStatement(Query query) {
        OrderBy orderBy = query.getOrderBy();
        if (orderBy != null) {
            // Compatible with old client side implementation
            String orderByField;
            if ("costTime".equals(orderBy.getName())) {
                orderByField = Tables.BITHON_TRACE_SPAN_SUMMARY.COSTTIMEUS.getName();
            } else if ("startTime".equals(orderBy.getName())) {
                orderByField = Tables.BITHON_TRACE_SPAN_SUMMARY.STARTTIMEUS.getName();
            } else {
                orderByField = orderBy.getName();
            }
            orderBy = new OrderBy(orderByField, orderBy.getOrder());
        } else {
            orderBy = new OrderBy(Tables.BITHON_TRACE_SPAN.TIMESTAMP.getName(), Order.desc);
        }

        TraceFilterSplitter splitter = new TraceFilterSplitter(this.traceSpanSchema, this.traceTagIndexSchema);
        splitter.split(query.getFilter());

        boolean isOnRootTable = query.getSchema().getName().equals(this.traceSpanSummarySchema.getName());

        SelectStatement selectStatement = SelectStatementBuilder.builder()
                                                                // Choose the correct table for speed up based on filter
                                                                .schema(query.getSchema())
                                                                .fields(query.getSelectors())
                                                                .filter(splitter.getExpression())
                                                                .interval(query.getInterval())
                                                                .groupBy(query.getGroupBy())
                                                                .orderBy(orderBy)
                                                                .limit(query.getLimit())
                                                                .offset(query.getOffset())
                                                                .sqlDialect(this.sqlDialect)
                                                                .querySettings(query.getSettings())
                                                                .buildSelectStatement();

        // Build the indexed tag sub query
        List<IExpression> indexedTagFilter = splitter.getIndexedTagFilters();
        if (CollectionUtils.isNotEmpty(indexedTagFilter)) {
            SelectConditionStep<Record1<String>> indexedTagQuery = new IndexedTagQueryBuilder(this.sqlDialect).dslContext(this.dslContext)
                                                                                                              .start(query.getInterval().getStartTime().toTimestamp().toLocalDateTime())
                                                                                                              .end(query.getInterval().getEndTime().toTimestamp().toLocalDateTime())
                                                                                                              .build(indexedTagFilter);
            Condition subQuery = isOnRootTable ? Tables.BITHON_TRACE_SPAN_SUMMARY.as(this.traceSpanSummarySchema.getDataStoreSpec().getStore()).TRACEID.in(indexedTagQuery)
                                               : Tables.BITHON_TRACE_SPAN.as(this.traceSpanSchema.getDataStoreSpec().getStore()).TRACEID.in(indexedTagQuery);
            String subQueryText = dslContext.renderInlined(subQuery);

            selectStatement.getWhere().and(new IExpression() {
                @Override
                public IDataType getDataType() {
                    return null;
                }

                @Override
                public String getType() {
                    return "";
                }

                @Override
                public Object evaluate(IEvaluationContext context) {
                    return null;
                }

                @Override
                public void accept(IExpressionInDepthVisitor visitor) {

                }

                @Override
                public <T> T accept(IExpressionVisitor<T> visitor) {
                    return null;
                }

                @Override
                public void serializeToText(ExpressionSerializer serializer) {
                    serializer.append(subQueryText);
                }
            });
        }

        return selectStatement;
    }

    protected Function<Record, ?> createTraceSpanMapper(ResultFormat format, int startTimeFieldIndex, int tagFieldIndex) {
        if (format == ResultFormat.ValueArray) {
            return (record) -> {
                int colSize = record.size();
                Object[] rowObject = new Object[colSize];
                for (int i = 0; i < colSize; i++) {
                    rowObject[i] = record.get(i);
                }
                if (startTimeFieldIndex >= 0 && startTimeFieldIndex < colSize) {
                    rowObject[startTimeFieldIndex] = MicrosecondsUtils.toMicroseconds(rowObject[startTimeFieldIndex]);
                }
                if (tagFieldIndex >= 0 && tagFieldIndex < colSize) {
                    rowObject[tagFieldIndex] = toTagMap(rowObject[tagFieldIndex]);
                }
                return rowObject;
            };
        } else { // If not given or Object, default to Object
            return (record) -> {
                Map<String, Object> rowObject = new LinkedHashMap<>(record.size());
                for (int i = 0, size = record.size(); i < size; i++) {
                    Field<?> field = record.field(i);
                    Object val = record.get(i);
                    if (i == tagFieldIndex) {
                        val = toTagMap(val);
                    } else if (i == startTimeFieldIndex) {
                        val = MicrosecondsUtils.toMicroseconds(val);
                    }
                    rowObject.put(field.getName(), val);
                }
                return rowObject;
            };
        }
    }

    @Override
    public int count(Query query) {
        return getDataSourceReader().count(chooseSchema(query));
    }

    @Override
    public List<String> distinct(Query query) {
        String schemaName = query.getSchema().getName();

        // Because the distinct queries can also be executed on tag table, so we need to check if the target schema is the span table
        if (schemaName.equals(this.traceSpanSchema.getName()) || schemaName.equals(this.traceSpanSummarySchema.getName())) {
            query = chooseSchema(query);
        }

        return getDataSourceReader().distinct(query);
    }

    public static class AnalyzeResult {
        private final boolean isRootSpan;
        private final IExpression expression;

        public AnalyzeResult(boolean isRootSpan, IExpression expression) {
            this.isRootSpan = isRootSpan;
            this.expression = expression;
        }

        public boolean isRootSpan() {
            return isRootSpan;
        }

        public IExpression getExpression() {
            return expression;
        }
    }

    public static class RootSpanKindFilterAnalyzer implements IExpressionInDepthVisitor {
        public static AnalyzeResult analyze(IExpression expression) {
            if (expression == null) {
                return new AnalyzeResult(false, null);
            }

            final String kindFieldName = Tables.BITHON_TRACE_SPAN_SUMMARY.KIND.getName();
            RootSpanKindFilterAnalyzer detector = new RootSpanKindFilterAnalyzer(kindFieldName);
            expression.accept(detector);
            IExpression resultExpression = expression;
            if (detector.isTrue) {
                // Fold
                resultExpression = ExpressionOptimizer.optimize(expression);
            }
            return new AnalyzeResult(detector.isTrue, resultExpression);
        }

        private boolean isTrue = false;

        private final String kindFieldName;

        private RootSpanKindFilterAnalyzer(String kindFieldName) {
            this.kindFieldName = kindFieldName;
        }

        @Override
        public boolean visit(ConditionalExpression expression) {
            if (!(expression.getLhs() instanceof IdentifierExpression identifierExpression)) {
                // Only support the IdentifierExpression in the left for simplicity.
                // Do not throw exception here 'cause the AST might contain some other internal optimization rule
                // such as 1 = 1 for simple processing
                return false;
            }

            if (!identifierExpression.getIdentifier().equals(kindFieldName)) {
                return false;
            }

            if (expression instanceof ComparisonExpression.EQ) {
                IExpression right = expression.getRhs();

                if (right instanceof LiteralExpression) {
                    String kindValue = (String) ((LiteralExpression<?>) right).getValue();
                    isTrue = SpanKind.isRootSpan(kindValue);
                }
                return false;
            }

            if (expression instanceof ConditionalExpression.In) {
                ExpressionList inList = ((ExpressionList) expression.getRhs());

                Set<Object> sets = new HashSet<>();
                for (IExpression expr : inList.getExpressions()) {
                    if (expr instanceof LiteralExpression<?> literal) {
                        Object val = literal.getValue();
                        if (SpanKind.isRootSpan(val)) {
                            sets.add(val);
                        }
                    }
                }
                if (!sets.isEmpty()) {
                    isTrue = true;
                }
                if (sets.size() == SpanKind.distinctRootSpanCount()) {
                    // Change to: 1 in (1)
                    expression.setLhs(LiteralExpression.ofLong(1));
                    expression.setRhs(new ExpressionList(LiteralExpression.ofLong(1)));
                }
            }

            return true;
        }

        @Override
        public boolean visit(LogicalExpression expression) {
            // A little complicated to apply the optimization, ignore
            return !(expression instanceof LogicalExpression.NOT);
        }
    }

    private Query chooseSchema(Query query) {
        ISchema schema;
        AnalyzeResult result = RootSpanKindFilterAnalyzer.analyze(query.getFilter());
        if (result.isRootSpan()) {
            schema = this.traceSpanSummarySchema;
        } else {
            schema = this.traceSpanSchema;
        }

        return query.copy()
                    .schema(schema)
                    .filter(result.getExpression())
                    .interval(query.getInterval().with(schema.getTimestampSpec().getColumnName()))
                    .build();
    }
}
