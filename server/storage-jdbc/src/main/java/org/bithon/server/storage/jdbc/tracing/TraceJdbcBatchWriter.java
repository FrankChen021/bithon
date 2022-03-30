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

import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.concurrency.NamedThreadFactory;
import org.bithon.server.tracing.TraceSpan;
import org.bithon.server.tracing.index.TagIndex;
import org.bithon.server.tracing.mapping.TraceIdMapping;
import org.bithon.server.tracing.storage.ITraceWriter;
import org.bithon.server.tracing.storage.TraceStorageConfig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The batch writer here may not be a perfect design.
 * It can be put at the message handler layer so that all writers can gain batch capability.
 * For metrics have already been aggregated at agent side it's TPS is not very high, So it's not a pain point.
 * <p>
 * But for trace, there's no such aggregation layer which may result in high QPS of insert.
 * Since I'm not focusing on the implementation detail now, perfect solution is left in the future.
 *
 * @author Frank Chen
 * @date 24/12/21
 */
@Slf4j
public class TraceJdbcBatchWriter implements ITraceWriter {
    private final List<TraceSpan> traceSpans = new ArrayList<>();
    private final List<TraceIdMapping> traceIdMappings = new ArrayList<>();
    private final List<TagIndex> tagIndexes = new ArrayList<>();

    private final ITraceWriter writer;
    private final TraceStorageConfig config;
    private final ScheduledExecutorService executor;

    public TraceJdbcBatchWriter(ITraceWriter writer, TraceStorageConfig config) {
        this.writer = writer;
        this.config = config;
        this.executor = Executors.newSingleThreadScheduledExecutor(NamedThreadFactory.of("trace-batch-writer"));
        this.executor.scheduleWithFixedDelay(this::flush, 5, 1, TimeUnit.SECONDS);
    }

    @Override
    public void write(Collection<TraceSpan> spans,
                      Collection<TraceIdMapping> mappings,
                      Collection<TagIndex> tagIndices) {
        synchronized (this) {
            this.traceSpans.addAll(spans);
            this.traceIdMappings.addAll(mappings);
            this.tagIndexes.addAll(tagIndices);
        }
        if (traceSpans.size() > config.getBatchSize()) {
            flush();
        }
    }

    private void flush() {
        List<TraceSpan> spans;
        List<TraceIdMapping> idMappings;
        List<TagIndex> tagIndexes;
        synchronized (this) {
            spans = new ArrayList<>(this.traceSpans);
            idMappings = new ArrayList<>(this.traceIdMappings);
            tagIndexes = new ArrayList<>(this.tagIndexes);

            this.traceSpans.clear();
            this.traceIdMappings.clear();
            this.tagIndexes.clear();
        }

        if (spans.isEmpty() && idMappings.isEmpty() && tagIndexes.isEmpty()) {
            return;
        }
        try {
            log.debug("Flushing [{}] spans into storage...", spans.size());
            this.writer.write(spans, idMappings, tagIndexes);
        } catch (Exception e) {
            log.error("Exception when flushing spans into storage", e);
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
