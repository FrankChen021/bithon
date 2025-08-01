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
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.expression.ComparisonExpression;
import org.bithon.component.commons.expression.ConditionalExpression;
import org.bithon.component.commons.expression.ExpressionList;
import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IExpressionInDepthVisitor;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.component.commons.expression.function.builtin.AggregateFunction;
import org.bithon.component.commons.tracing.SpanKind;
import org.bithon.component.commons.utils.CloseableIterator;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.datasource.ISchema;
import org.bithon.server.datasource.query.IDataSourceReader;
import org.bithon.server.datasource.query.Limit;
import org.bithon.server.datasource.query.Order;
import org.bithon.server.datasource.query.OrderBy;
import org.bithon.server.datasource.query.Query;
import org.bithon.server.datasource.query.ast.ExpressionNode;
import org.bithon.server.datasource.query.pipeline.Column;
import org.bithon.server.datasource.query.pipeline.ColumnarTable;
import org.bithon.server.datasource.query.setting.QuerySettings;
import org.bithon.server.datasource.reader.jdbc.JdbcDataSourceReader;
import org.bithon.server.datasource.reader.jdbc.dialect.ISqlDialect;
import org.bithon.server.datasource.reader.jdbc.statement.ast.OrderByClause;
import org.bithon.server.datasource.reader.jdbc.statement.ast.SelectStatement;
import org.bithon.server.datasource.reader.jdbc.statement.ast.TableIdentifier;
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
import org.jooq.SelectSeekStep1;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 30/12/20
 */
@Slf4j
public class TraceJdbcReader implements ITraceReader {

    protected final DSLContext dslContext;
    protected final ObjectMapper objectMapper;
    protected final TraceStorageConfig traceStorageConfig;
    protected final ISchema traceSpanSchema;
    protected final ISchema traceTagIndexSchema;
    protected final ISqlDialect sqlDialect;
    protected final QuerySettings querySettings;

