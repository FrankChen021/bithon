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

package org.bithon.server.storage.jdbc.tracing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.time.DateTime;
import org.bithon.component.commons.tracing.SpanKind;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.commons.matcher.StringEqualMatcher;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.typing.StringValueType;
import org.bithon.server.storage.jdbc.jooq.Tables;
import org.bithon.server.storage.jdbc.jooq.tables.BithonTraceSpanSummary;
import org.bithon.server.storage.jdbc.jooq.tables.records.BithonTraceSpanRecord;
import org.bithon.server.storage.jdbc.jooq.tables.records.BithonTraceSpanSummaryRecord;
import org.bithon.server.storage.jdbc.utils.ISqlDialect;
import org.bithon.server.storage.jdbc.utils.SQLFilterBuilder;
import org.bithon.server.storage.metrics.DimensionFilter;
import org.bithon.server.storage.metrics.IFilter;
import org.bithon.server.storage.tracing.ITraceReader;
import org.bithon.server.storage.tracing.TraceSpan;
import org.bithon.server.storage.tracing.TraceStorageConfig;
import org.bithon.server.storage.tracing.index.TagIndexConfig;
import org.bithon.server.storage.tracing.mapping.TraceIdMapping;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.SelectConditionStep;
import org.jooq.SelectSeekStep1;
import org.jooq.impl.DSL;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 30/12/20
 */
@Slf4j
public class TraceJdbcReader implements ITraceReader {

    public static final String SPAN_TAGS_PREFIX = "tags.";
    private final DSLContext dslContext;
    private final ObjectMapper objectMapper;
    private final TraceStorageConfig traceStorageConfig;
    private final DataSourceSchema traceSpanSchema;
    private final DataSourceSchema traceTagIndexSchema;
    private final ISqlDialect sqlDialect;

    public TraceJdbcReader(DSLContext dslContext,
                           ObjectMapper objectMapper,
                           DataSourceSchema traceSpanSchema,
                           DataSourceSchema traceTagIndexSchema,
                           TraceStorageConfig traceStorageConfig,
                           ISqlDialect sqlDialect) {
        this.dslContext = dslContext;
        this.objectMapper = objectMapper;
        this.traceStorageConfig = traceStorageConfig;
        this.traceSpanSchema = traceSpanSchema;
        this.traceTagIndexSchema = traceTagIndexSchema;
        this.sqlDialect = sqlDialect;
    }

    @Override
    public List<TraceSpan> getTraceByTraceId(String traceId, TimeSpan start, TimeSpan end) {
        SelectConditionStep<BithonTraceSpanRecord> sql = dslContext.selectFrom(Tables.BITHON_TRACE_SPAN)
                                                                   .where(Tables.BITHON_TRACE_SPAN.TRACEID.eq(traceId));
        if (start != null) {
            sql = sql.and(Tables.BITHON_TRACE_SPAN.TIMESTAMP.ge(start.toTimestamp().toLocalDateTime()));
        }
        if (end != null) {
            sql = sql.and(Tables.BITHON_TRACE_SPAN.TIMESTAMP.lt(end.toTimestamp().toLocalDateTime()));
        }

        // For spans coming from the same application instance, sort them by the start time
        return sql.orderBy(Tables.BITHON_TRACE_SPAN.TIMESTAMP.asc(),
                           Tables.BITHON_TRACE_SPAN.INSTANCENAME,
                           Tables.BITHON_TRACE_SPAN.STARTTIMEUS)
                  .fetch(this::toTraceSpan);
    }

