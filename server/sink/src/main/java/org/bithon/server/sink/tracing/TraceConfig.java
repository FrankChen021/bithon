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

package org.bithon.server.sink.tracing;

import lombok.Data;
import org.bithon.server.sink.tracing.index.TagIndexConfig;
import org.bithon.server.sink.tracing.mapping.TraceIdMappingConfig;
import org.bithon.server.sink.tracing.sanitization.SanitizerConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * @author Frank Chen
 * @date 10/12/21 3:33 PM
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "bithon.tracing")
public class TraceConfig {
    private List<TraceIdMappingConfig> mapping;

    private SanitizerConfig globalSanitizer;
    private Map<String, SanitizerConfig> applicationSanitizer;

    private TagIndexConfig indexes;
}
