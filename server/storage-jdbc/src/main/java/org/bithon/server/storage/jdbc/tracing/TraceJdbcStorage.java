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

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.ThreadUtils;
import org.bithon.server.common.utils.datetime.TimeSpan;
import org.bithon.server.metric.storage.DimensionCondition;
import org.bithon.server.storage.jdbc.jooq.Tables;
import org.bithon.server.storage.jdbc.jooq.tables.BithonTraceSpanSummary;
import org.bithon.server.storage.jdbc.jooq.tables.records.BithonTraceSpanRecord;
import org.bithon.server.storage.jdbc.jooq.tables.records.BithonTraceSpanSummaryRecord;
import org.bithon.server.storage.jdbc.utils.SQLFilterBuilder;
import org.bithon.server.tracing.mapping.TraceIdMapping;
import org.bithon.server.tracing.sink.TraceSpan;
import org.bithon.server.tracing.storage.ITraceCleaner;
import org.bithon.server.tracing.storage.ITraceReader;
import org.bithon.server.tracing.storage.ITraceStorage;
import org.bithon.server.tracing.storage.ITraceWriter;
import org.bithon.server.tracing.storage.TraceStorageConfig;
import org.jooq.BatchBindStep;
import org.jooq.ContextTransactionalRunnable;
import org.jooq.DSLContext;
import org.jooq.SelectConditionStep;
import org.jooq.SelectSeekStep1;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.dao.DuplicateKeyException;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/4 8:34 下午
 */
@Slf4j
@JsonTypeName("jdbc")
public class TraceJdbcStorage implements ITraceStorage {

    protected final DSLContext dslContext;
    protected final ObjectMapper objectMapper;
    protected final TraceStorageConfig config;

    @JsonCreator
    public TraceJdbcStorage(@JacksonInject(useInput = OptBoolean.FALSE) DSLContext dslContext,
                            @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper objectMapper,
                            @JacksonInject(useInput = OptBoolean.FALSE) TraceStorageConfig storageConfig) {
        this.dslContext = dslContext;
        this.objectMapper = objectMapper;
        this.config = storageConfig;
    }

    @Override
    public void initialize() {
        dslContext.createTableIfNotExists(Tables.BITHON_TRACE_SPAN)
                  .columns(Tables.BITHON_TRACE_SPAN.fields())
                  .indexes(Tables.BITHON_TRACE_SPAN.getIndexes())
                  .execute();
        dslContext.createTableIfNotExists(Tables.BITHON_TRACE_SPAN_SUMMARY)
                  .columns(Tables.BITHON_TRACE_SPAN_SUMMARY.fields())
                  .indexes(Tables.BITHON_TRACE_SPAN_SUMMARY.getIndexes())
                  .execute();
        dslContext.createTableIfNotExists(Tables.BITHON_TRACE_MAPPING)
                  .columns(Tables.BITHON_TRACE_MAPPING.fields())
                  .indexes(Tables.BITHON_TRACE_MAPPING.getIndexes())
                  .execute();
    }

    @Override
    public ITraceWriter createWriter() {
        return new BatchWriter(new TraceJdbcWriter(dslContext, objectMapper), config);
    }

    @Override
    public ITraceReader createReader() {
        return new TraceJdbcReader();
    }

    @Override
    public ITraceCleaner createCleaner() {
        return beforeTimestamp -> {
            Timestamp before = new Timestamp(beforeTimestamp);

            dslContext.deleteFrom(Tables.BITHON_TRACE_SPAN)
                      .where(Tables.BITHON_TRACE_SPAN.TRACEID
                                 .in(dslContext.selectDistinct(Tables.BITHON_TRACE_SPAN.TRACEID)
                                               .where(Tables.BITHON_TRACE_SPAN.TIMESTAMP.le(before))))
                      .execute();

            dslContext.deleteFrom(Tables.BITHON_TRACE_MAPPING)
                      .where(Tables.BITHON_TRACE_MAPPING.TIMESTAMP.le(before))
                      .execute();
        };
    }