    @Override
    public List<TraceSpan> getTraceList(List<IFilter> filters,
                                        Timestamp start,
                                        Timestamp end,
                                        String orderBy,
                                        String order,
                                        int pageNumber,
                                        int pageSize) {
        BithonTraceSpanSummary summaryTable = Tables.BITHON_TRACE_SPAN_SUMMARY;
        SelectConditionStep<BithonTraceSpanSummaryRecord> listQuery = dslContext.selectFrom(summaryTable)
                                                                                .where(summaryTable.TIMESTAMP.ge(start.toLocalDateTime()))
                                                                                .and(summaryTable.TIMESTAMP.lt(end.toLocalDateTime()));

        String moreFilter = SQLFilterBuilder.build(traceSpanSchema,
                                                   filters.stream().filter(filter -> !filter.getName().startsWith(SPAN_TAGS_PREFIX)));
        if (StringUtils.hasText(moreFilter)) {
            listQuery = listQuery.and(moreFilter);
        }

        // Build the tag query
        SelectConditionStep<Record1<String>> tagQuery = buildTagQuery(start, end, filters);
        if (tagQuery != null) {
            listQuery = listQuery.and(summaryTable.TRACEID.in(tagQuery));
        }

        Field<?> orderField;
        if ("costTime".equals(orderBy)) {
            orderField = summaryTable.COSTTIMEMS;
        } else if ("startTime".equals(orderBy)) {
            orderField = summaryTable.STARTTIMEUS;
        } else {
            orderField = summaryTable.TIMESTAMP;
        }
        SelectSeekStep1<?, ?> orderedListQuery;
        if ("desc".equals(order)) {
            orderedListQuery = listQuery.orderBy(orderField.desc());
        } else {
            orderedListQuery = listQuery.orderBy(orderField.asc());
        }

        return orderedListQuery.offset(pageNumber * pageSize)
                               .limit(pageSize)
                               .fetch(r -> this.toTraceSpan((BithonTraceSpanSummaryRecord) r));
    }

    abstract static class NestQueryBuilder {
        protected DSLContext dslContext;
        protected LocalDateTime start;
        protected LocalDateTime end;

        protected SelectConditionStep<Record1<String>> in;

        public NestQueryBuilder dslContext(DSLContext dslContext) {
            this.dslContext = dslContext;
            return this;
        }

        public NestQueryBuilder start(LocalDateTime start) {
            this.start = start;
            return this;
        }

        public NestQueryBuilder end(LocalDateTime end) {
            this.end = end;
            return this;
        }

        public NestQueryBuilder in(SelectConditionStep<Record1<String>> in) {
            this.in = in;
            return this;
        }

        public abstract SelectConditionStep<Record1<String>> build(List<IFilter> filters);
    }

    /**
     * TODO:
     *  1. If a given tag name is not in the index list, the query on that name should fall back to BITHON_TRACE_SPAN table to match
     *  2. For multiple tags which are not in the same group, nested query should be applied
     */
    static class TagConditionQueryBuilder extends NestQueryBuilder {

        private final TagIndexConfig indexConfig;
        private final DataSourceSchema traceTagIndexSchema;

        TagConditionQueryBuilder(TagIndexConfig indexConfig, DataSourceSchema traceTagIndexSchema) {
            this.indexConfig = indexConfig;
            this.traceTagIndexSchema = traceTagIndexSchema;
        }

        @Override
        public SelectConditionStep<Record1<String>> build(List<IFilter> filters) {
            if (filters.isEmpty()) {
                return null;
            }

            SelectConditionStep<Record1<String>> query = null;

            Iterator<IFilter> i = filters.iterator();
            while (i.hasNext()) {
                IFilter filter = i.next();
                if (!filter.getName().startsWith(SPAN_TAGS_PREFIX)) {
                    continue;
                }

                // Remove this filter
                i.remove();

                String tagName = filter.getName().substring(SPAN_TAGS_PREFIX.length());
                if (!StringUtils.hasText(tagName)) {
                    throw new RuntimeException(StringUtils.format("Wrong tag name [%s]", filter.getName()));
                }

                Preconditions.checkNotNull(indexConfig, "No index configured for 'tags' attribute.");

                int tagIndex = this.indexConfig.getColumnPos(tagName);
                if (tagIndex == 0) {
                    throw new RuntimeException(StringUtils.format("Can't search on tag [%s] because it is not configured in the index.", tagName));
                }
                if (tagIndex > Tables.BITHON_TRACE_SPAN_TAG_INDEX.fieldsRow().size() - 2) {
                    throw new RuntimeException(StringUtils.format("Tag [%s] is configured to use wrong index [%d]. Should be in the range [1, %d]",
                                                                  tagName,
                                                                  tagIndex,
                                                                  Tables.BITHON_TRACE_SPAN_TAG_INDEX.fieldsRow().size() - 2));
                }

                if (query == null) {
                    query = dslContext.select(Tables.BITHON_TRACE_SPAN_TAG_INDEX.TRACEID)
                                      .from(Tables.BITHON_TRACE_SPAN_TAG_INDEX)
                                      .where(Tables.BITHON_TRACE_SPAN_TAG_INDEX.TIMESTAMP.ge(this.start))
                                      .and(Tables.BITHON_TRACE_SPAN_TAG_INDEX.TIMESTAMP.lt(this.end));
                }
                query = query.and(filter.getMatcher().accept(new SQLFilterBuilder(this.traceTagIndexSchema,
                                                                                  new DimensionFilter("f" + tagIndex, filter.getMatcher()))));
            }

            if (query != null) {
                if (this.in != null) {
                    return query.and(Tables.BITHON_TRACE_SPAN.TRACEID.in(this.in));
                } else {
                    return query;
                }
            } else {
                return this.in;
            }
        }
    }

