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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import lombok.extern.slf4j.Slf4j;
import org.bithon.server.sink.metrics.transformer.TopoTransformers;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.DataSourceSchemaManager;
import org.bithon.server.storage.datasource.input.IInputSource;
import org.bithon.server.storage.datasource.input.TransformSpec;
import org.bithon.server.storage.meta.IMetaStorage;
import org.bithon.server.storage.metrics.IMetricStorage;
import org.springframework.context.ApplicationContext;

import javax.validation.constraints.NotNull;
import java.io.IOException;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/7/31 11:28
 */
@Slf4j
@JsonTypeName("metric")
public class MetricInputSource implements IInputSource {

    private final MetricMessageHandlers handlers;
    private final TransformSpec transformSpec;
    private final ApplicationContext applicationContext;
    private String name;

    @JsonCreator
    public MetricInputSource(@JsonProperty("transformSpec") @NotNull TransformSpec transformSpec,
                             @JacksonInject(useInput = OptBoolean.FALSE) MetricMessageHandlers handlers,
                             @JacksonInject(useInput = OptBoolean.FALSE) ApplicationContext applicationContext) {
        this.transformSpec = transformSpec;
        this.handlers = handlers;
        this.applicationContext = applicationContext;
    }

    @Override
    public TransformSpec getTransformSpec() {
        return transformSpec;
    }

    @Override
    public void start(DataSourceSchema schema) {
        name = schema.getName();
        try {
            handlers.add(new MetricMessageHandler(name,
                                                  applicationContext.getBean(TopoTransformers.class),
                                                  applicationContext.getBean(IMetaStorage.class),
                                                  applicationContext.getBean(IMetricStorage.class),
                                                  applicationContext.getBean(DataSourceSchemaManager.class),
                                                  this.transformSpec));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        if (name != null) {
            handlers.remove(name);
        }
    }
}
