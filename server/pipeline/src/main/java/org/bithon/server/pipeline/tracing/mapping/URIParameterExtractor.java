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

package org.bithon.server.pipeline.tracing.mapping;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.commons.utils.UrlUtils;
import org.bithon.server.storage.tracing.TraceSpan;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Extract a corresponding id from a value in tags which is formatted as URI
 *
 * @author frank.chen021@outlook.com
 * @date 10/12/21 3:13 PM
 */
@Slf4j
public class URIParameterExtractor implements ITraceIdMappingExtractor {

    /**
     * key - The tag name whose value has the form of URL
     * val - The parameter name in the URL
     */
    private final Map<String, Set<String>> args;

    /**
     * This object is created by deserializing the SpringBoot configuration.
     * However, it treats the List/Set in the configuration as the type of Map.
     */
    @JsonCreator
    public URIParameterExtractor(@JsonProperty("tags") Map<String, Map<String, String>> tags) {
        this.args = new HashMap<>();

        tags.forEach((key, val) -> this.args.put(key, new HashSet<>(val.values())));
    }

    @Override
    public void extract(TraceSpan span, BiConsumer<TraceSpan, String> consumer) {
        for (Map.Entry<String, Set<String>> entry : args.entrySet()) {
            String tag = entry.getKey();
            Set<String> parameters = entry.getValue();

            String val = span.getTag(tag);
            if (!StringUtils.hasText(val)) {
                continue;
            }

            UrlUtils.parseURLParameters(val, parameters)
                    .values()
                    .forEach((v) -> consumer.accept(span, v));
        }
    }
}
