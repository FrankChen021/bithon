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

package org.bithon.server.pipeline.metrics;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.bithon.server.storage.datasource.input.IInputRow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/11/17 14:44
 */
@Slf4j
public class MetricMessageListDeserializer extends JsonDeserializer<List<IInputRow>> {
    @Override
    public List<IInputRow> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        List<IInputRow> messages = new ArrayList<>();

        ObjectCodec codec = jsonParser.getCodec();
        JsonNode array = codec.readTree(jsonParser);
        if (!array.isArray()) {
            throw new RuntimeException("Failed to deserialize metric message list");
        }

        for (JsonNode node : array) {
            try {
                messages.add(codec.treeToValue(node, MetricMessage.class));
            } catch (JsonProcessingException e) {
                log.warn("Failed to metrics. Error: {}. Input: {},", e.getMessage(), node.toPrettyString());
            }
        }

        return messages;
    }
}
