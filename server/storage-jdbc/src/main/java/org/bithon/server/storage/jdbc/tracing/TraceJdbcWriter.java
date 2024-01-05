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
import org.bithon.component.commons.tracing.SpanKind;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.server.storage.jdbc.common.jooq.Tables;
import org.bithon.server.storage.tracing.ITraceWriter;
import org.bithon.server.storage.tracing.TraceSpan;
import org.bithon.server.storage.tracing.TraceStorageConfig;
import org.bithon.server.storage.tracing.index.TagIndex;
import org.bithon.server.storage.tracing.mapping.TraceIdMapping;
import org.jooq.BatchBindStep;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.TransactionalRunnable;
import org.jooq.exception.DataAccessException;
import org.springframework.dao.DuplicateKeyException;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 30/12/20
 */
@Slf4j
public class TraceJdbcWriter implements ITraceWriter {

    private final DSLContext dslContext;
    private final ObjectMapper objectMapper;
    private final TraceStorageConfig traceStorageConfig;

    public TraceJdbcWriter(DSLContext dslContext, ObjectMapper objectMapper, TraceStorageConfig traceStorageConfig) {
        this.dslContext = dslContext;
        this.objectMapper = objectMapper;
        this.traceStorageConfig = traceStorageConfig;
    }

    private String getOrDefault(String v) {
        return v == null ? "" : v;
    }

    protected boolean isTransactionSupported() {
        return true;
    }

    protected Object toTagStore(Map<String, String> tag) {
        try {
            return objectMapper.writeValueAsString(tag);
        } catch (JsonProcessingException ignored) {
            return "{}";
        }
    }

    private void writeSpans(Collection<TraceSpan> traceSpans, Table<?> table) throws Throwable {
        if (traceSpans.isEmpty()) {
            return;
        }
        String insertTemplate = dslContext.render(dslContext.insertInto(table,
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
                                                                        Tables.BITHON_TRACE_SPAN.ATTRIBUTES,
                                                                        Tables.BITHON_TRACE_SPAN.NORMALIZEDURL,
                                                                        Tables.BITHON_TRACE_SPAN.STATUS)
                                                            .values((LocalDateTime) null,
                                                                    //app name
                                                                    null,
                                                                    // instance
                                                                    null,
                                                                    //trace id
                                                                    null,
                                                                    // span id
                                                                    null,
                                                                    // parent id
                                                                    null,
                                                                    // name
                                                                    null,
                                                                    // class
                                                                    null,
                                                                    // method
                                                                    null,
                                                                    // kind
                                                                    null,
                                                                    // start time
                                                                    null,
                                                                    // end time
                                                                    null,
                                                                    // cost time
                                                                    null,
                                                                    // tags
                                                                    null,
                                                                    // normalized url
                                                                    null,
                                                                    // status
                                                                    null
                                                            ));
        try {
            dslContext.connection(conn -> {
                try (PreparedStatement stmt = conn.prepareStatement(insertTemplate)) {
                    for (TraceSpan span : traceSpans) {
                        stmt.setObject(1, new Timestamp(span.startTime / 1000));
                        stmt.setObject(2, span.appName);
                        stmt.setObject(3, span.instanceName);
                        stmt.setObject(4, span.traceId);
                        stmt.setObject(5, span.spanId);
                        stmt.setObject(6, span.parentSpanId);
                        stmt.setObject(7, getOrDefault(span.name));
                        stmt.setObject(8, getOrDefault(span.clazz));
                        stmt.setObject(9, getOrDefault(span.method));
                        stmt.setObject(10, getOrDefault(span.kind));
                        stmt.setObject(11, span.startTime);
                        stmt.setObject(12, span.endTime);
                        stmt.setObject(13, span.costTime);
                        stmt.setObject(14, toTagStore(span.getTags()));
                        stmt.setObject(15, span.getNormalizedUri());
                        stmt.setObject(16, span.getStatus());
                        stmt.addBatch();
                    }
                    stmt.executeBatch();
                }
            });
        } catch (DataAccessException e) {
            // Re-throw the caused exception for more clear stack trace
            // In such case, the caused exception is not NULL.
            throw e.getCause();
        }
    }