    // TODO: httpclient span[http.status]--->status
    //
    static class TraceSpanTableQueryBuilder extends NestQueryBuilder {
        @Override
        public SelectConditionStep<Record1<String>> build(List<IFilter> filters) {
            if (filters.isEmpty()) {
                return null;
            }

            SelectConditionStep<Record1<String>> query = null;

            Iterator<IFilter> iterator = filters.iterator();
            while (iterator.hasNext()) {
                IFilter filter = iterator.next();

                boolean matches = false;
                if ("name".equals(filter.getName())) {
                    matches = true;
                } else if ("kind".equals(filter.getName())) {
                    if (filter.getMatcher() instanceof StringEqualMatcher) {
                        String kindValue = ((StringEqualMatcher) filter.getMatcher()).getPattern();

                        // RootSpan has been extracted into trace_span_summary table during ingestion
                        // If the filter is on the root spans, it SHOULD query on the summary table
                        matches = !SpanKind.isRootSpan(kindValue);
                    }
                }

                if (!matches) {
                    continue;
                }

                iterator.remove();
                if (query == null) {
                    query = dslContext.select(Tables.BITHON_TRACE_SPAN.TRACEID)
                                      .from(Tables.BITHON_TRACE_SPAN)
                                      .where(Tables.BITHON_TRACE_SPAN.TIMESTAMP.ge(start))
                                      .and(Tables.BITHON_TRACE_SPAN.TIMESTAMP.lt(end));
                }

                query = query.and(filter.getMatcher().accept(new SQLFilterBuilder(Tables.BITHON_TRACE_SPAN.getName(),
                                                                                  filter.getName(),
                                                                                  StringValueType.INSTANCE)));
            }

            if (query != null) {
                if (this.in != null) {
                    return query.and(Tables.BITHON_TRACE_SPAN.TRACEID.in(this.in));
                } else {
                    return query;
                }
            } else {
                return this.in;
            }
        }
    }

