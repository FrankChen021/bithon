/*
 *    Copyright 2020 bithon.cn
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

package com.sbss.bithon.server.tracing.storage.jdbc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbss.bithon.component.db.jooq.Indexes;
import com.sbss.bithon.component.db.jooq.Tables;
import com.sbss.bithon.component.db.jooq.tables.records.BithonTraceSpanRecord;
import com.sbss.bithon.server.tracing.handler.TraceSpan;
import com.sbss.bithon.server.tracing.storage.ITraceReader;
import com.sbss.bithon.server.tracing.storage.ITraceStorage;
import com.sbss.bithon.server.tracing.storage.ITraceWriter;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.impl.ThreadLocalTransactionProvider;
import org.springframework.dao.DuplicateKeyException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/4 8:34 下午
 */
@Slf4j
public class TraceJdbcStorage implements ITraceStorage {

    private final DSLContext dslContext;
    private final ObjectMapper objectMapper;

    public TraceJdbcStorage(DSLContext dslContext, ObjectMapper objectMapper) {
        this.dslContext = DSL.using(dslContext
                                        .configuration()
                                        .derive(new ThreadLocalTransactionProvider(dslContext.configuration()
                                                                                             .connectionProvider())));
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
    public TraceJdbcWriter createWriter() {
        return new TraceJdbcWriter();
    }

    @Override
    public ITraceReader createReader() {
        return new TraceJdbcReader();
    }

    private class TraceJdbcReader implements ITraceReader {
        @Override
        public List<TraceSpan> getTraceByTraceId(String traceId) {
            return dslContext.selectFrom(Tables.BITHON_TRACE_SPAN)
                             .where(Tables.BITHON_TRACE_SPAN.TRACEID.eq(traceId))
                             .fetch(this::toTraceSpan);
        }

        @Override
        public List<TraceSpan> getTraceList(String applicationName, int pageNumber, int pageSize) {
            return dslContext.selectFrom(Tables.BITHON_TRACE_SPAN)
                             .where(Tables.BITHON_TRACE_SPAN.APP_NAME.eq(applicationName))
                             .and(Tables.BITHON_TRACE_SPAN.PARENTSPANID.eq(""))
                             .orderBy(Tables.BITHON_TRACE_SPAN.TIMESTAMP.desc())
                             .offset(pageNumber * pageSize)
                             .limit(pageSize)
                             .fetch(this::toTraceSpan);
        }

        @Override
        public int getTraceListSize(String applicationName) {
            return dslContext.fetchCount(dslContext.selectFrom(Tables.BITHON_TRACE_SPAN)
                                                   .where(Tables.BITHON_TRACE_SPAN.APP_NAME.eq(applicationName))
                                                   .and(Tables.BITHON_TRACE_SPAN.PARENTSPANID.eq("")));
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

    private class TraceJdbcWriter implements ITraceWriter {
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
                    try {
                        dslContext.batchInsert(batchRecords).execute();
                    } catch (DuplicateKeyException e) {
                        log.error("Duplicated Span Records: {}", batchRecords);
                    }

                    leftSize -= thisBatch;
                }
            });
        }

        @Override
        public void close() {

        }
    }
}
