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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.component.commons.time.DateTime;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.datasource.DataSourceSchema;
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
import org.bithon.server.storage.tracing.mapping.TraceIdMapping;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.SelectConditionStep;
import org.jooq.SelectSeekStep1;
import org.jooq.impl.DSL;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author frank.chen021@outlook.com
 * @date 30/12/20
 */
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

        // for spans coming from a same application instance, sort them by the start time
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

        // build tag query
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

    @Override
    public List<Map<String, Object>> getTraceDistribution(List<IFilter> filters, Timestamp start, Timestamp end, int interval) {
        BithonTraceSpanSummary summaryTable = Tables.BITHON_TRACE_SPAN_SUMMARY;

        String timeBucket = sqlDialect.timeFloor("timestamp", interval);
        StringBuilder sqlBuilder = new StringBuilder(StringUtils.format("SELECT %s AS \"_timestamp\", count(1) AS \"count\", min(\"%s\") AS \"minResponse\", avg(\"%s\") AS \"avgResponse\", max(\"%s\") AS \"maxResponse\" FROM %s",
                                                                        timeBucket,
                                                                        summaryTable.COSTTIMEMS.getName(),
                                                                        summaryTable.COSTTIMEMS.getName(),
                                                                        summaryTable.COSTTIMEMS.getName(),
                                                                        summaryTable.getQualifiedName()));
        sqlBuilder.append(StringUtils.format(" WHERE \"%s\" >= '%s' AND \"%s\" < '%s'",
                                             summaryTable.TIMESTAMP.getName(),
                                             DateTime.toYYYYMMDDhhmmss(start.getTime()),
                                             summaryTable.TIMESTAMP.getName(),
                                             DateTime.toYYYYMMDDhhmmss(end.getTime())));

        String moreFilter = SQLFilterBuilder.build(traceSpanSchema, filters.stream().filter(filter -> !filter.getName().startsWith(SPAN_TAGS_PREFIX)));
        if (StringUtils.hasText(moreFilter)) {
            sqlBuilder.append(" AND ");
            sqlBuilder.append(moreFilter);
        }

        // build tag query
        SelectConditionStep<Record1<String>> tagQuery = buildTagQuery(start, end, filters);
        if (tagQuery != null) {
            sqlBuilder.append(" AND ");
            sqlBuilder.append(dslContext.renderInlined(summaryTable.TRACEID.in(tagQuery)));
        }

        sqlBuilder.append(StringUtils.format(" GROUP BY \"_timestamp\" ORDER BY \"_timestamp\"", timeBucket));

        return dslContext.fetch(sqlBuilder.toString()).intoMaps();
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

        // build tag query
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
                         // for spans coming from a same application instance, sort them by the start time
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
        try {
            span.tags = objectMapper.readValue(record.getTags(), new TypeReference<TreeMap<String, String>>() {
            });
        } catch (JsonProcessingException ignored) {
        }
        span.name = record.getName();
        return span;
    }

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
        try {
            span.tags = objectMapper.readValue(record.getTags(), new TypeReference<TreeMap<String, String>>() {
            });
        } catch (JsonProcessingException ignored) {
        }
        span.name = record.getName();
        return span;
    }
}
