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

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.server.storage.jdbc.jooq.Tables;
import org.bithon.server.tracing.index.TagIndex;
import org.bithon.server.tracing.mapping.TraceIdMapping;
import org.bithon.server.tracing.sink.TraceSpan;
import org.bithon.server.tracing.storage.ITraceWriter;
import org.jooq.BatchBindStep;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.TransactionalRunnable;
import org.springframework.dao.DuplicateKeyException;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Frank Chen
 * @date 30/12/20
 */
@Slf4j
public class TraceJdbcWriter implements ITraceWriter {

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

    private void writeSpans(Collection<TraceSpan> traceSpans, Table<?> table) {
        if (traceSpans.isEmpty()) {
            return;
        }

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
                                                        .values((Timestamp) null,
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
                                                                null, // cost time
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
            List<TraceSpan> summary = spans.stream().filter((span) -> "SERVER".equals(span.getKind())).collect(Collectors.toList());
            this.writeSpans(summary, Tables.BITHON_TRACE_SPAN_SUMMARY);
            this.writeSpans(spans, Tables.BITHON_TRACE_SPAN);
            this.writeMappings(mappings);
            this.writeTagIndices(tagIndices);
        };
        if (isTransactionSupported()) {
            dslContext.transaction(runnable);
        } else {
            try {
                runnable.run(dslContext.configuration());
            } catch (Throwable e) {
                log.error("Exception when write spans", e);
            }
        }
    }

    private void writeTagIndices(Collection<TagIndex> tagIndices) {
        if (CollectionUtils.isEmpty(tagIndices)) {
            return;
        }

        BatchBindStep sql = dslContext.batch(dslContext.insertInto(Tables.BITHON_TRACE_SPAN_TAG_INDEX,
                                                                   Tables.BITHON_TRACE_SPAN_TAG_INDEX.TIMESTAMP,
                                                                   Tables.BITHON_TRACE_SPAN_TAG_INDEX.NAME,
                                                                   Tables.BITHON_TRACE_SPAN_TAG_INDEX.VALUE,
                                                                   Tables.BITHON_TRACE_SPAN_TAG_INDEX.TRACEID)
                                                       .values((Timestamp) null, null, null, null));

        for (TagIndex index : tagIndices) {
            sql.bind(new Timestamp(index.getTimestamp()),
                     index.getName(),
                     index.getValue(),
                     index.getTraceId());
        }
        try {
            sql.execute();
        } catch (DuplicateKeyException ignored) {
            // for database like H2
        }
    }
}
