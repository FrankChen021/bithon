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
import org.bithon.server.storage.jdbc.jooq.Indexes;
import org.bithon.server.storage.jdbc.jooq.Tables;
import org.bithon.server.storage.jdbc.jooq.tables.records.BithonTraceSpanRecord;
import org.bithon.server.tracing.handler.TraceSpan;
import org.bithon.server.tracing.storage.ITraceCleaner;
import org.bithon.server.tracing.storage.ITraceReader;
import org.bithon.server.tracing.storage.ITraceStorage;
import org.bithon.server.tracing.storage.ITraceWriter;
import org.jooq.DSLContext;
import org.springframework.dao.DuplicateKeyException;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
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

    @JsonCreator
    public TraceJdbcStorage(@JacksonInject(useInput = OptBoolean.FALSE) DSLContext dslContext,
                            @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper objectMapper) {
        this.dslContext = dslContext;
        this.objectMapper = objectMapper;
    }

    @Override
    public void initialize() {
        dslContext.createTableIfNotExists(Tables.BITHON_TRACE_SPAN)
                  .columns(Tables.BITHON_TRACE_SPAN.TIMESTAMP,
                           Tables.BITHON_TRACE_SPAN.APPNAME,
                           Tables.BITHON_TRACE_SPAN.INSTANCENAME,
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

    @Override
    public ITraceCleaner createCleaner() {
        return beforeTimestamp -> dslContext.deleteFrom(Tables.BITHON_TRACE_SPAN)
                                            .where(Tables.BITHON_TRACE_SPAN.TRACEID
                                                       .in(dslContext.selectDistinct(Tables.BITHON_TRACE_SPAN.TRACEID)
                                                                     .where(Tables.BITHON_TRACE_SPAN.TIMESTAMP.le(new Timestamp(
                                                                         beforeTimestamp)))));
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
                             .where(Tables.BITHON_TRACE_SPAN.APPNAME.eq(applicationName))
                             .and(Tables.BITHON_TRACE_SPAN.PARENTSPANID.eq(""))
                             .orderBy(Tables.BITHON_TRACE_SPAN.TIMESTAMP.desc())
                             .offset(pageNumber * pageSize)
                             .limit(pageSize)
                             .fetch(this::toTraceSpan);
        }

        @Override
        public int getTraceListSize(String applicationName) {
            return dslContext.fetchCount(dslContext.selectFrom(Tables.BITHON_TRACE_SPAN)
                                                   .where(Tables.BITHON_TRACE_SPAN.APPNAME.eq(applicationName))
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
            span.appName = record.getAppname();
            span.instanceName = record.getInstancename();
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
        @Override
        public void write(List<TraceSpan> traceSpans) {
            List<BithonTraceSpanRecord> records = traceSpans.stream().map((span) -> {
                BithonTraceSpanRecord spanRecord = new BithonTraceSpanRecord();
                spanRecord.setAppname(span.appName);
                spanRecord.setInstancename(span.instanceName);
                spanRecord.setTimestamp(new Timestamp(span.startTime / 1000));
                spanRecord.setTraceid(span.traceId);
                spanRecord.setSpanid(span.spanId);
                spanRecord.setParentspanid(span.parentSpanId);
                spanRecord.setCosttime(span.costTime);
                spanRecord.setName(span.name == null ? "" : span.name);
                spanRecord.setClazz(span.clazz == null ? "" : span.clazz);
                spanRecord.setMethod(span.method == null ? "" : span.method);
                spanRecord.setKind(span.kind == null ? "" : span.kind);
                try {
                    spanRecord.setTags(span.tags == null ? "{}" : objectMapper.writeValueAsString(span.tags));
                } catch (IOException ignored) {
                }
                return spanRecord;
            }).collect(Collectors.toList());
            try {
                dslContext.batchInsert(records).execute();
            } catch (DuplicateKeyException e) {
                log.error("Duplicated Span Records: {}", records);
            }
        }
    }
}
