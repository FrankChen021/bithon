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
import org.bithon.server.sink.tracing.exporter.ITraceExporter;
import org.bithon.server.sink.tracing.receiver.ITraceReceiver;
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
 * @author frank.chen021@outlook.com
 * @date 12/4/22 11:38 AM
 */
@Slf4j
public class TracePipeline implements SmartLifecycle {

    @Getter
    private final boolean isEnabled;

    private final ITraceReceiver source;
    private final List<ITransformer> transformers;
    private final List<ITraceExporter> exporters;
    private boolean isRunning = false;

    public TracePipeline(@JacksonInject(useInput = OptBoolean.FALSE) TracePipelineConfig tracePipelineConfig,
                         @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper objectMapper,
                         @JacksonInject(useInput = OptBoolean.FALSE) ApplicationContext applicationContext) throws IOException {
        this.isEnabled = tracePipelineConfig.isEnabled();

        this.source = this.isEnabled ? createObject(ITraceReceiver.class, objectMapper, tracePipelineConfig.getSource()) : null;
        this.transformers = isEnabled ? createTransformers(tracePipelineConfig.getTransforms(), applicationContext, objectMapper) : null;
        this.exporters = this.isEnabled ? createExporters(tracePipelineConfig.getExporters(), objectMapper) : null;
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

    private List<ITraceExporter> createExporters(List<Map<String, String>> sinks,
                                                 ObjectMapper objectMapper) {
        List<ITraceExporter> sinkObjects = new ArrayList<>();
        for (Map<String, String> sink : sinks) {
            try {
                sinkObjects.add(createObject(ITraceExporter.class, objectMapper, sink));
            } catch (IOException e) {
                log.error("Failed to create sink from configuration", e);
            }
        }
        return sinkObjects;
    }

    public <T extends ITraceExporter> T link(T sink) {
        synchronized (exporters) {
            exporters.add(sink);
        }
        return sink;
    }

    public <T extends ITraceExporter> T unlink(T sink) {
        synchronized (exporters) {
            exporters.remove(sink);
        }
        return sink;
    }

    @Override
    public void start() {
        log.info("Starting the source of trace process pipeline...");

        this.source.registerProcessor(new PipelineProcessor());
        this.source.start();
        this.isRunning = true;
    }

    @Override
    public void stop() {
        if (!this.isEnabled) {
            return;
        }

        // Stop the source first
        this.source.stop();

        for (ITraceExporter export : exporters) {
            try {
                export.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    class PipelineProcessor implements ITraceProcessor {
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

            ITraceExporter[] sinks = exporters.toArray(new ITraceExporter[0]);
            for (ITraceExporter sink : sinks) {
                try {
                    sink.process(messageType, spans);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }

        @Override
        public void close() throws Exception {
        }
    }
}
