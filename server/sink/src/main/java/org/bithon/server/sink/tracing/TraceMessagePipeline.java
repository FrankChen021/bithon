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
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.server.sink.common.service.UriNormalizer;
import org.bithon.server.sink.tracing.sink.ITraceMessageSink2;
import org.bithon.server.sink.tracing.source.ITraceMessageSource;
import org.bithon.server.sink.tracing.transform.TraceSpanTransformer;
import org.bithon.server.storage.datasource.input.transformer.ExceptionSafeTransformer;
import org.bithon.server.storage.datasource.input.transformer.ITransformer;
import org.bithon.server.storage.tracing.TraceSpan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * <pre>
 *     bithon:
 *       processor:
 *         tracing:
 *           source:
 *             type: brpc|kafka
 *             props:
 *           transforms:
 *             - type: filter
 *             - type: sanitize
 *           sinks:
 *             - type: store
 *             - type: metric-over-span
 * </pre>
 *
 * @author frank.chen021@outlook.com
 * @date 12/4/22 11:38 AM
 */
@Slf4j
public class TraceMessagePipeline implements SmartLifecycle, ITraceMessageSink {

    @Getter
    private final boolean isEnabled;

    private final ITraceMessageSource source;
    private final List<ITransformer> transformers;
    private final List<ITraceMessageSink2> sinks;
    private boolean isRunning = false;

    public TraceMessagePipeline(@JacksonInject(useInput = OptBoolean.FALSE) TraceSinkConfig traceSinkConfig,
                                @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper objectMapper,
                                @JacksonInject(useInput = OptBoolean.FALSE) ApplicationContext applicationContext) throws IOException {
        this.isEnabled = traceSinkConfig.isEnabled();

        this.source = this.isEnabled ? createObject(ITraceMessageSource.class, objectMapper, traceSinkConfig.getSource()) : null;
        this.transformers = isEnabled ? createTransformers(traceSinkConfig.getTransforms(), applicationContext, objectMapper) : null;
        this.sinks = this.isEnabled ? createSinks(traceSinkConfig.getSinks(), objectMapper) : null;

        if (this.source != null) {
            this.source.registerProcessor(this);
        }
    }

    private <T> T createObject(Class<T> clazz, ObjectMapper objectMapper, Object configuration) throws IOException {
        return objectMapper.readValue(objectMapper.writeValueAsBytes(configuration), clazz);
    }

    private List<ITransformer> createTransformers(List<Map<String, String>> transforms,
                                                  ApplicationContext applicationContext,
                                                  ObjectMapper objectMapper) {
        List<ITransformer> transformers = new ArrayList<>();
        transformers.add(new TraceSpanTransformer(applicationContext.getBean(UriNormalizer.class)));

        if (CollectionUtils.isEmpty(transforms)) {
            return transformers;
        }

        for (Map<String, String> transform : transforms) {
            try {
                transformers.add(new ExceptionSafeTransformer(createObject(ITransformer.class, objectMapper, transform)));
            } catch (IOException ignored) {
            }
        }
        return transformers;
    }

    private List<ITraceMessageSink2> createSinks(List<Map<String, String>> sinks,
                                                 ObjectMapper objectMapper) {
        List<ITraceMessageSink2> sinkObjects = new ArrayList<>();
        for (Map<String, String> sink : sinks) {
            try {
                sinkObjects.add(createObject(ITraceMessageSink2.class, objectMapper, sink));
            } catch (IOException e) {
                log.error("Failed to create sink from configuration", e);
            }
        }
        return sinkObjects;
    }

    public <T extends ITraceMessageSink2> T link(T sink) {
        synchronized (sinks) {
            sinks.add(sink);
        }
        return sink;
    }

    public <T extends ITraceMessageSink2> T unlink(T sink) {
        synchronized (sinks) {
            sinks.remove(sink);
        }
        return sink;
    }

    public void process(String messageType, List<TraceSpan> spans) {
        if (CollectionUtils.isEmpty(spans)) {
            return;
        }

        Iterator<TraceSpan> iterator = spans.iterator();
        while (iterator.hasNext()) {
            TraceSpan span = iterator.next();

            for (ITransformer transformer : transformers) {
                if (!transformer.transform(span)) {
                    iterator.remove();
                }
            }
        }

        if (spans.isEmpty()) {
            return;
        }

        ITraceMessageSink2[] sinks = this.sinks.toArray(new ITraceMessageSink2[0]);
        for (ITraceMessageSink2 sink : sinks) {
            try {
                sink.process(messageType, spans);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    public void close() throws Exception {
        if (!this.isEnabled) {
            return;
        }

        // Stop the source first
        this.source.stop();

        for (ITraceMessageSink2 sink : sinks) {
            sink.close();
        }
    }

    @Override
    public void start() {
        log.info("Starting the source of trace process pipeline...");
        this.source.start();
        this.isRunning = true;
    }

    @Override
    public void stop() {
        try {
            this.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }
}
