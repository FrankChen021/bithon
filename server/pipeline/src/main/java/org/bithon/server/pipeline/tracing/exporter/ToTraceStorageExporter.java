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

package org.bithon.server.pipeline.tracing.exporter;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.OptBoolean;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bithon.server.pipeline.common.handler.AbstractThreadPoolMessageHandler;
import org.bithon.server.pipeline.tracing.TracePipelineConfig;
import org.bithon.server.pipeline.tracing.index.TagIndexGenerator;
import org.bithon.server.pipeline.tracing.mapping.TraceMappingFactory;
import org.bithon.server.storage.tracing.ITraceStorage;
import org.bithon.server.storage.tracing.ITraceWriter;
import org.bithon.server.storage.tracing.TraceSpan;
import org.bithon.server.storage.tracing.TraceStorageConfig;
import org.bithon.server.storage.tracing.mapping.TraceIdMapping;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Function;

/**
 * Backpressure sink handler
 *
 * @author frank.chen021@outlook.com
 * @date 2021/2/4 8:21 下午
 */
@Slf4j
public class ToTraceStorageExporter implements ITraceExporter {

    @Getter
    private final Handler handler;

    @JsonCreator
    public ToTraceStorageExporter(@JacksonInject(useInput = OptBoolean.FALSE) ApplicationContext applicationContext) {
        this.handler = new Handler(applicationContext);
    }

    @Override
    public void process(String messageType, List<TraceSpan> spans) {
        handler.submit(spans);
    }

    @Override
    public void close() throws Exception {
        handler.close();
    }

    static class Handler extends AbstractThreadPoolMessageHandler<List<TraceSpan>> {

        private final ITraceWriter traceWriter;
        private final Function<Collection<TraceSpan>, List<TraceIdMapping>> mappingExtractor;
        private final TagIndexGenerator tagIndexBuilder;

        public Handler(ApplicationContext applicationContext) {
            super("trace-sink",
                  1,
                  10,
                  Duration.ofMinutes(1),
                  10,
                  // Use CallRunsPolicy to implement a backpressure policy
                  new ThreadPoolExecutor.CallerRunsPolicy());

            TracePipelineConfig sinkConfig = applicationContext.getBean(TracePipelineConfig.class);
            this.traceWriter = new TraceBatchWriter(applicationContext.getBean(ITraceStorage.class).createWriter(), sinkConfig);
            this.mappingExtractor = TraceMappingFactory.create(applicationContext);
            this.tagIndexBuilder = new TagIndexGenerator(applicationContext.getBean(TraceStorageConfig.class));
        }

        @Override
        protected void onMessage(List<TraceSpan> spans) throws IOException {
            if (spans.isEmpty()) {
                return;
            }

            traceWriter.write(spans,
                              mappingExtractor.apply(spans),
                              tagIndexBuilder.generate(spans));
        }

        @Override
        public String getType() {
            return "trace";
        }

        @Override
        public void close() throws Exception {
            super.close();
            this.traceWriter.close();
        }
    }

    @Override
    public String toString() {
        return "export-trace-to-storage";
    }
}
