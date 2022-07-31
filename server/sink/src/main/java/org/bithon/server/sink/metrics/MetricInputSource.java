package org.bithon.server.sink.metrics;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import lombok.extern.slf4j.Slf4j;
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
 * @author frank.chen
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
                                                  applicationContext.getBean(IMetaStorage.class),
                                                  applicationContext.getBean(IMetricStorage.class),
                                                  applicationContext.getBean(DataSourceSchemaManager.class)));
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