    /**
     * The batch writer here may not be a perfect design.
     * It can be put at the message handler layer so that all writers can gain batch capability.
     * For metrics have already been aggregated at agent side it's TPS is not very high, So it's not a pain point.
     * <p>
     * But for trace, there's no such aggregation layer which may result in high QPS of insert.
     * Since I'm not focusing on the implementation detail now, perfect solution is left in the future.
     */
    protected static class BatchWriter implements ITraceWriter {
        private final List<TraceSpan> traceSpans = new ArrayList<>();
        private final List<TraceIdMapping> traceIdMappings = new ArrayList<>();

        private final ITraceWriter writer;
        private final TraceStorageConfig config;
        private final ScheduledExecutorService executor;

        public BatchWriter(ITraceWriter writer, TraceStorageConfig config) {
            this.writer = writer;
            this.config = config;
            this.executor = Executors.newSingleThreadScheduledExecutor(new ThreadUtils.NamedThreadFactory("trace-batch-writer"));
            this.executor.scheduleWithFixedDelay(this::flush, 5, 1, TimeUnit.SECONDS);
        }

        @Override
        public void writeSpans(Collection<TraceSpan> spans) {
            synchronized (this) {
                this.traceSpans.addAll(spans);
            }
            if (traceSpans.size() > config.getBatchSize()) {
                flushSpans();
            }
        }

        @Override
        public void writeMappings(Collection<TraceIdMapping> mappings) {
            synchronized (this) {
                this.traceIdMappings.addAll(mappings);
            }
            if (this.traceIdMappings.size() > config.getBatchSize()) {
                flushMappings();
            }
        }

        private void flush() {
            flushSpans();
            flushMappings();
        }

        private void flushSpans() {
            List<TraceSpan> spans;
            synchronized (this) {
                spans = new ArrayList<>(traceSpans);
                traceSpans.clear();
            }

            if (!spans.isEmpty()) {
                try {
                    log.debug("Flushing [{}] spans into storage...", spans.size());
                    this.writer.writeSpans(spans);
                } catch (IOException e) {
                    log.info("Exception when flushing spans into storage", e);
                }
            }
        }

        private void flushMappings() {
            List<TraceIdMapping> mappings;
            synchronized (this) {
                mappings = new ArrayList<>(traceIdMappings);
                traceIdMappings.clear();
            }

            if (!mappings.isEmpty()) {
                try {
                    log.debug("Flushing [{}] trace id mappings into storage...", mappings.size());
                    this.writer.writeMappings(mappings);
                } catch (IOException e) {
                    log.info("Exception when flushing id mapping into storage", e);
                }
            }
        }

        @Override
        public void close() {
            log.info("Shutting down trace batch writer...");
            // shutdown and wait for current scheduler to close
            this.executor.shutdown();
            try {
                if (!this.executor.awaitTermination(20, TimeUnit.SECONDS)) {
                    log.warn("Timeout when shutdown trace batch writer");
                }
            } catch (InterruptedException ignored) {
            }

            // flush all data to see if there's any more data
            flush();

            // close underlying writer at last
            this.writer.close();
        }
    }

    private class TraceJdbcReader implements ITraceReader {
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

    protected static class TraceJdbcWriter implements ITraceWriter {

        private final DSLContext dslContext;
        private final ObjectMapper objectMapper;

        public TraceJdbcWriter(DSLContext dslContext, ObjectMapper objectMapper) {
            this.dslContext = dslContext;
            this.objectMapper = objectMapper;
        }

        private String getOrDefault(String v) {
            return v == null ? "" : v;
        }

        protected boolean isTransactionSupported() {
            return true;
        }

