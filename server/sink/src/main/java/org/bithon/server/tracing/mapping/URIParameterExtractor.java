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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * @author Frank Chen
 * @date 10/12/21 3:13 PM
 */
@Slf4j
public class URIParameterExtractor implements ITraceMappingExtractor {

    private final Collection<String> parameters;

    public URIParameterExtractor(@JsonProperty("parameters") Collection<String> parameters) {
        this.parameters = parameters;
    }

    /**
     * yaml deserialize the value of args parameter on {@link TraceMappingConfig} into a Map instead of an expected array
     */
    @JsonCreator
    public URIParameterExtractor(@JsonProperty("parameters") Map<String, String> parameters) {
        this(new ArrayList<>(parameters.values()));
    }

    @Override
    public void extract(TraceSpan span, BiConsumer<TraceSpan, String> callback) {
        String uriText = span.getTags().get("uri");
        if (!StringUtils.hasText(uriText)) {
            return;
        }

        try {
            URI uri = new URI(uriText);

            Map<String, String> variables = parseQuery(uri.getQuery());

            for (String parameter : this.parameters) {
                String userTxId = variables.get(parameter);
                if (StringUtils.hasText(userTxId)) {
                    callback.accept(span, userTxId);
                }
            }

        } catch (URISyntaxException e) {
            log.warn("Invalid URI[{}] to extract trace mapping: {}", uriText, e.getMessage());
        }
    }

    private Map<String, String> parseQuery(String query) {
        if (!StringUtils.hasText(query)) {
            return Collections.emptyMap();
        }

        Map<String, String> variables = new HashMap<>();
        int fromIndex = 0;
        int toIndex = 0;
        while (toIndex != -1) {
            String name;
            String value;
            toIndex = query.indexOf('=', fromIndex);
            if (toIndex - fromIndex > 1) {
                name = query.substring(fromIndex, toIndex);
                fromIndex = toIndex + 1;
                toIndex = query.indexOf('&', fromIndex);
                if (toIndex == -1) {
                    value = query.substring(fromIndex);
                } else {
                    value = query.substring(fromIndex, toIndex);
                }
                variables.put(name, value);
                fromIndex = toIndex + 1;
            } else {
                fromIndex = query.indexOf('&', toIndex) + 1;
            }
        }
        return variables;
    }
}
