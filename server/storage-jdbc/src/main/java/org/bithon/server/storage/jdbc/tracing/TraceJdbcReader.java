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
import org.bithon.server.common.utils.datetime.TimeSpan;
import org.bithon.server.metric.storage.DimensionCondition;
import org.bithon.server.storage.jdbc.jooq.Tables;
import org.bithon.server.storage.jdbc.jooq.tables.BithonTraceSpanSummary;
import org.bithon.server.storage.jdbc.jooq.tables.records.BithonTraceSpanRecord;
import org.bithon.server.storage.jdbc.jooq.tables.records.BithonTraceSpanSummaryRecord;
import org.bithon.server.storage.jdbc.utils.SQLFilterBuilder;
import org.bithon.server.tracing.sink.TraceSpan;
import org.bithon.server.tracing.storage.ITraceReader;
import org.jooq.DSLContext;
import org.jooq.SelectConditionStep;
import org.jooq.SelectSeekStep1;
import org.jooq.impl.DSL;

import java.sql.Timestamp;
import java.util.List;
import java.util.TreeMap;

/**
 * @author Frank Chen
 * @date 30/12/20
 */
public class TraceJdbcReader implements ITraceReader {
    private final DSLContext dslContext;
    private final ObjectMapper objectMapper;

    public TraceJdbcReader(DSLContext dslContext, ObjectMapper objectMapper) {
        this.dslContext = dslContext;
        this.objectMapper = objectMapper;
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
        SelectConditionStep<BithonTraceSpanSummaryRecord> sql = dslContext.selectFrom(summaryTable)
                                                                          .where(summaryTable.TIMESTAMP.ge(start))
                                                                          .and(summaryTable.TIMESTAMP.lt(end));

        sql = sql.and(SQLFilterBuilder.build(filters));

        //noinspection rawtypes
        SelectSeekStep1 sql2;
        if ("costTime".equals(orderBy)) {
            if ("desc".equals(order)) {
                sql2 = sql.orderBy(summaryTable.COSTTIMEMS.desc());
            } else {
                sql2 = sql.orderBy(summaryTable.COSTTIMEMS.asc());
            }
        } else {
            if ("desc".equals(order)) {
                sql2 = sql.orderBy(summaryTable.TIMESTAMP.desc());
            } else {
                sql2 = sql.orderBy(summaryTable.TIMESTAMP.asc());
            }
        }

        //noinspection unchecked
        return sql2.offset(pageNumber * pageSize)
                   .limit(pageSize)
                   .fetch(r -> this.toTraceSpan((BithonTraceSpanSummaryRecord) r));
    }

    @Override
    public int getTraceListSize(List<DimensionCondition> filters,
                                Timestamp start,
                                Timestamp end) {
        BithonTraceSpanSummary summaryTable = Tables.BITHON_TRACE_SPAN_SUMMARY;

        return (int) dslContext.select(DSL.count(summaryTable.TRACEID))
                               .from(summaryTable)
                               .where(summaryTable.TIMESTAMP.ge(start))
                               .and(summaryTable.TIMESTAMP.lt(end))
                               .and(SQLFilterBuilder.build(filters))
                               .fetchOne(0);
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
