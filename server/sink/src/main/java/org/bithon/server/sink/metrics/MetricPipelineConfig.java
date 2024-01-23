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

import lombok.Data;
import org.bithon.server.sink.SinkModuleEnabler;
import org.bithon.server.sink.common.BatchConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/11/29 21:37
 */
@Data
@Configuration
@Conditional(SinkModuleEnabler.class)
@ConfigurationProperties(prefix = "bithon.pipeline.metrics")
public class MetricPipelineConfig {
    private boolean enabled = true;

    private List<Map<String, String>> receivers;
    private List<Map<String, String>> transforms;
    private List<Map<String, String>> exporters;

    private BatchConfig batch;
}
