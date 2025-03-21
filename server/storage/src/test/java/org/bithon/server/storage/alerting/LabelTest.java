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

package org.bithon.server.storage.alerting;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author frank.chen021@outlook.com
 * @date 18/3/25 10:48 pm
 */
public class LabelTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void testLabelSerialization() throws JsonProcessingException {
        Label label = Label.builder().add("k", "test-label").build();
        String json = objectMapper.writeValueAsString(label);
        assertEquals("\"k=test-label\"", json);
    }

    @Test
    void testLabelDeserialization() throws IOException {
        String json = "\"k=test-label\"";
        Label label = objectMapper.readValue(json, Label.class);

        assertEquals("test-label", label.get("k"));
        assertEquals(Label.builder().add("k", "test-label").build(), label);
    }

    @Test
    void testLabelAsMapKey() throws IOException {
        // Given
        Map<Label, String> map = new HashMap<>();
        map.put(Label.builder().add("k", "k1").build(), "value1");
        map.put(Label.builder().add("k", "k2").build(), "value2");

        String json = objectMapper.writeValueAsString(map);
        Map<Label, String> deserializedMap = objectMapper.readValue(
            json,
            objectMapper.getTypeFactory().constructMapType(Map.class, Label.class, String.class)
        );

        assertEquals(2, deserializedMap.size());
        assertEquals("value1", deserializedMap.get(Label.builder().add("k", "k1").build()));
        assertEquals("value2", deserializedMap.get(Label.builder().add("k", "k2").build()));
    }

    @Test
    void testLabelContainsComma() throws JsonProcessingException {
        Label label = Label.builder().add("k", "v1,").build();
        String json = objectMapper.writeValueAsString(label);
        Assertions.assertEquals("\"k=v1&quote;\"", json);

        Label deserialized = objectMapper.readValue(json, Label.class);
        Assertions.assertEquals(label, deserialized);
    }
}
