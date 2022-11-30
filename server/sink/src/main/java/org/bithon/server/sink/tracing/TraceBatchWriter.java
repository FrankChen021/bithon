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

package org.bithon.server.sink.tracing;

import lombok.extern.slf4j.Slf4j;
import org.bithon.server.sink.common.FixedDelayExecutor;
import org.bithon.server.storage.tracing.ITraceWriter;
import org.bithon.server.storage.tracing.TraceSpan;
import org.bithon.server.storage.tracing.index.TagIndex;
import org.bithon.server.storage.tracing.mapping.TraceIdMapping;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 24/12/21
 */
@Slf4j
public class TraceBatchWriter implements ITraceWriter {
    private final List<TraceSpan> traceSpans = new ArrayList<>();
    private final List<TraceIdMapping> traceIdMappings = new ArrayList<>();
    private final List<TagIndex> tagIndexes = new ArrayList<>();

    private final ITraceWriter writer;
    private final FixedDelayExecutor executor;
    private final int batchSize;

    public TraceBatchWriter(ITraceWriter writer, TraceSinkConfig config) {
        this.writer = writer;
        this.batchSize = config.getBatch() == null ? 2000 : config.getBatch().getSize();
        this.executor = new FixedDelayExecutor("trace-batch-writer",
                                               this::flush,
                                               5,
                                               () -> Duration.ofSeconds(config.getBatch() == null ? 1 : config.getBatch().getInterval()));
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
        if (traceSpans.size() > this.batchSize) {
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
        try {
            this.executor.shutdown(Duration.ofSeconds(20));
        } catch (InterruptedException ignored) {
        }

        // flush all data to see if there's any more data
        flush();

        // close underlying writer at last
        this.writer.close();
    }
}
