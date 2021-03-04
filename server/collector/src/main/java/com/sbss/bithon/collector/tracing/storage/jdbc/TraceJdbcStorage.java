package com.sbss.bithon.collector.tracing.storage.jdbc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbss.bithon.collector.tracing.storage.ITraceReader;
import com.sbss.bithon.collector.tracing.storage.ITraceStorage;
import com.sbss.bithon.collector.tracing.storage.ITraceWriter;
import com.sbss.bithon.collector.tracing.storage.TraceSpan;
import com.sbss.bithon.component.db.jooq.Indexes;
import com.sbss.bithon.component.db.jooq.Tables;
import com.sbss.bithon.component.db.jooq.tables.records.BithonTraceSpanRecord;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.impl.ThreadLocalTransactionProvider;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/4 8:34 下午
 */
public class TraceJdbcStorage implements ITraceStorage {

    private final DSLContext dslContext;
    private final ObjectMapper objectMapper;

    public TraceJdbcStorage(DSLContext dslContext, ObjectMapper objectMapper) {
        this.dslContext = DSL.using(dslContext
                                        .configuration()
                                        .derive(new ThreadLocalTransactionProvider(dslContext.configuration().connectionProvider())));
        this.objectMapper = objectMapper;

        dslContext.createTableIfNotExists(Tables.BITHON_TRACE_SPAN)
            .columns(Tables.BITHON_TRACE_SPAN.ID,
                     Tables.BITHON_TRACE_SPAN.TIMESTAMP,
                     Tables.BITHON_TRACE_SPAN.APP_NAME,
                     Tables.BITHON_TRACE_SPAN.INSTANCE_NAME,
                     Tables.BITHON_TRACE_SPAN.NAME,
                     Tables.BITHON_TRACE_SPAN.CLAZZ,
                     Tables.BITHON_TRACE_SPAN.METHOD,
                     Tables.BITHON_TRACE_SPAN.KIND,
                     Tables.BITHON_TRACE_SPAN.TRACEID,
                     Tables.BITHON_TRACE_SPAN.SPANID,
                     Tables.BITHON_TRACE_SPAN.COSTTIME,
                     Tables.BITHON_TRACE_SPAN.PARENTSPANID,
                     Tables.BITHON_TRACE_SPAN.TAGS)
            .indexes(Indexes.BITHON_TRACE_SPAN_IDX_KEY)
            .execute();
    }

    @Override
    public TraceWriter createWriter() {
        return new TraceWriter();
    }

    @Override
    public ITraceReader createReader() {
        return new TraceReader();
    }

    private class TraceReader implements ITraceReader {
        @Override
        public List<TraceSpan> getTraceByTraceId(String traceId) {
            return dslContext.selectFrom(Tables.BITHON_TRACE_SPAN)
                .where(Tables.BITHON_TRACE_SPAN.TRACEID.eq(traceId))
                .fetch(this::toTraceSpan);
        }

        @Override
        public List<TraceSpan> getTraceList(String applicationName) {
            return dslContext.selectFrom(Tables.BITHON_TRACE_SPAN)
                .where(Tables.BITHON_TRACE_SPAN.APP_NAME.eq(applicationName))
                .and(Tables.BITHON_TRACE_SPAN.PARENTSPANID.eq(""))
                .orderBy(Tables.BITHON_TRACE_SPAN.TIMESTAMP.desc())
                .fetch(this::toTraceSpan);
        }

        @Override
        public List<TraceSpan> getTraceByParentSpanId(String parentSpanId) {
            return dslContext.selectFrom(Tables.BITHON_TRACE_SPAN)
                .where(Tables.BITHON_TRACE_SPAN.PARENTSPANID.eq(parentSpanId))
                .orderBy(Tables.BITHON_TRACE_SPAN.TIMESTAMP.asc())
                .fetch(this::toTraceSpan);
        }

        private TraceSpan toTraceSpan(BithonTraceSpanRecord record) {
            TraceSpan span = new TraceSpan();
            span.appName = record.getAppName();
            span.instanceName = record.getInstanceName();
            span.traceId = record.getTraceid();
            span.spanId = record.getSpanid();
            span.parentSpanId = record.getParentspanid();
            span.startTime = record.getTimestamp().getTime();
            span.costTime = record.getCosttime();
            span.name = record.getName();
            span.kind = record.getKind();
            span.method = record.getMethod();
            span.clazz = record.getClazz();
            try {
                span.tags = objectMapper.readValue(record.getTags(), new TypeReference<Map<String, String>>() {
                });
            } catch (JsonProcessingException ignored) {
            }
            span.name = record.getName();
            return span;
        }
    }

    private class TraceWriter implements ITraceWriter {
        static final int BATCH_SIZE = 10;

        @Override
        public void write(List<TraceSpan> traceSpans) {

            dslContext.transaction((configuration) -> {
                List<BithonTraceSpanRecord> batchRecords = new ArrayList<>();

                int index = 0;
                int leftSize = traceSpans.size();

                while (leftSize > 0) {
                    int thisBatch = Math.min(BATCH_SIZE, leftSize);

                    batchRecords.clear();
                    for (int i = 0; i < thisBatch; i++, index++) {
                        TraceSpan span = traceSpans.get(index);

                        BithonTraceSpanRecord spanRecord = new BithonTraceSpanRecord();
                        spanRecord.setAppName(span.appName);
                        spanRecord.setInstanceName(span.instanceName);
                        spanRecord.setTimestamp(new Timestamp(span.startTime / 1000));
                        spanRecord.setTraceid(span.traceId);
                        spanRecord.setSpanid(span.spanId);
                        spanRecord.setParentspanid(span.parentSpanId);
                        spanRecord.setCosttime(span.costTime);
                        spanRecord.setName(span.name);
                        spanRecord.setClazz(span.clazz == null ? "" : span.clazz);
                        spanRecord.setMethod(span.method == null ? "" : span.method);
                        spanRecord.setKind(span.kind);
                        spanRecord.setTags(span.tags == null ? "{}" : objectMapper.writeValueAsString(span.tags));
                        batchRecords.add(spanRecord);
                    }
                    dslContext.batchInsert(batchRecords).execute();

                    leftSize -= thisBatch;
                }
            });
        }

        @Override
        public void close() {

        }
    }
}
