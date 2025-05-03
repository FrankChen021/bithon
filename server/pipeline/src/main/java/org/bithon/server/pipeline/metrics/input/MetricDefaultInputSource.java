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

package org.bithon.server.pipeline.metrics.input;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.bithon.server.datasource.ISchema;
import org.bithon.server.pipeline.common.transformer.TransformSpec;
import org.bithon.server.pipeline.metrics.MetricPipelineConfig;
import org.bithon.server.pipeline.metrics.exporter.MetricMessageHandler;
import org.bithon.server.pipeline.metrics.exporter.MetricMessageHandlers;
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
@JsonTypeName("metric")
public class MetricDefaultInputSource implements IMetricInputSource {

    private final TransformSpec transformSpec;
    private final ApplicationContext applicationContext;
    private String name;

    @JsonCreator
    public MetricDefaultInputSource(@JsonProperty("transformSpec") @NotNull TransformSpec transformSpec,
                                    @JacksonInject(useInput = OptBoolean.FALSE) ApplicationContext applicationContext) {
        this.transformSpec = transformSpec;
        this.applicationContext = applicationContext;
    }

    @Override
    public TransformSpec getTransformSpec() {
        return transformSpec;
    }

    @Override
    public void start(ISchema schema) {
        name = schema.getName();
        try {
            MetricMessageHandlers.getInstance()
                                 .add(new MetricMessageHandler(name,
                                                               applicationContext.getBean(IMetaStorage.class),
                                                               applicationContext.getBean(IMetricStorage.class),
                                                               applicationContext.getBean(SchemaManager.class),
                                                               this.transformSpec,
                                                               applicationContext.getBean(MetricPipelineConfig.class)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        if (name != null) {
            MetricMessageHandler handler = MetricMessageHandlers.getInstance().remove(name);
            if (handler != null) {
                handler.close();
            }
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
        return name;
    }
}
