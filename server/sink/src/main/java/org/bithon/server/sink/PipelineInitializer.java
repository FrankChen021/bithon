package org.bithon.server.sink;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.sink.common.pipeline.AbstractPipeline;
import org.bithon.server.sink.event.EventPipeline;
import org.bithon.server.sink.event.EventPipelineConfig;
import org.bithon.server.sink.metrics.MetricPipeline;
import org.bithon.server.sink.metrics.MetricPipelineConfig;
import org.bithon.server.sink.tracing.TracePipeline;
import org.bithon.server.sink.tracing.TracePipelineConfig;
import org.bithon.server.storage.InvalidConfigurationException;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Extra pipeline support.
 * Allows to support more than 1 pipelines under.
 *
 * @author Frank Chen
 * @date 24/1/24 2:24 pm
 */
@Component
public class PipelineInitializer implements SmartLifecycle {

    static class AllPipelineConfig extends HashMap<String, Map<String, Object>> {
    }

    private final List<AbstractPipeline<?, ?>> pipelilnes = new ArrayList<>();

    public PipelineInitializer(ObjectMapper objectMapper, Environment env, ApplicationContext applicationContext) {
        Binder binder = Binder.get(env);
        AllPipelineConfig allPipelineConfig = binder.bind("bithon.pipelines", AllPipelineConfig.class).orElse(new AllPipelineConfig());

        for (Map.Entry<String, Map<String, Object>> entry : allPipelineConfig.entrySet()) {
            String type = entry.getKey();
            Map<String, Object> props = entry.getValue();

            switch (type) {
                case "metrics":
                case "traces":
                case "events":
                    break;
                default:
                    String prefix = "bithon.pipelines." + type;
                    String realType = (String) props.get("type");
                    Preconditions.checkNotNull(type, "The 'type' property is missed under the pipeline property [%s]", prefix);
                    if ("metrics".equals(realType)) {
                        pipelilnes.add(new MetricPipeline(binder.bind(prefix, MetricPipelineConfig.class).get(), objectMapper));
                    } else if ("traces".equals(realType)) {
                        pipelilnes.add(new TracePipeline(binder.bind(prefix, TracePipelineConfig.class).get(), objectMapper));
                    } else if ("events".equals(realType)) {
                        pipelilnes.add(new EventPipeline(binder.bind(prefix, EventPipelineConfig.class).get(), objectMapper));
                    } else {
                        throw new InvalidConfigurationException(StringUtils.format("Unknown value [%s] of property [%s]", realType, prefix));
                    }
                    break;
            }
        }
    }

    @Override
    public void start() {
        for (AbstractPipeline<?, ?> pipeline : this.pipelilnes) {
            pipeline.start();
        }
    }

    @Override
    public void stop() {
        for (AbstractPipeline<?, ?> pipeline : this.pipelilnes) {
            pipeline.stop();
        }
    }

    @Override
    public boolean isRunning() {
        return false;
    }
}
