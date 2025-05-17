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

package org.bithon.server.pipeline.tracing.sampler;

import org.bithon.server.commons.spring.EnvironmentBinder;
import org.bithon.server.pipeline.tracing.TracePipelineConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * @author frank.chen021@outlook.com
 * @date 16/7/24 10:17 am
 */
public class TraceSamplingEnabler implements Condition {

    /**
     * Use {@link TracePipelineConfig} to check because it defines default value for some properties
     */
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        ConfigurationProperties properties = TracePipelineConfig.class.getAnnotation(ConfigurationProperties.class);
        String propertyPath = properties.prefix();

        // Since TraceSamplingEnable is initialized before EnvironmentBinder is injected, we have to construct a new EnvironmentBinder
        TracePipelineConfig config = EnvironmentBinder.from((ConfigurableEnvironment) context.getEnvironment())
                                                      .bind(propertyPath, TracePipelineConfig.class);

        return config != null
               && config.isEnabled()
               && config.isMetricOverSpanEnabled();
    }
}
