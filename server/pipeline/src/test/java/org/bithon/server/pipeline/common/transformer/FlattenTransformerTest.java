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

package org.bithon.server.pipeline.common.transformer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.bithon.server.storage.datasource.input.InputRow;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/6/26 22:15
 */
public class FlattenTransformerTest {
    @Test
    public void test() throws JsonProcessingException {
        FlattenTransformer transformer = new FlattenTransformer(new String[]{"a"}, new String[]{"a1"}, null);

        // deserialize from json to test deserialization
        ObjectMapper om = new ObjectMapper();
        String transformerText = om.writeValueAsString(transformer);
        ITransformer newTransformer = om.readValue(transformerText, ITransformer.class);

        InputRow row1 = new InputRow(new HashMap<>(ImmutableMap.of("a", "default")));
        newTransformer.transform(row1);
        Assertions.assertEquals("default", row1.getCol("a"));
        Assertions.assertEquals("default", row1.getCol("a1"));
    }

    @Test
    public void test_Flatten_NestedProperties() throws JsonProcessingException {
        FlattenTransformer transformer = new FlattenTransformer(new String[]{"a.b", "a.c.d"},
                                                                new String[]{"b1", "d1"},
                                                                null);

        // deserialize from json to test deserialization
        ObjectMapper om = new ObjectMapper();
        String transformerText = om.writeValueAsString(transformer);
        ITransformer newTransformer = om.readValue(transformerText, ITransformer.class);

        Map<String, Map<String, Object>> map = new HashMap<>();
        Map<String, Object> a = map.compute("a", (k, v) -> new HashMap<>());
        a.put("b", "b-value");
        a.put("c", ImmutableMap.of("d", "d-value"));

        //noinspection unchecked
        InputRow row1 = new InputRow((Map<String, Object>) (Map<?, ?>) map);
        newTransformer.transform(row1);

        Assertions.assertEquals("b-value", row1.getCol("b1"));
        Assertions.assertEquals("d-value", row1.getCol("d1"));
    }
}
