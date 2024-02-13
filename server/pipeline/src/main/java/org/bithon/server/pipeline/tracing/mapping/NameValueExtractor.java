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
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.tracing.TraceSpan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Extract a corresponding id from a name-value pair in tags
 *
 * @author frank.chen021@outlook.com
 * @date 10/12/21 4:32 PM
 */
public class NameValueExtractor implements ITraceIdMappingExtractor {

    private final Collection<String> tags;

    public NameValueExtractor(Collection<String> tags) {
        this.tags = tags;
    }

    /**
     * This object is created by deserializing the SpringBoot configuration.
     * However, it treats the List/Set in the configuration as the type of Map.
     */
    @JsonCreator
    public NameValueExtractor(@JsonProperty("tags") Map<String, String> tags) {
        this(new ArrayList<>(tags.values()));
    }

    @Override
    public void extract(TraceSpan span, BiConsumer<TraceSpan, String> consumer) {
        for (String tag : tags) {
            String value = span.getTags().get(tag);
            if (StringUtils.hasText(value)) {
                consumer.accept(span, value);
            }
        }
    }
}
