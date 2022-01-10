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
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.tracing.sink.TraceSpan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Extract a corresponding id from a value in tags which is formatted as URI
 *
 * @author Frank Chen
 * @date 10/12/21 3:13 PM
 */
@Slf4j
public class URIParameterExtractor implements ITraceIdMappingExtractor {

    private final Collection<String> parameters;

    public URIParameterExtractor(@JsonProperty("parameters") Collection<String> parameters) {
        this.parameters = parameters;
    }

    /**
     * yaml deserialize the value of args parameter on {@link TraceIdMappingConfig} into a Map instead of an expected array
     */
    @JsonCreator
    public URIParameterExtractor(@JsonProperty("parameters") Map<String, String> parameters) {
        this(new ArrayList<>(parameters.values()));
    }

    @Override
    public void extract(TraceSpan span, BiConsumer<TraceSpan, String> callback) {
        Map<String, String> urlParameters = span.getURLParameters();

        for (String parameter : this.parameters) {
            String userTxId = urlParameters.get(parameter);
            if (StringUtils.hasText(userTxId)) {
                callback.accept(span, userTxId);
            }
        }
    }
}
