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

package org.bithon.server.commons.serializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.bithon.component.commons.utils.HumanReadablePercentage;
import org.bithon.component.commons.utils.Preconditions;

import java.io.IOException;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/2/11 12:01
 */
public class HumanReadablePercentageDeserializer extends JsonDeserializer<HumanReadablePercentage> {
    @Override
    public HumanReadablePercentage deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        if (node.isObject()) {
            String type = node.get("type").asText();
            Preconditions.checkIfTrue("percentage".equals(type), "Expecting type to be 'percentage' but got '%s'", type);
            return HumanReadablePercentage.of(node.get("value").asText());
        } else {
            // Simple string
            if (node.isValueNode()) {
                return HumanReadablePercentage.of(node.asText());
            }
            throw new IllegalArgumentException("Expecting a value node but got " + node.getNodeType());
        }
    }

    @Override
    public Class<?> handledType() {
        return HumanReadablePercentage.class;
    }
}
