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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.bithon.component.commons.utils.HumanReadableNumber;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/3/3 20:13
 */
public class HumanReadableNumberSerializerTest {

    private ObjectMapper om;

    @BeforeEach
    public void setUp() {
        om = new ObjectMapper();
        SimpleModule m = new SimpleModule();
        m.addDeserializer(HumanReadableNumber.class, new HumanReadableSizeDeserializer());
        m.addSerializer(HumanReadableNumber.class, new HumanReadableSizeSerializer());
        om.registerModule(m);
    }

    @Test
    public void test_SerializationAndDeserialization() throws JsonProcessingException {
        Assertions.assertEquals("5G", om.readValue(om.writeValueAsString(HumanReadableNumber.of("5G")), HumanReadableNumber.class)
                                        .toString());

        Assertions.assertEquals("5Gi", om.readValue(om.writeValueAsString(HumanReadableNumber.of("5Gi")), HumanReadableNumber.class)
                                     .toString());

        Assertions.assertEquals("5GiB", om.readValue(om.writeValueAsString(HumanReadableNumber.of("5GiB")), HumanReadableNumber.class)
                                      .toString());
    }

    @Test
    public void test_DeserializeFromSimpleString() throws JsonProcessingException {
        // Deserialization from simple string
        Assertions.assertEquals("5GiB", om.readValue("\"5GiB\"", HumanReadableNumber.class).toString());

    }
}
