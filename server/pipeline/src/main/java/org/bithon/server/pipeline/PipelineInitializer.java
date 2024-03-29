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

package org.bithon.server.pipeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.pipeline.common.pipeline.AbstractPipeline;
import org.bithon.server.pipeline.event.EventPipeline;
import org.bithon.server.pipeline.event.EventPipelineConfig;
import org.bithon.server.pipeline.metrics.MetricPipeline;
import org.bithon.server.pipeline.metrics.MetricPipelineConfig;
import org.bithon.server.pipeline.metrics.input.MetricInputSourceManager;
import org.bithon.server.pipeline.tracing.TracePipeline;
import org.bithon.server.pipeline.tracing.TracePipelineConfig;
import org.bithon.server.storage.InvalidConfigurationException;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Extra pipeline support.
 * Allows adding more than one pipeline.
 * It's mainly designed for test that runs two pipelines in one deployment.
 * See the application-pipeline-to-kafka-to-local.yml configuration for more.
 *
 * @author Frank Chen
 * @date 24/1/24 2:24 pm
 */
@Component
public class PipelineInitializer implements SmartLifecycle {

    static class AllPipelineConfig extends HashMap<String, Map<String, Object>> {
    }

    private final List<AbstractPipeline<?, ?>> pipelines = new ArrayList<>();

    public PipelineInitializer(MetricInputSourceManager metricInputSourceManager, ObjectMapper objectMapper, Environment env) {
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
                        pipelines.add(new MetricPipeline(binder.bind(prefix, MetricPipelineConfig.class).get(), metricInputSourceManager, objectMapper));
                    } else if ("traces".equals(realType)) {
                        pipelines.add(new TracePipeline(binder.bind(prefix, TracePipelineConfig.class).get(), metricInputSourceManager, objectMapper));
                    } else if ("events".equals(realType)) {
                        pipelines.add(new EventPipeline(binder.bind(prefix, EventPipelineConfig.class).get(), metricInputSourceManager, objectMapper));
                    } else {
                        throw new InvalidConfigurationException(StringUtils.format("Unknown value [%s] of property [%s]", realType, prefix));
                    }
                    break;
            }
        }
    }

    @Override
    public void start() {
        for (AbstractPipeline<?, ?> pipeline : this.pipelines) {
            pipeline.start();
        }
    }

    @Override
    public void stop() {
        for (AbstractPipeline<?, ?> pipeline : this.pipelines) {
            pipeline.stop();
        }
    }

    @Override
    public boolean isRunning() {
        return false;
    }
}
