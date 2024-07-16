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

package org.bithon.server.pipeline.event.metrics;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.bithon.server.pipeline.common.transform.TransformSpec;
import org.bithon.server.pipeline.event.EventPipeline;
import org.bithon.server.pipeline.event.exporter.IEventExporter;
import org.bithon.server.pipeline.event.exporter.MetricOverEventExporter;
import org.bithon.server.pipeline.metrics.MetricPipelineConfig;
import org.bithon.server.pipeline.metrics.input.IMetricInputSource;
import org.bithon.server.storage.datasource.ISchema;
import org.bithon.server.storage.datasource.SchemaManager;
import org.bithon.server.storage.meta.IMetaStorage;
import org.bithon.server.storage.metrics.IMetricStorage;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.time.Duration;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/7/31 11:28
 */
@Slf4j
@JsonTypeName("event")
public class MetricOverEventInputSource implements IMetricInputSource {

    private final TransformSpec transformSpec;
    private final ApplicationContext applicationContext;
    private final EventPipeline pipeline;
    private final String eventType;

    private IEventExporter exporter;

    @JsonCreator
    public MetricOverEventInputSource(@JsonProperty("eventType") String eventType,
                                      @JsonProperty("transformSpec") @NotNull TransformSpec transformSpec,
                                      @JacksonInject(useInput = OptBoolean.FALSE) EventPipeline pipeline,
                                      @JacksonInject(useInput = OptBoolean.FALSE) ApplicationContext applicationContext) {
        this.transformSpec = transformSpec;
        this.pipeline = pipeline;
        this.applicationContext = applicationContext;
        this.eventType = eventType;
    }

    @Override
    public TransformSpec getTransformSpec() {
        return transformSpec;
    }

    @Override
    public void start(ISchema schema) {
        final String schemaName = schema.getName();

        if (!this.pipeline.getPipelineConfig().isEnabled()) {
            log.warn("The event processing pipeline is not enabled in this module. The input source of [{}] has no effect.", schema.getName());
            return;
        }

        if (!this.pipeline.getPipelineConfig().isMetricOverEventEnabled()) {
            log.info("The metric over event [{}] is not enabled for this pipeline.", schema);
            return;
        }

        try {
            this.exporter = new MetricOverEventExporter(eventType,
                                                        schemaName,
                                                        applicationContext.getBean(ObjectMapper.class),
                                                        applicationContext.getBean(IMetaStorage.class),
                                                        applicationContext.getBean(IMetricStorage.class),
                                                        applicationContext.getBean(SchemaManager.class),
                                                        applicationContext.getBean(MetricPipelineConfig.class));

            pipeline.link(this.exporter);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        if (this.exporter != null) {
            pipeline.unlink(this.exporter);
        }
    }

    @Override
    public SamplingResult sample(ISchema schema, Duration timeout) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * For easier debugging
     */
    @Override
    public String toString() {
        return this.eventType;
    }
}
