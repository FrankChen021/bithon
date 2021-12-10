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

package org.bithon.server.tracing.mapping;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.tracing.sink.TraceSpan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * @author Frank Chen
 * @date 10/12/21 4:32 PM
 */
public class NameValueExtractor implements ITraceMappingExtractor {

    private final Collection<String> names;

    public NameValueExtractor(@JsonProperty("names") Collection<String> names) {
        this.names = names;
    }

    @JsonCreator
    public NameValueExtractor(@JsonProperty("names") Map<String, String> names) {
        this(new ArrayList<>(names.values()));
    }

    @Override
    public void extract(TraceSpan span, BiConsumer<TraceSpan, String> callback) {
        for (String name : names) {
            String value = span.getTags().get(name);
            if (StringUtils.hasText(value)) {
                callback.accept(span, value);
            }
        }
    }
}
