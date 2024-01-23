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

package org.bithon.server.sink.metrics;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bithon.server.sink.metrics.exporter.IMetricExporter;
import org.bithon.server.sink.metrics.receiver.IMetricReceiver;
import org.bithon.server.storage.datasource.input.transformer.ITransformer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 3/10/21 14:11
 */
@Slf4j
public class MetricMessagePipeline implements SmartLifecycle {

    final ApplicationContext applicationContext;
    private final boolean isEnabled;

    private final IMetricReceiver source;
    private final List<ITransformer> transformers;
    private final List<IMetricExporter> exporters;
    private boolean isRunning = false;

    @JsonCreator
    public MetricMessagePipeline(@JacksonInject(useInput = OptBoolean.FALSE) MetricSinkConfig sinkConfig,
                                 @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper objectMapper,
                                 @JacksonInject(useInput = OptBoolean.FALSE) ApplicationContext applicationContext) throws IOException {
        this.applicationContext = applicationContext;

        this.isEnabled = sinkConfig.isEnabled();
        this.transformers = null;
        this.source = this.isEnabled ? createObject(IMetricReceiver.class, objectMapper, sinkConfig.getSource()) : null;
        this.exporters = this.isEnabled ? createExporters(sinkConfig.getExporters(), objectMapper) : null;
    }

    private <T> T createObject(Class<T> clazz, ObjectMapper objectMapper, Object configuration) throws IOException {
        return objectMapper.readValue(objectMapper.writeValueAsBytes(configuration), clazz);
    }

    private List<IMetricExporter> createExporters(List<Map<String, String>> sinks,
                                                  ObjectMapper objectMapper) {
        List<IMetricExporter> sinkObjects = new ArrayList<>();
        for (Map<String, String> sink : sinks) {
            try {
                sinkObjects.add(createObject(IMetricExporter.class, objectMapper, sink));
            } catch (IOException e) {
                log.error("Failed to create sink from configuration", e);
            }
        }
        return sinkObjects;
    }

    @Override
    public void start() {
        log.info("Starting the source of metrics process pipeline...");

        this.source.registerProcessor(new IMetricProcessor() {
            @Override
            public void close() {
            }

            @Override
            public void process(String messageType, SchemaMetricMessage message) {
                IMetricExporter[] exporter = exporters.toArray(new IMetricExporter[0]);
                for (IMetricExporter sink : exporter) {
                    try {
                        sink.process(messageType, message);
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }
            }
        });
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

        for (IMetricExporter exporter : exporters) {
            try {
                exporter.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }
}
