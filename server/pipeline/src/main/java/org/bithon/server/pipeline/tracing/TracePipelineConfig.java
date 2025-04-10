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

package org.bithon.server.pipeline.tracing;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bithon.server.pipeline.common.BatchConfig;
import org.bithon.server.pipeline.common.pipeline.PipelineConfig;
import org.bithon.server.pipeline.tracing.mapping.TraceIdMappingConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 *
 * @author frank.chen021@outlook.com
 * @date 10/12/21 3:33 PM
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Configuration
@ConfigurationProperties(prefix = "bithon.pipelines.traces")
public class TracePipelineConfig extends PipelineConfig {

    private boolean metricOverSpanEnabled = true;

    // NOTE: if change to this property name, please also make sure to change the corresponding property name in TraceIdMappingBatchExtractor
    private List<TraceIdMappingConfig> mapping;

    private BatchConfig batch;
}
