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

package org.bithon.server.storage.jdbc.tracing.writer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.tracing.SpanKind;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.server.storage.jdbc.common.IOnceTableWriter;
import org.bithon.server.storage.jdbc.common.jooq.Tables;
import org.bithon.server.storage.tracing.ITraceWriter;
import org.bithon.server.storage.tracing.TraceSpan;
import org.bithon.server.storage.tracing.TraceStorageConfig;
import org.bithon.server.storage.tracing.index.TagIndex;
import org.bithon.server.storage.tracing.mapping.TraceIdMapping;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.TransactionalRunnable;
import org.jooq.exception.DataAccessException;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 30/12/20
 */
@Slf4j
public class TraceJdbcWriter implements ITraceWriter {

    protected final DSLContext dslContext;
    private final TraceStorageConfig traceStorageConfig;
    protected final Predicate<Exception> isRetryableException;

    public TraceJdbcWriter(DSLContext dslContext, TraceStorageConfig traceStorageConfig, Predicate<Exception> isRetryableException) {
        this.dslContext = dslContext;
        this.traceStorageConfig = traceStorageConfig;
        this.isRetryableException = isRetryableException;
    }

    protected boolean isTransactionSupported() {
        return true;
    }

    protected boolean isWriteSummaryTable() {
        return true;
    }

    @Override
    public void write(List<TraceSpan> spans,
                      List<TraceIdMapping> mappings,
                      List<TagIndex> tagIndices) {

        TransactionalRunnable runnable = (configuration) -> {
            if (isWriteSummaryTable()) {
                List<TraceSpan> summary = spans.stream()
                                               .filter((span) -> SpanKind.isRootSpan(span.getKind()))
                                               .collect(Collectors.toList());

                this.writeSpans(summary, Tables.BITHON_TRACE_SPAN_SUMMARY);
            }
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

    private void writeSpans(List<TraceSpan> traceSpans, Table<?> table) throws Throwable {
        if (traceSpans.isEmpty()) {
            return;
        }

        String insertStatement = dslContext.render(dslContext.insertInto(table,
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

        doInsert(createInsertSpanRunnable(table.getName(), insertStatement, traceSpans));
    }

    private void writeMappings(List<TraceIdMapping> mappings) throws Throwable {
        if (CollectionUtils.isEmpty(mappings)) {
            return;
        }

        doInsert(createInsertMappingRunnable(dslContext, mappings));
    }

    private void writeTagIndices(Collection<TagIndex> tagIndices) throws Throwable {
        if (CollectionUtils.isEmpty(tagIndices)) {
            return;
        }

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

        doInsert(createInsertIndexRunnable(dslContext, batchValues.values()));
    }


    protected void doInsert(IOnceTableWriter runnable) throws Throwable {
        try {
            dslContext.connection(runnable);
        } catch (DataAccessException e) {
            // Re-throw the caused exception for more clear stack trace
            // In such a case, the caused exception is not NULL.
            throw e.getCause();
        }
    }

    protected IOnceTableWriter createInsertSpanRunnable(String table, String insertStatement, List<TraceSpan> spans) {
        return new SpanTableJdbcWriter(table, insertStatement, spans, isRetryableException) {
            private final ObjectMapper objectMapper = new ObjectMapper();

            @Override
            protected Object toTagStore(Map<String, String> tag) {
                try {
                    return objectMapper.writeValueAsString(tag);
                } catch (JsonProcessingException ignored) {
                    return "{}";
                }
            }
        };
    }

    private IOnceTableWriter createInsertMappingRunnable(DSLContext dslContext, List<TraceIdMapping> mappings) {
        return new MappingTableJdbcWriter(dslContext, mappings, this.isRetryableException);
    }

    private IOnceTableWriter createInsertIndexRunnable(DSLContext dslContext, Collection<Object[]> indice) {
        return new IndexTableJdbcWriter(dslContext, indice, this.isRetryableException);
    }
}
