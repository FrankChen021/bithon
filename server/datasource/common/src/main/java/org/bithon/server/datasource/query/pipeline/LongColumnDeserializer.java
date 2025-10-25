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

package org.bithon.server.datasource.query.pipeline;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.IOException;

/**
 * Custom Jackson deserializer for LongColumn that reconstructs the column from serialized data.
 *
 * @author frank.chen021@outlook.com
 * @date 20/10/25
 */
public class LongColumnDeserializer extends JsonDeserializer<LongColumn> {

    @Override
    public LongColumn deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        
        String name = node.get("name").asText();
        ArrayNode dataNode = (ArrayNode) node.get("data");
        int size = dataNode.size();
        
        long[] data = new long[size];
        for (int i = 0; i < size; i++) {
            data[i] = dataNode.get(i).asLong();
        }
        
        return new LongColumn(name, data, size);
    }
}

