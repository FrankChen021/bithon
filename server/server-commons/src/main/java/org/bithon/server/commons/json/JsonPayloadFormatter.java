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

package org.bithon.server.commons.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * @author Frank Chen
 * @date 8/4/24 9:35 pm
 */
public enum JsonPayloadFormatter {

    YAML {
        @Override
        public String format(String jsonString, ObjectMapper jsonDeserializer, Function<Object, Object> transformer) {

            try {
                Object deserialized = jsonDeserializer.readValue(jsonString,
                                                                 // Use LinkedHashMap
                                                                 // to keep the order of fields in the original payload
                                                                 new TypeReference<LinkedHashMap<String, Object>>() {
                                                                 });
                if (transformer != null) {
                    deserialized = transformer.apply(deserialized);
                }

                //noinspection unchecked
                if (((Map<String, Object>) deserialized).isEmpty()) {
                    // YAML will serialize an empty map as {}.
                    // However, we want it to be serialized as empty string
                    return "";
                }

                ObjectMapper om = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                                                                    .enable(YAMLGenerator.Feature.INDENT_ARRAYS)
                                                                    .enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE));

                return om.writeValueAsString(deserialized);

            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    },

    JSON {
        @Override
        public String format(String jsonString, ObjectMapper jsonDeserializer, Function<Object, Object> transformer) {
            return jsonString;
        }
    };

    public abstract String format(String jsonString, ObjectMapper jsonDeserializer, Function<Object, Object> transformer);

    public static JsonPayloadFormatter get(String format) {
        if ("json".equalsIgnoreCase(format)) {
            return JSON;
        }
        if ("yaml".equalsIgnoreCase(format)) {
            return YAML;
        }
        throw new RuntimeException("Unknown format " + format);
    }
}
