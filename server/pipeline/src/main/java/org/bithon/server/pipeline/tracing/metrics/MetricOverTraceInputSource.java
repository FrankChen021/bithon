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

package org.bithon.server.pipeline.tracing.metrics;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.datasource.DefaultSchema;
import org.bithon.server.datasource.ISchema;
import org.bithon.server.datasource.input.IInputRow;
import org.bithon.server.pipeline.common.transformer.TransformSpec;
import org.bithon.server.pipeline.metrics.exporter.IMetricMessageHandler;
import org.bithon.server.pipeline.metrics.input.IMetricInputSource;
import org.bithon.server.pipeline.tracing.TracePipeline;
import org.bithon.server.pipeline.tracing.exporter.MetricOverTraceExporter;
import org.bithon.server.storage.metrics.IMetricStorage;
import org.springframework.context.ApplicationContext;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author frank.chen021@outlook.com
 * @date 12/4/22 11:27 AM
 */
@Slf4j
@JsonTypeName("span")
public class MetricOverTraceInputSource implements IMetricInputSource {

    @JsonIgnore
    private final TracePipeline pipeline;

    @Getter
    private final TransformSpec transformSpec;

    @JsonIgnore
    private final IMetricStorage metricStorage;

    @JsonIgnore
    private final ApplicationContext applicationContext;

    @JsonIgnore
    private MetricOverTraceExporter metricExporter;

    @JsonCreator
    public MetricOverTraceInputSource(@JsonProperty("transformSpec") @NotNull TransformSpec transformSpec,
                                      @JacksonInject(useInput = OptBoolean.FALSE) TracePipeline pipeline,
                                      @JacksonInject(useInput = OptBoolean.FALSE) IMetricStorage metricStorage,
                                      @JacksonInject(useInput = OptBoolean.FALSE) ApplicationContext applicationContext) {
        Preconditions.checkArgumentNotNull("transformSpec", transformSpec);

        this.transformSpec = transformSpec;
        this.pipeline = pipeline;
        this.metricStorage = metricStorage;
        this.applicationContext = applicationContext;
    }