    public TraceJdbcReader(DSLContext dslContext,
                           ObjectMapper objectMapper,
                           ISchema traceSpanSchema,
                           ISchema traceTagIndexSchema,
                           TraceStorageConfig traceStorageConfig,
                           ISqlDialect sqlDialect,
                           QuerySettings querySettings) {
        this.dslContext = dslContext;
        this.objectMapper = objectMapper;
        this.traceStorageConfig = traceStorageConfig;
        this.traceSpanSchema = traceSpanSchema;
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
                                           this::toTraceSpan,
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
    public List<TraceSpan> getTraceList(IExpression filter,
                                        List<IExpression> indexedTagFilter,
                                        Timestamp start,
                                        Timestamp end,
                                        OrderBy orderBy,
                                        Limit limit) {
        boolean isOnSummaryTable = isFilterOnRootSpanOnly(filter);

        Field<LocalDateTime> timestampField = isOnSummaryTable ? Tables.BITHON_TRACE_SPAN_SUMMARY.TIMESTAMP : Tables.BITHON_TRACE_SPAN.TIMESTAMP;

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

        Field<?> orderField;
        if ("costTime".equals(orderBy.getName())) {
            orderField = isOnSummaryTable ? Tables.BITHON_TRACE_SPAN_SUMMARY.COSTTIMEMS : Tables.BITHON_TRACE_SPAN.COSTTIMEMS;
        } else if ("startTime".equals(orderBy.getName())) {
            orderField = isOnSummaryTable ? Tables.BITHON_TRACE_SPAN_SUMMARY.STARTTIMEUS : Tables.BITHON_TRACE_SPAN.COSTTIMEMS;
        } else {
            orderField = Arrays.stream((isOnSummaryTable ? Tables.BITHON_TRACE_SPAN_SUMMARY : Tables.BITHON_TRACE_SPAN).fields())
                               .filter((f) -> f.getName().equals(orderBy.getName()))
                               .findFirst().orElse(isOnSummaryTable ? Tables.BITHON_TRACE_SPAN_SUMMARY.TIMESTAMP : Tables.BITHON_TRACE_SPAN.COSTTIMEMS);
        }

        SelectSeekStep1<?, ?> orderedListQuery;
        if (Order.desc.equals(orderBy.getOrder())) {
            orderedListQuery = listQuery.orderBy(orderField.desc());
        } else {
            orderedListQuery = listQuery.orderBy(orderField.asc());
        }

        String sql = toSQL(orderedListQuery.offset(limit.getOffset())
                                           .limit(limit.getLimit()));
        log.info("Get trace list: {}", sql);
        return dslContext.fetch(sql)
                         .map(this::toTraceSpan);
    }

    @Override
    public ColumnarTable getTraceDistribution(IExpression filter,
                                              List<IExpression> indexedTagFilter,
                                              Timestamp start,
                                              Timestamp end,
                                              long interval) {
        boolean isOnSummaryTable = isFilterOnRootSpanOnly(filter);

        IdentifierExpression timestampCol = new IdentifierExpression(Tables.BITHON_TRACE_SPAN_SUMMARY.TIMESTAMP.getName());

        String timeBucket = sqlDialect.timeFloorExpression(timestampCol, interval);
        StringBuilder sqlBuilder = new StringBuilder(StringUtils.format("SELECT %s AS %s, count(1) AS %s, min(%s) AS %s, avg(%s) AS %s, max(%s) AS %s FROM %s",
                                                                        timeBucket,
                                                                        sqlDialect.quoteIdentifier("_timestamp"),
                                                                        sqlDialect.quoteIdentifier("count"),
                                                                        sqlDialect.quoteIdentifier(Tables.BITHON_TRACE_SPAN_SUMMARY.COSTTIMEMS.getName()),
                                                                        sqlDialect.quoteIdentifier("minResponse"),
                                                                        sqlDialect.quoteIdentifier(Tables.BITHON_TRACE_SPAN_SUMMARY.COSTTIMEMS.getName()),
                                                                        sqlDialect.quoteIdentifier("avgResponse"),
                                                                        sqlDialect.quoteIdentifier(Tables.BITHON_TRACE_SPAN_SUMMARY.COSTTIMEMS.getName()),
                                                                        sqlDialect.quoteIdentifier("maxResponse"),
                                                                        sqlDialect.quoteIdentifier(isOnSummaryTable ? Tables.BITHON_TRACE_SPAN_SUMMARY.getName() : Tables.BITHON_TRACE_SPAN.getName())));

        IExpression tsExpression = new LogicalExpression.AND(new ComparisonExpression.GTE(timestampCol, sqlDialect.toISO8601TimestampExpression(start)),
                                                             new ComparisonExpression.LT(timestampCol, sqlDialect.toISO8601TimestampExpression(end)));
        sqlBuilder.append(" WHERE ");
        sqlBuilder.append(sqlDialect.createSqlSerializer(null).serialize(tsExpression));

        if (filter != null) {
            sqlBuilder.append(" AND ");
            sqlBuilder.append(Expression2Sql.from((isOnSummaryTable ? Tables.BITHON_TRACE_SPAN_SUMMARY : Tables.BITHON_TRACE_SPAN).getName(),
                                                  sqlDialect,
                                                  filter));
        }

        // Build the indexed tag sub query
        if (CollectionUtils.isNotEmpty(indexedTagFilter)) {
            SelectConditionStep<Record1<String>> indexedTagQuery = new IndexedTagQueryBuilder(this.sqlDialect)
                .dslContext(this.dslContext)
                .start(start.toLocalDateTime())
                .end(end.toLocalDateTime())
                .build(indexedTagFilter);
            Condition subQuery = isOnSummaryTable ? Tables.BITHON_TRACE_SPAN_SUMMARY.TRACEID.in(indexedTagQuery) :
                                 Tables.BITHON_TRACE_SPAN.TRACEID.in(indexedTagQuery);

            sqlBuilder.append(" AND ");
            sqlBuilder.append(dslContext.renderInlined(subQuery));
        }

        sqlBuilder.append(StringUtils.format(" GROUP BY %s ORDER BY %s", this.sqlDialect.quoteIdentifier("_timestamp"), this.sqlDialect.quoteIdentifier("_timestamp"), timeBucket));

        String sql = sqlBuilder.toString();
        log.info("Get trace distribution: {}", sql);

        ColumnarTable table = new ColumnarTable();
        Column tsColumn = table.addColumn(Column.create("_timestamp", IDataType.LONG, 256));
        Column countColumn = table.addColumn(Column.create("count", IDataType.LONG, 256));
        Column minColumn = table.addColumn(Column.create("minResponse", IDataType.LONG, 256));
        Column avgColumn = table.addColumn(Column.create("avgResponse", IDataType.LONG, 256));
        Column maxColumn = table.addColumn(Column.create("maxResponse", IDataType.LONG, 256));

        try (Cursor<Record> records = dslContext.fetchLazy(decorateSQL(sql))) {
            for (Record record : records) {
                tsColumn.addObject(record.get(0));
                countColumn.addObject(record.get(1));
                minColumn.addObject(record.get(2));
                avgColumn.addObject(record.get(3));
                maxColumn.addObject(record.get(4));
            }
        }

        return table;
    }

    @Override
    public int getTraceListSize(IExpression filter,
                                List<IExpression> indexedTagFilters,
                                Timestamp start,
                                Timestamp end) {
        boolean isOnSummaryTable = isFilterOnRootSpanOnly(filter);

        Field<LocalDateTime> timestampField = isOnSummaryTable ? Tables.BITHON_TRACE_SPAN_SUMMARY.TIMESTAMP : Tables.BITHON_TRACE_SPAN.TIMESTAMP;

        // NOTE:
        // 1. the query is performed on summary table or detail table based on input filters
        // 2. the WHERE clause is built on raw SQL string
        // because the jOOQ DSL expression, where(summary.TIMESTAMP.lt(xxx)), might translate the TIMESTAMP as a full qualified name,
        // but the query might be performed on the detailed table
        SelectConditionStep<Record1<Integer>> countQuery = dslContext.selectCount()
                                                                     .from(isOnSummaryTable ? Tables.BITHON_TRACE_SPAN_SUMMARY : Tables.BITHON_TRACE_SPAN)
                                                                     .where(timestampField.ge(start.toLocalDateTime()).and(timestampField.lt(end.toLocalDateTime())));

        if (filter != null) {
            countQuery = countQuery.and(Expression2Sql.from((isOnSummaryTable ? Tables.BITHON_TRACE_SPAN_SUMMARY : Tables.BITHON_TRACE_SPAN).getName(),
                                                            sqlDialect,
                                                            filter));
        }

        // Build the indexed tag query
        SelectConditionStep<Record1<String>> indexedTagQuery = new IndexedTagQueryBuilder(this.sqlDialect).dslContext(this.dslContext)
                                                                                                          .start(start.toLocalDateTime())
                                                                                                          .end(end.toLocalDateTime())
                                                                                                          .build(indexedTagFilters);
        if (indexedTagQuery != null) {
            if (isOnSummaryTable) {
                countQuery = countQuery.and(Tables.BITHON_TRACE_SPAN_SUMMARY.TRACEID.in(indexedTagQuery));
            } else {
                countQuery = countQuery.and(Tables.BITHON_TRACE_SPAN.TRACEID.in(indexedTagQuery));
            }
        }

        return ((Number) dslContext.fetchOne(toSQL(countQuery)).get(0)).intValue();
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
                         .fetch(this::toTraceSpan);
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

    private TraceSpan toTraceSpan(Record record) {
        TraceSpan span = new TraceSpan();
        span.appName = TraceSpanRecordAccessor.getAppName(record);
        span.instanceName = TraceSpanRecordAccessor.getInstanceName(record);
        span.traceId = TraceSpanRecordAccessor.getTraceId(record);
        span.spanId = TraceSpanRecordAccessor.getSpanId(record);
        span.parentSpanId = TraceSpanRecordAccessor.getParentSpanId(record);
        span.startTime = TraceSpanRecordAccessor.getStartTime(record);
        span.costTime = TraceSpanRecordAccessor.getCostTime(record);
        span.endTime = TraceSpanRecordAccessor.getEndTime(record);
        span.name = TraceSpanRecordAccessor.getName(record);
        span.kind = TraceSpanRecordAccessor.getKind(record);
        span.method = TraceSpanRecordAccessor.getMethod(record);
        span.clazz = TraceSpanRecordAccessor.getClazz(record);
        span.status = TraceSpanRecordAccessor.getStatus(record);
        span.normalizedUri = TraceSpanRecordAccessor.getNormalizedUrl(record);
        if (StringUtils.hasText(TraceSpanRecordAccessor.getTags(record))) {
            // Compatible with old data
            try {
                span.tags = objectMapper.readValue(TraceSpanRecordAccessor.getTags(record), TraceSpan.TagDeserializer.TYPE);
            } catch (JsonProcessingException ignored) {
            }
        } else {
            span.tags = toTagMap(TraceSpanRecordAccessor.getAttributes(record));
        }
        return span;
    }

    /**
     * RootSpan has been extracted into trace_span_summary table during ingestion.
     * If the filter on the 'kind'
     * column selects those rows extracted into the summary table, later we only query the summary table.
     */
    private boolean isFilterOnRootSpanOnly(IExpression expression) {
        if (expression == null) {
            return true;
        }

        final String kindFieldName = Tables.BITHON_TRACE_SPAN_SUMMARY.KIND.getName();
        SpanKindIsRootDetector detector = new SpanKindIsRootDetector(kindFieldName);
        expression.accept(detector);
        return detector.isTrue;
    }

    protected Map<String, String> toTagMap(Object attributes) {
        try {
            return objectMapper.readValue((String) attributes, TraceSpan.TagDeserializer.TYPE);
        } catch (JsonProcessingException ignored) {
            return Collections.emptyMap();
        }
    }

    protected IDataSourceReader getDataSourceReader() {
        return new JdbcDataSourceReader(this.dslContext, this.sqlDialect, this.querySettings);
    }

    @Override
    public ColumnarTable timeseries(Query query) {
        TraceFilterSplitter splitter = new TraceFilterSplitter(this.traceSpanSchema, this.traceTagIndexSchema);
        splitter.split(query.getFilter());

        return getTraceDistribution(splitter.getExpression(),
                                    splitter.getIndexedTagFilters(),
                                    query.getInterval().getStartTime().toTimestamp(),
                                    query.getInterval().getEndTime().toTimestamp(),
                                    query.getInterval().getStep().getSeconds());
    }

    @Override
    public List<?> groupBy(Query query) {
        return getDataSourceReader().groupBy(query);
    }

    @Override
    public List<?> select(Query query) {
        TraceFilterSplitter splitter = new TraceFilterSplitter(this.traceSpanSchema, this.traceTagIndexSchema);
        splitter.split(query.getFilter());

        return getTraceList(splitter.getExpression(),
                            splitter.getIndexedTagFilters(),
                            query.getInterval().getStartTime().toTimestamp(),
                            query.getInterval().getEndTime().toTimestamp(),
                            query.getOrderBy(),
                            query.getLimit());
    }

    @Override
    public int count(Query query) {
        TraceFilterSplitter splitter = new TraceFilterSplitter(this.traceSpanSchema, this.traceTagIndexSchema);
        splitter.split(query.getFilter());

        return getTraceListSize(splitter.getExpression(),
                                splitter.getIndexedTagFilters(),
                                query.getInterval().getStartTime().toTimestamp(),
                                query.getInterval().getEndTime().toTimestamp());
    }

    @Override
    public List<String> distinct(Query query) {
        return getDataSourceReader().distinct(query);
    }

    static class SpanKindIsRootDetector implements IExpressionInDepthVisitor {
        private boolean isTrue = false;

        private final String kindFieldName;

        SpanKindIsRootDetector(String kindFieldName) {
            this.kindFieldName = kindFieldName;
        }

        @Override
        public boolean visit(ConditionalExpression expression) {
            if (!(expression.getLhs() instanceof IdentifierExpression)) {
                // Only support the IdentifierExpression in the left for simplicity.
                // Do not throw exception here 'cause the AST might contain some other internal optimization rule
                // such as 1 = 1 for simple processing
                return false;
            }

            if (expression instanceof ComparisonExpression.EQ) {
                IExpression left = expression.getLhs();
                IExpression right = expression.getRhs();

                if (((IdentifierExpression) left).getIdentifier().equals(kindFieldName)) {
                    if (right instanceof LiteralExpression) {
                        String kindValue = (String) ((LiteralExpression<?>) right).getValue();
                        isTrue = SpanKind.isRootSpan(kindValue);
                    }
                }
                return false;
            }

            if (expression instanceof ConditionalExpression.In) {
                IExpression left = expression.getLhs();
                IExpression right = expression.getRhs();

                if (((IdentifierExpression) left).getIdentifier().equals(kindFieldName)) {
                    isTrue = ((ExpressionList) right).getExpressions()
                                                     .stream()
                                                     .allMatch((s) -> (s instanceof LiteralExpression) && SpanKind.isRootSpan(((LiteralExpression<?>) s).getValue()));

                    // TODO: Apply more optimization here is the collection size equals to the size of all root spans
                    // We can remove such filter
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
}
