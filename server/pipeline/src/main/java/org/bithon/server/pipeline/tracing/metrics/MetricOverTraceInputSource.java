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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.pipeline.common.transformer.TransformSpec;
import org.bithon.server.pipeline.metrics.exporter.IMetricMessageHandler;
import org.bithon.server.pipeline.metrics.input.IMetricInputSource;
import org.bithon.server.pipeline.tracing.TracePipeline;
import org.bithon.server.pipeline.tracing.exporter.MetricOverTraceExporter;
import org.bithon.server.storage.datasource.DefaultSchema;
import org.bithon.server.storage.datasource.ISchema;
import org.bithon.server.storage.datasource.input.IInputRow;
import org.bithon.server.storage.metrics.IMetricStorage;
import org.springframework.context.ApplicationContext;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    public SamplingResult sample(ISchema schema, Duration timeout) {
        if (!this.pipeline.getPipelineConfig().isEnabled()) {
            throw new RuntimeException("The trace processing pipeline is not enabled in this module.");
        }

        if (!this.pipeline.getPipelineConfig().isMetricOverSpanEnabled()) {
            throw new RuntimeException("The metric over span is not enabled for this pipeline");
        }

        try (MetricSampler sampler = new MetricSampler()) {
            try (MetricOverTraceExporter exporter = new MetricOverTraceExporter(transformSpec,
                                                                                (DefaultSchema) schema,
                                                                                sampler)) {
                try {
                    try {
                        this.pipeline.link(exporter);
                    } catch (Exception e) {

                        throw new RuntimeException(e);
                    }

                    long elapsed = 0;
                    while (sampler.sampled.isEmpty() && elapsed < timeout.toMillis()) {
                        try {
                            //noinspection BusyWait
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            break;
                        }
                        elapsed += 100;
                    }

                    return new SamplingResult(sampler.sampled
                                                  .stream()
                                                  .map(IInputRow::toMap)
                                                  .toList());
                } finally {
                    exporter.close();
                    this.pipeline.unlink(exporter);
                }
            }
        }
    }

    static class MetricSampler implements IMetricMessageHandler {
        private final List<IInputRow> sampled = Collections.synchronizedList(new ArrayList<>());

        @Override
        public void process(List<IInputRow> metricMessages) {
            sampled.addAll(metricMessages);
        }

        @Override
        public void close() {
        }
    }
}
