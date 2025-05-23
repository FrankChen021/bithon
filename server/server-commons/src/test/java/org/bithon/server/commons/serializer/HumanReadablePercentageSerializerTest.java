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
import org.bithon.component.commons.utils.HumanReadablePercentage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * @author Frank Chen
 * @date 16/2/24 10:33 am
 */
public class HumanReadablePercentageSerializerTest {
    @Test
    public void test_Deserialization() throws JsonProcessingException {
        ObjectMapper om = new ObjectMapper();
        SimpleModule m = new SimpleModule();
        m.addDeserializer(HumanReadablePercentage.class, new HumanReadablePercentageDeserializer());
        m.addSerializer(HumanReadablePercentage.class, new HumanReadablePercentageSerializer());
        om.registerModule(m);

        String serialized = om.writeValueAsString(HumanReadablePercentage.of("50%"));
        HumanReadablePercentage percentage = om.readValue(serialized, HumanReadablePercentage.class);
        Assertions.assertEquals("50%", percentage.toString());
    }

    @Test
    public void test_Deserialization_SimpleString() throws JsonProcessingException {
        ObjectMapper om = new ObjectMapper();
        SimpleModule m = new SimpleModule();
        m.addDeserializer(HumanReadablePercentage.class, new HumanReadablePercentageDeserializer());
        m.addSerializer(HumanReadablePercentage.class, new HumanReadablePercentageSerializer());
        om.registerModule(m);

        HumanReadablePercentage percentage = om.readValue("\"50%\"", HumanReadablePercentage.class);
        Assertions.assertEquals("50%", percentage.toString());
    }
}