    @Override
    public List<Map<String, Object>> getTraceDistribution(List<IFilter> filters, Timestamp start, Timestamp end, int interval) {
        BithonTraceSpanSummary summaryTable = Tables.BITHON_TRACE_SPAN_SUMMARY;

        SelectConditionStep<Record1<String>> tagQuery = new TagConditionQueryBuilder(this.traceStorageConfig.getIndexes(),
                                                                                     this.traceTagIndexSchema).dslContext(this.dslContext)
                                                                                                              .start(start.toLocalDateTime())
                                                                                                              .end(end.toLocalDateTime())
                                                                                                              .build(filters);

        boolean isOnRootSpan = filters.stream().anyMatch((filter) -> {
            if (!summaryTable.KIND.getName().equals(filter.getName())) {
                return false;
            }

            if (!(filter.getMatcher() instanceof StringEqualMatcher)) {
                // If the filter is NOT equal filter, we turn this query to run on the detail table
                return false;
            }

            String kindValue = ((StringEqualMatcher) filter.getMatcher()).getPattern();

            // RootSpan has been extracted into trace_span_summary table during ingestion
            // If the filter is on the root spans, it SHOULD query on the summary table
            return SpanKind.isRootSpan(kindValue);
        });

        String timeBucket = sqlDialect.timeFloor("timestamp", interval);
        StringBuilder sqlBuilder = new StringBuilder(StringUtils.format("SELECT %s AS \"_timestamp\", count(1) AS \"count\", min(\"%s\") AS \"minResponse\", avg(\"%s\") AS \"avgResponse\", max(\"%s\") AS \"maxResponse\" FROM \"%s\"",
                                                                        timeBucket,
                                                                        summaryTable.COSTTIMEMS.getName(),
                                                                        summaryTable.COSTTIMEMS.getName(),
                                                                        summaryTable.COSTTIMEMS.getName(),
                                                                        isOnRootSpan ? summaryTable.getQualifiedName() : Tables.BITHON_TRACE_SPAN.getName()));
        sqlBuilder.append(StringUtils.format(" WHERE \"%s\" >= '%s' AND \"%s\" < '%s'",
                                             summaryTable.TIMESTAMP.getName(),
                                             DateTime.toYYYYMMDDhhmmss(start.getTime()),
                                             summaryTable.TIMESTAMP.getName(),
                                             DateTime.toYYYYMMDDhhmmss(end.getTime())));

        String extraSummaryFilter = SQLFilterBuilder.build(traceSpanSchema,
                                                           filters.stream().filter(filter -> !filter.getName().startsWith(SPAN_TAGS_PREFIX)));
        if (StringUtils.hasText(extraSummaryFilter)) {
            sqlBuilder.append(" AND ");
            sqlBuilder.append(extraSummaryFilter);
        }

        // Build the tag query
        if (tagQuery != null) {
            sqlBuilder.append(" AND ");
            sqlBuilder.append(dslContext.renderInlined(summaryTable.TRACEID.in(tagQuery)));
        }

        sqlBuilder.append(StringUtils.format(" GROUP BY \"_timestamp\" ORDER BY \"_timestamp\"", timeBucket));

        String sql = sqlBuilder.toString();
        log.info("Get trace distribution: {}", sql);
        return dslContext.fetch(sql).intoMaps();
    }

    @Override
    public int getTraceListSize(List<IFilter> filters,
                                Timestamp start,
                                Timestamp end) {
        BithonTraceSpanSummary summaryTable = Tables.BITHON_TRACE_SPAN_SUMMARY;

        SelectConditionStep<Record1<Integer>> countQuery = dslContext.select(DSL.count(summaryTable.TRACEID))
                                                                     .from(summaryTable)
                                                                     .where(summaryTable.TIMESTAMP.ge(start.toLocalDateTime()))
                                                                     .and(summaryTable.TIMESTAMP.lt(end.toLocalDateTime()));

        String moreFilter = SQLFilterBuilder.build(traceSpanSchema,
                                                   filters.stream().filter(filter -> !filter.getName().startsWith(SPAN_TAGS_PREFIX)));
        if (StringUtils.hasText(moreFilter)) {
            countQuery = countQuery.and(moreFilter);
        }

        // Build the tag query
        SelectConditionStep<Record1<String>> tagQuery = buildTagQuery(start, end, filters);
        if (tagQuery != null) {
            countQuery = countQuery.and(summaryTable.TRACEID.in(tagQuery));
        }

        return (int) countQuery.fetchOne(0);
    }

