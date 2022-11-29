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

package org.bithon.server.sink.tracing.index;

import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.sink.tracing.TraceSinkConfig;
import org.bithon.server.storage.tracing.TraceSpan;
import org.bithon.server.storage.tracing.index.TagIndex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Generate tag index for given span logs
 *
 * @author frank.chen021@outlook.com
 * @date 3/3/22 12:06 PM
 */
public class TagIndexGenerator {
    /**
     * use TraceConfig so that if the configuration changes dynamically, the latest configuration can be used immediately.
     */
    private final TagIndexConfig config;

    public TagIndexGenerator(TraceSinkConfig config) {
        this.config = config.getIndexes();
    }

    public List<TagIndex> generate(Collection<TraceSpan> spans) {
        if (this.config == null || CollectionUtils.isEmpty(config.getMap()) || spans.isEmpty()) {
            return Collections.emptyList();
        }

        List<TagIndex> indexes = new ArrayList<>();

        Collection<String> tagNames = this.config.getMap().keySet();
        for (TraceSpan span : spans) {
            for (String tagName : tagNames) {
                String value = span.getTag(tagName);
                if (!StringUtils.hasText(value)) {
                    continue;
                }
                indexes.add(TagIndex.builder()
                                    .timestamp(span.getStartTime() / 1000)
                                    .traceId(span.getTraceId())
                                    .name(tagName)
                                    .value(value)
                                    .build());
            }
        }

        return indexes;
    }
}
