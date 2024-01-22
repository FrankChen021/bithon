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
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.sink.common.service.UriNormalizer;
import org.bithon.server.sink.tracing.transform.TraceSpanTransformer;
import org.bithon.server.storage.datasource.input.IInputRow;
import org.bithon.server.storage.datasource.input.filter.IInputRowFilter;
import org.bithon.server.storage.datasource.input.transformer.ITransformer;
import org.bithon.server.storage.tracing.TraceSpan;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
        private String lastException;

        ExceptionSafeTransformer(ITransformer delegate) {
            this.delegate = delegate;
        }

        @Override
        public void transform(IInputRow inputRow) throws TransformException {
            try {
                delegate.transform(inputRow);
            } catch (Exception e) {
                long now = System.currentTimeMillis();
                if (now - lastLogTimestamp < 5_000) {
                    return;
                }

                log.error(StringUtils.format("Fail to transform, message [%s], Span [%s]", e.getMessage(), inputRow),
                          e.getClass().getName().equals(this.lastException) ? null : e);

                this.lastLogTimestamp = now;
                this.lastException = e.getClass().getName();
            }
        }
    }

    private final List<ITraceMessageSink> sinks = Collections.synchronizedList(new ArrayList<>());
    private final ITraceMessageSink defaultSink;

    private final ExecutorService executorService;

    private final List<ITransformer> transformers;
    private final IInputRowFilter filter;

    @JsonCreator
    public LocalTraceSink(@JacksonInject(useInput = OptBoolean.FALSE) TraceSinkConfig traceSinkConfig,
                          @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper objectMapper,
                          @JacksonInject(useInput = OptBoolean.FALSE) ApplicationContext applicationContext) {
        this.filter = traceSinkConfig.createFilter(objectMapper);
        this.transformers = createTransformers(traceSinkConfig.getTransform(), applicationContext, objectMapper);

        this.executorService = Executors.newCachedThreadPool(NamedThreadFactory.of("trace-processor"));
        this.defaultSink = new SinkImpl(applicationContext);
    }

    public List<ITransformer> createTransformers(List<Map<String, String>> transform,
                                                 ApplicationContext applicationContext,
                                                 ObjectMapper objectMapper) {
        List<ITransformer> transformers = new ArrayList<>();
        transformers.add(new TraceSpanTransformer(applicationContext.getBean(UriNormalizer.class)));

        if (CollectionUtils.isEmpty(transform)) {
            return transformers;
        }

        for (Map<String, String> oneTransform : transform) {
            try {
                transformers.add(new ExceptionSafeTransformer(objectMapper.readValue(objectMapper.writeValueAsBytes(oneTransform),
                                                                                     ITransformer.class)));
            } catch (IOException ignored) {
            }
        }
        return transformers;
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
        if (CollectionUtils.isEmpty(spans)) {
            return;
        }

        Iterator<TraceSpan> iterator = spans.iterator();
        while (iterator.hasNext()) {
            TraceSpan span = iterator.next();

            // Transform first
            for (ITransformer transformer : transformers) {
                transformer.transform(span);
            }

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

        // Run the default sink first so that backpressure can be leveraged
        this.defaultSink.process(messageType, spans);

        // Run other sinks in parallel
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
        this.defaultSink.close();
        for (ITraceMessageSink sink : sinks) {
            sink.close();
        }
        this.executorService.shutdownNow();
        //noinspection ResultOfMethodCallIgnored
        this.executorService.awaitTermination(10, TimeUnit.SECONDS);
    }
}