    private void writeMappings(Collection<TraceIdMapping> mappings) {
        if (CollectionUtils.isEmpty(mappings)) {
            return;
        }

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

    @Override
    public void write(Collection<TraceSpan> spans,
                      Collection<TraceIdMapping> mappings,
                      Collection<TagIndex> tagIndices) {

        TransactionalRunnable runnable = (configuration) -> {
            List<TraceSpan> summary = spans.stream()
                                           .filter((span) -> SpanKind.isRootSpan(span.getKind()))
                                           .collect(Collectors.toList());
            this.writeSpans(summary, Tables.BITHON_TRACE_SPAN_SUMMARY);
            this.writeSpans(spans, Tables.BITHON_TRACE_SPAN);
            this.writeMappings(mappings);
            this.writeTagIndices(tagIndices);
        };
        try {
            if (isTransactionSupported()) {
                dslContext.transaction(runnable);
            } else {
                runnable.run(dslContext.configuration());
            }
        } catch (Throwable e) {
            log.error("Exception when write spans", e);
        }
    }

    private void writeTagIndices(Collection<TagIndex> tagIndices) {
        if (CollectionUtils.isEmpty(tagIndices)) {
            return;
        }

        BatchBindStep sql = dslContext.batch(dslContext.insertInto(Tables.BITHON_TRACE_SPAN_TAG_INDEX,
                                                                   Tables.BITHON_TRACE_SPAN_TAG_INDEX.TIMESTAMP,
                                                                   Tables.BITHON_TRACE_SPAN_TAG_INDEX.F1,
                                                                   Tables.BITHON_TRACE_SPAN_TAG_INDEX.F2,
                                                                   Tables.BITHON_TRACE_SPAN_TAG_INDEX.F3,
                                                                   Tables.BITHON_TRACE_SPAN_TAG_INDEX.F4,
                                                                   Tables.BITHON_TRACE_SPAN_TAG_INDEX.F5,
                                                                   Tables.BITHON_TRACE_SPAN_TAG_INDEX.F6,
                                                                   Tables.BITHON_TRACE_SPAN_TAG_INDEX.F7,
                                                                   Tables.BITHON_TRACE_SPAN_TAG_INDEX.F8,
                                                                   Tables.BITHON_TRACE_SPAN_TAG_INDEX.F9,
                                                                   Tables.BITHON_TRACE_SPAN_TAG_INDEX.F10,
                                                                   Tables.BITHON_TRACE_SPAN_TAG_INDEX.F11,
                                                                   Tables.BITHON_TRACE_SPAN_TAG_INDEX.F12,
                                                                   Tables.BITHON_TRACE_SPAN_TAG_INDEX.F13,
                                                                   Tables.BITHON_TRACE_SPAN_TAG_INDEX.F14,
                                                                   Tables.BITHON_TRACE_SPAN_TAG_INDEX.F15,
                                                                   Tables.BITHON_TRACE_SPAN_TAG_INDEX.F16,
                                                                   Tables.BITHON_TRACE_SPAN_TAG_INDEX.TRACEID)
                                                       .values((LocalDateTime) null,
                                                               null,
                                                               null,
                                                               null,
                                                               null,
                                                               null,
                                                               null,
                                                               null,
                                                               null,
                                                               null,
                                                               null,
                                                               null,
                                                               null,
                                                               null,
                                                               null,
                                                               null,
                                                               null,
                                                               null));

        Map<String, Object[]> batchValues = new HashMap<>(tagIndices.size());
        for (TagIndex index : tagIndices) {
            Object[] values = batchValues.computeIfAbsent(index.getTraceId(), k -> {
                Object[] r = new Object[18];
                r[0] = new Timestamp(index.getTimestamp());
                r[17] = index.getTraceId();
                return r;
            });

            int fieldIndex = this.traceStorageConfig.getIndexes().getColumnPos(index.getName());
            if (fieldIndex == 0 || fieldIndex >= values.length) {
                // TODO: log error
                continue;
            }
            values[fieldIndex] = index.getValue();
        }
        batchValues.forEach((traceId, values) -> sql.bind(values));

        try {
            sql.execute();
        } catch (DuplicateKeyException ignored) {
            // for database like H2
        }
    }
}
