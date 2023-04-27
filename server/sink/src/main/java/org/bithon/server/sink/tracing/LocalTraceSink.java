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

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.concurrency.NamedThreadFactory;
import org.bithon.server.sink.common.service.UriNormalizer;
import org.bithon.server.sink.tracing.transform.TraceSpanTransformer;
import org.bithon.server.storage.datasource.input.IInputRow;
import org.bithon.server.storage.datasource.input.filter.IInputRowFilter;
import org.bithon.server.storage.datasource.input.transformer.ITransformer;
import org.bithon.server.storage.tracing.TraceSpan;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author frank.chen021@outlook.com
 * @date 12/4/22 11:38 AM
 */
@Slf4j
@JsonTypeName("local")
public class LocalTraceSink implements ITraceMessageSink {

    static class SinkImpl implements ITraceMessageSink {

        @Getter
        private final TraceSinkHandler handler;

        public SinkImpl(ApplicationContext applicationContext) {
            this.handler = new TraceSinkHandler(applicationContext);
        }

        @Override
        public void process(String messageType, List<TraceSpan> messages) {
            handler.submit(messages);
        }

        @Override
        public void close() throws Exception {
            handler.close();
        }
    }

    static class ExceptionSafeTransformer implements ITransformer {
        private final ITransformer delegate;

        /**
         * There are too many spans, suppress exception logs to avoid too many logs
         */
        private long lastLogTimestamp = System.currentTimeMillis();

        ExceptionSafeTransformer(ITransformer delegate) {
            this.delegate = delegate;
        }

        @Override
        public void transform(IInputRow inputRow) throws TransformException {
            try {
                delegate.transform(inputRow);
            } catch (Exception e) {
                long now = System.currentTimeMillis();
                if (now - lastLogTimestamp >= 5_000) {
                    lastLogTimestamp = now;
                    log.error("Fail to transform span [{}], message: {}", inputRow, e.getMessage());
                }
            }
        }
    }

    private final List<ITraceMessageSink> sinks = Collections.synchronizedList(new ArrayList<>());

    private final ExecutorService executorService;

    private final ITransformer transformer;
    private final IInputRowFilter filter;

    @JsonCreator
    public LocalTraceSink(@JacksonInject(useInput = OptBoolean.FALSE) TraceSinkConfig traceSinkConfig,
                          @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper objectMapper,
                          @JacksonInject(useInput = OptBoolean.FALSE) ApplicationContext applicationContext) {
        this.filter = traceSinkConfig.createFilter(objectMapper);

        ITransformer transformer = traceSinkConfig.createTransformers(objectMapper);
        if (transformer == null) {
            transformer = new TraceSpanTransformer(applicationContext.getBean(UriNormalizer.class));
        }
        this.transformer = new ExceptionSafeTransformer(transformer);

        this.executorService = Executors.newCachedThreadPool(NamedThreadFactory.of("trace-processor"));
        link(new SinkImpl(applicationContext));
    }

    public <T extends ITraceMessageSink> T link(T sink) {
        sinks.add(sink);
        return sink;
    }

    public <T extends ITraceMessageSink> T unlink(T sink) {
        sinks.remove(sink);
        return sink;
    }

    @Override
    public void process(String messageType, List<TraceSpan> spans) {
        if (spans.isEmpty()) {
            return;
        }

        Iterator<TraceSpan> iterator = spans.iterator();
        while (iterator.hasNext()) {
            TraceSpan span = iterator.next();

            // Transform first
            this.transformer.transform(span);

            // Post filter
            if (this.filter != null) {
                if (!this.filter.shouldInclude(span)) {
                    iterator.remove();
                }
            }
        }

        if (spans.isEmpty()) {
            return;
        }
        for (ITraceMessageSink sink : sinks) {
            executorService.execute(new TraceMessageSinkRunnable(sink, messageType, spans));
        }
    }

    /**
     * Use an explicitly defined class instead of lambda
     * because it helps improve the observability of tracing logs on {@link ExecutorService#execute}
     */
    static class TraceMessageSinkRunnable implements Runnable {
        private final ITraceMessageSink sink;
        private final String messageType;
        private final List<TraceSpan> spans;

        TraceMessageSinkRunnable(ITraceMessageSink sink, String messageType, List<TraceSpan> spans) {
            this.sink = sink;
            this.messageType = messageType;
            this.spans = spans;
        }

        @Override
        public void run() {
            try {
                sink.process(messageType, spans);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public void close() throws Exception {
        for (ITraceMessageSink sink : sinks) {
            sink.close();
        }
        this.executorService.shutdownNow();
        //noinspection ResultOfMethodCallIgnored
        this.executorService.awaitTermination(10, TimeUnit.SECONDS);
    }
}