    @Override
    public List<TraceSpan> getTraceByParentSpanId(String parentSpanId) {
        return dslContext.selectFrom(Tables.BITHON_TRACE_SPAN)
                         .where(Tables.BITHON_TRACE_SPAN.PARENTSPANID.eq(parentSpanId))
                         // fFor spans coming from the same application instance, sort them by the start time
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
                             mapping.setTimestamp(v.get(1, Timestamp.class).getTime());
                             mapping.setUserId(userId);
                             return mapping;
                         });
    }

    /**
     * TODO:
     *  1. If a given tag name is not in the index list, the query on that name should fall back to BITHON_TRACE_SPAN table to match
     *  2. For multiple tags which are not in the same group, nested query should be applied
     */
    protected SelectConditionStep<Record1<String>> buildTagQuery(Timestamp start, Timestamp end, Collection<IFilter> filters) {
        SelectConditionStep<Record1<String>> tagQuery = null;

        for (IFilter filter : filters) {
            if (!filter.getName().startsWith(SPAN_TAGS_PREFIX)) {
                continue;
            }
            String tagName = filter.getName().substring(SPAN_TAGS_PREFIX.length());
            if (!StringUtils.hasText(tagName)) {
                throw new RuntimeException(StringUtils.format("Wrong tag name [%s]", filter.getName()));
            }

            Preconditions.checkNotNull(this.traceStorageConfig.getIndexes(), "No index configured for 'tags' attribute.");

            int tagIndex = this.traceStorageConfig.getIndexes().getColumnPos(tagName);
            if (tagIndex == 0) {
                throw new RuntimeException(StringUtils.format("Can't search on tag [%s] because it is not configured in the index.", tagName));
            }
            if (tagIndex > Tables.BITHON_TRACE_SPAN_TAG_INDEX.fieldsRow().size() - 2) {
                throw new RuntimeException(StringUtils.format("Tag [%s] is configured to use wrong index [%d]. Should be in the range [1, %d]",
                                                              tagName,
                                                              tagIndex,
                                                              Tables.BITHON_TRACE_SPAN_TAG_INDEX.fieldsRow().size() - 2));
            }

            if (tagQuery == null) {
                tagQuery = dslContext.select(Tables.BITHON_TRACE_SPAN_TAG_INDEX.TRACEID)
                                     .from(Tables.BITHON_TRACE_SPAN_TAG_INDEX)
                                     .where(Tables.BITHON_TRACE_SPAN_TAG_INDEX.TIMESTAMP.ge(start.toLocalDateTime()))
                                     .and(Tables.BITHON_TRACE_SPAN_TAG_INDEX.TIMESTAMP.lt(end.toLocalDateTime()));
            }
            tagQuery = tagQuery.and(filter.getMatcher().accept(new SQLFilterBuilder(this.traceTagIndexSchema,
                                                                                    new DimensionFilter("f" + tagIndex, filter.getMatcher()))));
        }

        return tagQuery;
    }

    @SuppressWarnings("unchecked")
    private TraceSpan toTraceSpan(BithonTraceSpanRecord record) {
        TraceSpan span = new TraceSpan();
        span.appName = record.getAppname();
        span.instanceName = record.getInstancename();
        span.traceId = record.getTraceid();
        span.spanId = record.getSpanid();
        span.parentSpanId = record.getParentspanid();
        span.startTime = record.getStarttimeus();
        span.costTime = record.getCosttimems();
        span.endTime = record.getEndtimeus();
        span.name = record.getName();
        span.kind = record.getKind();
        span.method = record.getMethod();
        span.clazz = record.getClazz();
        span.status = record.getStatus();
        span.normalizedUri = record.getNormalizedurl();
        if (StringUtils.hasText(record.getTags())) {
            // Compatible with old data
            try {
                span.tags = objectMapper.readValue(record.getTags(), TraceSpan.TagMap.class);
            } catch (JsonProcessingException ignored) {
            }
        } else {
            span.tags = new TraceSpan.TagMap((Map<String, String>) record.getAttributes());
        }
        span.name = record.getName();
        return span;
    }

    @SuppressWarnings("unchecked")
    private TraceSpan toTraceSpan(BithonTraceSpanSummaryRecord record) {
        TraceSpan span = new TraceSpan();
        span.appName = record.getAppname();
        span.instanceName = record.getInstancename();
        span.traceId = record.getTraceid();
        span.spanId = record.getSpanid();
        span.parentSpanId = record.getParentspanid();
        span.startTime = record.getStarttimeus();
        span.costTime = record.getCosttimems();
        span.endTime = record.getEndtimeus();
        span.name = record.getName();
        span.kind = record.getKind();
        span.method = record.getMethod();
        span.clazz = record.getClazz();
        span.status = record.getStatus();
        span.normalizedUri = record.getNormalizedurl();
        if (StringUtils.hasText(record.getTags())) {
            // Compatible with old data
            try {
                span.tags = objectMapper.readValue(record.getTags(), TraceSpan.TagMap.class);
            } catch (JsonProcessingException ignored) {
            }
        } else {
            span.tags = new TraceSpan.TagMap((Map<String, String>) record.getAttributes());
        }
        span.name = record.getName();
        return span;
    }
}
