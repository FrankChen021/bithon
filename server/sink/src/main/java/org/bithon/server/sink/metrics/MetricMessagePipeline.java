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
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 3/10/21 14:11
 */
@Slf4j
public class MetricMessagePipeline implements SmartLifecycle {

    final ApplicationContext applicationContext;

    private final List<IMetricReceiver> receivers;
    private final List<IMetricExporter> exporters;
    private boolean isRunning = false;

    @JsonCreator
    public MetricMessagePipeline(@JacksonInject(useInput = OptBoolean.FALSE) MetricPipelineConfig pipelineConfig,
                                 @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper objectMapper,
                                 @JacksonInject(useInput = OptBoolean.FALSE) ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;

        this.receivers = createReceivers(pipelineConfig, objectMapper);
        this.exporters = createExporters(pipelineConfig, objectMapper);
    }

    private <T> T createObject(Class<T> clazz, ObjectMapper objectMapper, Object configuration) throws IOException {
        return objectMapper.readValue(objectMapper.writeValueAsBytes(configuration), clazz);
    }

    private List<IMetricReceiver> createReceivers(MetricPipelineConfig pipelineConfig,
                                                  ObjectMapper objectMapper) {
        if (!pipelineConfig.isEnabled()) {
            return Collections.emptyList();
        }

        return pipelineConfig.getReceivers()
                             .stream()
                             .map((receiverConfig) -> {
                                 try {
                                     return createObject(IMetricReceiver.class, objectMapper, receiverConfig);
                                 } catch (IOException e) {
                                     throw new RuntimeException(e);
                                 }
                             }).collect(Collectors.toList());

    }

    private List<IMetricExporter> createExporters(MetricPipelineConfig pipelineConfig,
                                                  ObjectMapper objectMapper) {
        if (!pipelineConfig.isEnabled()) {
            return Collections.emptyList();
        }

        return exporters.stream()
                        .map((exporterConfig) -> {
                            try {
                                return createObject(IMetricExporter.class, objectMapper, exporterConfig);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }).collect(Collectors.toList());
    }

    @Override
    public void start() {
        log.info("Starting the source of metrics process pipeline...");

        IMetricProcessor processor = new IMetricProcessor() {
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
        };

        for (IMetricReceiver receiver : this.receivers) {
            receiver.registerProcessor(processor);
            receiver.start();
        }

        this.isRunning = true;
    }

    @Override
    public void stop() {
        // Stop the source first
        for (IMetricReceiver receiver : this.receivers) {
            receiver.stop();
        }

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
