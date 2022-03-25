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
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.common.utils.datetime.TimeSpan;
import org.bithon.server.metric.storage.DimensionCondition;
import org.bithon.server.storage.jdbc.jooq.Tables;
import org.bithon.server.storage.jdbc.jooq.tables.BithonTraceSpanSummary;
import org.bithon.server.storage.jdbc.jooq.tables.records.BithonTraceSpanRecord;
import org.bithon.server.storage.jdbc.jooq.tables.records.BithonTraceSpanSummaryRecord;
import org.bithon.server.storage.jdbc.utils.SQLFilterBuilder;
import org.bithon.server.tracing.TraceConfig;
import org.bithon.server.tracing.TraceDataSourceSchema;
import org.bithon.server.tracing.sink.TraceSpan;
import org.bithon.server.tracing.storage.ITraceReader;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.SelectConditionStep;
import org.jooq.SelectSeekStep1;
import org.jooq.impl.DSL;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

/**
 * @author Frank Chen
 * @date 30/12/20
 */
public class TraceJdbcReader implements ITraceReader {

    public static final String SPAN_TAGS_PREFIX = "tags.";
    private final DSLContext dslContext;
    private final ObjectMapper objectMapper;
    private final TraceConfig traceConfig;

    public TraceJdbcReader(DSLContext dslContext, ObjectMapper objectMapper, TraceConfig traceConfig) {
        this.dslContext = dslContext;
        this.objectMapper = objectMapper;
        this.traceConfig = traceConfig;
    }

    @Override
    public List<TraceSpan> getTraceByTraceId(String traceId, TimeSpan start, TimeSpan end) {
        SelectConditionStep<BithonTraceSpanRecord> sql = dslContext.selectFrom(Tables.BITHON_TRACE_SPAN)
                                                                   .where(Tables.BITHON_TRACE_SPAN.TRACEID.eq(traceId));
        if (start != null) {
            sql = sql.and(Tables.BITHON_TRACE_SPAN.TIMESTAMP.ge(start.toTimestamp()));
        }
        if (end != null) {
            sql = sql.and(Tables.BITHON_TRACE_SPAN.TIMESTAMP.lt(end.toTimestamp()));
        }

        // for spans coming from a same application instance, sort them by the start time
        return sql.orderBy(Tables.BITHON_TRACE_SPAN.TIMESTAMP.asc(),
                           Tables.BITHON_TRACE_SPAN.INSTANCENAME,
                           Tables.BITHON_TRACE_SPAN.STARTTIMEUS)
                  .fetch(this::toTraceSpan);
    }

    @Override
    public List<TraceSpan> getTraceList(List<DimensionCondition> filters,
                                        Timestamp start,
                                        Timestamp end,
                                        String orderBy,
                                        String order,
                                        int pageNumber,
                                        int pageSize) {
        BithonTraceSpanSummary summaryTable = Tables.BITHON_TRACE_SPAN_SUMMARY;
        SelectConditionStep<BithonTraceSpanSummaryRecord> listQuery = dslContext.selectFrom(summaryTable)
                                                                                .where(summaryTable.TIMESTAMP.ge(start))
                                                                                .and(summaryTable.TIMESTAMP.lt(end));

        String moreFilter = SQLFilterBuilder.build(TraceDataSourceSchema.getTraceSpanSchema(),
                                                   filters.stream().filter(filter -> !filter.getDimension().startsWith(SPAN_TAGS_PREFIX)));
        if (StringUtils.hasText(moreFilter)) {
            listQuery = listQuery.and(moreFilter);
        }

        // build tag query
        SelectConditionStep<Record1<String>> tagQuery = buildTagQuery(start, end, filters);
        if (tagQuery != null) {
            listQuery = listQuery.and(summaryTable.TRACEID.in(tagQuery));
        }

        //noinspection rawtypes
        SelectSeekStep1 orderedListQuery;
        if ("costTime".equals(orderBy)) {
            if ("desc".equals(order)) {
                orderedListQuery = listQuery.orderBy(summaryTable.COSTTIMEMS.desc());
            } else {
                orderedListQuery = listQuery.orderBy(summaryTable.COSTTIMEMS.asc());
            }
        } else {
            if ("desc".equals(order)) {
                orderedListQuery = listQuery.orderBy(summaryTable.TIMESTAMP.desc());
            } else {
                orderedListQuery = listQuery.orderBy(summaryTable.TIMESTAMP.asc());
            }
        }

        //noinspection unchecked
        return orderedListQuery.offset(pageNumber * pageSize)
                               .limit(pageSize)
                               .fetch(r -> this.toTraceSpan((BithonTraceSpanSummaryRecord) r));
    }

    @Override
    public int getTraceListSize(List<DimensionCondition> filters,
                                Timestamp start,
                                Timestamp end) {
        BithonTraceSpanSummary summaryTable = Tables.BITHON_TRACE_SPAN_SUMMARY;

        SelectConditionStep<Record1<Integer>> countQuery = dslContext.select(DSL.count(summaryTable.TRACEID))
                                                                     .from(summaryTable)
                                                                     .where(summaryTable.TIMESTAMP.ge(start))
                                                                     .and(summaryTable.TIMESTAMP.lt(end));

        String moreFilter = SQLFilterBuilder.build(TraceDataSourceSchema.getTraceSpanSchema(),
                                                   filters.stream().filter(filter -> !filter.getDimension().startsWith(SPAN_TAGS_PREFIX)));
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
    public String getTraceIdByMapping(String id) {
        return dslContext.select(Tables.BITHON_TRACE_MAPPING.TRACE_ID)
                         .from(Tables.BITHON_TRACE_MAPPING)
                         .where(Tables.BITHON_TRACE_MAPPING.USER_TX_ID.eq(id))
                         .limit(1)
                         .fetchOne(Tables.BITHON_TRACE_MAPPING.TRACE_ID);
    }

    /**
     * TODO:
     *  1. If a given tag name is not in the index list, the query on that name should fall back to BITHON_TRACE_SPAN table to match
     *  2. For multiple tags which are not in the same group, nested query should be applied
     */
    private SelectConditionStep<Record1<String>> buildTagQuery(Timestamp start, Timestamp end, Collection<DimensionCondition> filters) {
        SelectConditionStep<Record1<String>> tagQuery = null;

        for (DimensionCondition filter : filters) {
            if (!filter.getDimension().startsWith(SPAN_TAGS_PREFIX)) {
                continue;
            }
            String tagName = filter.getDimension().substring(SPAN_TAGS_PREFIX.length());
            if (!StringUtils.hasText(tagName)) {
                throw new RuntimeException(StringUtils.format("Wrong tag name [%s]", filter.getDimension()));
            }

            Preconditions.checkNotNull(this.traceConfig.getIndexes(), "No index configured for 'tags' attribute.");

            int tagIndex = this.traceConfig.getIndexes().getColumnPos(tagName);
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
                                     .where(Tables.BITHON_TRACE_SPAN_TAG_INDEX.TIMESTAMP.ge(start))
                                     .and(Tables.BITHON_TRACE_SPAN_TAG_INDEX.TIMESTAMP.lt(end));
            }
            tagQuery = tagQuery.and(filter.getMatcher().accept(new SQLFilterBuilder("f" + tagIndex)));
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
        span.normalizeUri = record.getNormalizedurl();
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
        span.normalizeUri = record.getNormalizedurl();
        try {
            span.tags = objectMapper.readValue(record.getTags(), new TypeReference<TreeMap<String, String>>() {
            });
        } catch (JsonProcessingException ignored) {
        }
        span.name = record.getName();
        return span;
    }
}