    @Override
    public void start(ISchema schema) {
        if (!this.pipeline.getPipelineConfig().isEnabled()) {
            log.warn("The trace processing pipeline is not enabled in this module. The input source of [{}] has no effect.", schema.getName());
            return;
        }

        try {
            this.metricStorage.createMetricWriter(schema).close();
            log.info("Success to initialize metric storage for [{}].", schema.getName());
        } catch (Exception e) {
            log.info("Failed to initialize metric storage for [{}]: {}", schema.getName(), e.getMessage());
        }

        if (!this.pipeline.getPipelineConfig().isMetricOverSpanEnabled()) {
            log.info("The metric over span is not enabled for this pipeline");
            return;
        }

        log.info("Adding metric-exporter for [{}({})] to tracing logs processors...", schema.getName(), schema.getSignature());
        MetricOverTraceExporter exporter = null;
        try {
            exporter = new MetricOverTraceExporter(transformSpec, (DefaultSchema) schema, metricStorage, applicationContext);
            metricExporter = (MetricOverTraceExporter) this.pipeline.link(exporter);
        } catch (Exception e) {
            if (exporter != null) {
                this.pipeline.unlink(exporter);
                exporter.close();
            }
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        if (metricExporter == null) {
            return;
        }

        log.info("Removing metric-exporter for [{}({})] from tracing logs processors...",
                 metricExporter.getSchema().getName(),
                 metricExporter.getSchema().getSignature());
        try {
            this.pipeline.unlink(metricExporter).close();
        } catch (Exception ignored) {
        }
    }

    @Override
    public StreamingResponseBody sample(ISchema schema, Duration timeout) {
        if (!this.pipeline.getPipelineConfig().isEnabled()) {
            throw new RuntimeException("The trace processing pipeline is not enabled in this module.");
        }

        if (!this.pipeline.getPipelineConfig().isMetricOverSpanEnabled()) {
            throw new RuntimeException("The metric over span is not enabled for this pipeline");
        }

        final ObjectMapper objectMapper = applicationContext.getBean(ObjectMapper.class)
                                                            .copy()
                                                            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

        return output -> {
            try (MetricSampler sampler = new MetricSampler()) {
                try (MetricOverTraceExporter exporter = new MetricOverTraceExporter(transformSpec, (DefaultSchema) schema, sampler)) {
                    this.pipeline.link(exporter);

                    long startTime = System.currentTimeMillis();
                    long endTimestamp = startTime + timeout.toMillis();
                    int sampledEventCount = 0;
                    Duration pollTimeout = Duration.ofMillis(200);
                    try {
                        if (!sendStreamingEvent(objectMapper, output,
                                                new StreamingEvent("progress",
                                                                   TimeSpan.now().toISO8601()))) {
                            return;
                        }

                        while (System.currentTimeMillis() < endTimestamp) {
                            List<IInputRow> sampledEvents;
                            try {
                                sampledEvents = sampler.poll(10, pollTimeout);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                break;
                            }

                            if (!sampledEvents.isEmpty()) {
                                // NOTE: don't close the generator here, otherwise it will close the output stream
                                JsonGenerator generator = objectMapper.createGenerator(output);
                                for (IInputRow sampledEvent : sampledEvents) {
                                    sampledEventCount++;
                                    try {
                                        objectMapper.writeValue(generator, new StreamingEvent("data",
                                                                                              TimeSpan.now().toISO8601(),
                                                                                              sampledEvent.toMap()));
                                        generator.writeRaw('\n');
                                    } catch (IOException e) {
                                        return;
                                    } catch (Exception e) {
                                        log.error("[TraceSampler] Error serializing initial marker: {}. Aborting.", e.getMessage(), e);
                                        return;
                                    }
                                }
                                generator.flush();
                            } else {
                                // No data row from poll, sending progress event (nearly every second)
                                long elapsedTime = System.currentTimeMillis() - startTime;
                                if (elapsedTime % 1000 < pollTimeout.toMillis()) {
                                    if (!sendStreamingEvent(objectMapper,
                                                            output,
                                                            new StreamingEvent("progress",
                                                                               TimeSpan.now().toISO8601(),
                                                                               Map.of("duration", System.currentTimeMillis() - startTime,
                                                                                      "count", sampledEventCount)))) {
                                        return;
                                    }
                                }
                            }
                        }

                    } finally {
                        try {
                            this.pipeline.unlink(exporter);
                        } catch (Exception ignored) {
                        }
                        try {
                            exporter.close();
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        };
    }

    @Data
    static class StreamingEvent {
        private String type;
        private String eventTime;
        private Object event;

        public StreamingEvent(String type, String eventTime) {
            this.type = type;
            this.eventTime = eventTime;
            this.event = null;
        }

        public StreamingEvent(String type, String eventTime, Object event) {
            this.type = type;
            this.eventTime = eventTime;
            this.event = event;
        }
    }

    private boolean sendStreamingEvent(ObjectMapper objectMapper, OutputStream output, StreamingEvent event) {
        try {
            objectMapper.writeValue(objectMapper.createGenerator(output), event);
            output.write('\n');
            output.flush();
            return true;
        } catch (IOException e) {
            log.warn("[TraceSampler] IOException writing initial marker (client likely disconnected): {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("[TraceSampler] Error serializing initial marker: {}. Aborting.", e.getMessage(), e);
            return false;
        }
    }

    static class MetricSampler implements IMetricMessageHandler {
        private final LinkedBlockingQueue<IInputRow> sampled = new LinkedBlockingQueue<>();

        @Override
        public void process(List<IInputRow> metricMessages) {
            sampled.addAll(metricMessages);
        }

        @Override
        public void close() {
        }

        /**
         * poll up to n items from the queue for up to the specified timeout.
         */
        public List<IInputRow> poll(int n, Duration timeout) throws InterruptedException {

            // Wait for the first item with the specified timeout
            IInputRow firstRow = sampled.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (firstRow != null) {
                List<IInputRow> rows = new ArrayList<>();

                rows.add(firstRow);
                // Try to get up to n-1 more items without further blocking for each item
                if (n > 1) {
                    sampled.drainTo(rows, n - 1);
                }
                return rows;
            } else {
                return Collections.emptyList();
            }
        }
    }
}