        @Override
        public void writeSpans(Collection<TraceSpan> traceSpans) {
            List<TraceSpan> rootSpans = traceSpans.stream().filter((span) -> "SERVER".equals(span.getKind())).collect(Collectors.toList());

            ContextTransactionalRunnable runnable = () -> {
                if (!rootSpans.isEmpty()) {
                    writeSpans(rootSpans, Tables.BITHON_TRACE_SPAN_SUMMARY);
                }

                writeSpans(traceSpans, Tables.BITHON_TRACE_SPAN);
            };
            if (isTransactionSupported()) {
                dslContext.transaction(runnable);
            } else {
                try {
                    runnable.run();
                } catch (Throwable e) {
                    log.error("Exception when write spans", e);
                }
            }
        }

        protected void writeSpans(Collection<TraceSpan> traceSpans, Table<?> table) {
            BatchBindStep step = dslContext.batch(dslContext.insertInto(table,
                                                                        Tables.BITHON_TRACE_SPAN.TIMESTAMP,
                                                                        Tables.BITHON_TRACE_SPAN.APPNAME,
                                                                        Tables.BITHON_TRACE_SPAN.INSTANCENAME,
                                                                        Tables.BITHON_TRACE_SPAN.TRACEID,
                                                                        Tables.BITHON_TRACE_SPAN.SPANID,
                                                                        Tables.BITHON_TRACE_SPAN.PARENTSPANID,
                                                                        Tables.BITHON_TRACE_SPAN.NAME,
                                                                        Tables.BITHON_TRACE_SPAN.CLAZZ,
                                                                        Tables.BITHON_TRACE_SPAN.METHOD,
                                                                        Tables.BITHON_TRACE_SPAN.KIND,
                                                                        Tables.BITHON_TRACE_SPAN.STARTTIMEUS,
                                                                        Tables.BITHON_TRACE_SPAN.ENDTIMEUS,
                                                                        Tables.BITHON_TRACE_SPAN.COSTTIMEMS,
                                                                        Tables.BITHON_TRACE_SPAN.TAGS,
                                                                        Tables.BITHON_TRACE_SPAN.NORMALIZEDURL,
                                                                        Tables.BITHON_TRACE_SPAN.STATUS)
                                                            .values(null,
                                                                    null, //app name
                                                                    null, // instance
                                                                    null, //trace id
                                                                    null, // span id
                                                                    null, // parent id
                                                                    null, // name
                                                                    null, // class
                                                                    null, // method
                                                                    null, // kind
                                                                    null, // start time
                                                                    null, // end time
                                                                    (Long) null, // cost time
                                                                    null,   // tags
                                                                    null,   // normalized url
                                                                    null    // status
                                                            ));

            for (TraceSpan span : traceSpans) {
                String tags;
                try {
                    tags = span.getTags() == null ? "{}" : objectMapper.writeValueAsString(span.tags);
                } catch (IOException ignored) {
                    tags = "{}";
                }
                step.bind(new Timestamp(span.startTime / 1000),
                          span.appName,
                          span.instanceName,
                          span.traceId,
                          span.spanId,
                          span.parentSpanId,
                          getOrDefault(span.name),
                          getOrDefault(span.clazz),
                          getOrDefault(span.method),
                          getOrDefault(span.kind),
                          span.startTime,
                          span.endTime,
                          span.costTime,
                          tags,
                          span.getNormalizeUri(),
                          span.getStatus());
            }
            step.execute();
        }

        @Override
        public void writeMappings(Collection<TraceIdMapping> mappings) {
            BatchBindStep step = dslContext.batch(dslContext.insertInto(Tables.BITHON_TRACE_MAPPING,
                                                                        Tables.BITHON_TRACE_MAPPING.TRACE_ID,
                                                                        Tables.BITHON_TRACE_MAPPING.USER_TX_ID,
                                                                        Tables.BITHON_TRACE_SPAN.TIMESTAMP).values((String) null, null, null));

            for (TraceIdMapping mapping : mappings) {
                step.bind(mapping.getTraceId(), mapping.getUserId(), new Timestamp(mapping.getTimestamp()));
            }
            try {
                step.execute();
            } catch (DuplicateKeyException ignored) {
                // for database like H2
            }
        }
    }
}
