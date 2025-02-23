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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.bithon.component.commons.utils.HumanReadableDuration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/2/11 12:04
 */
public class HumanReadableDurationSerializerTest {

    @Test
    public void serialize() throws IOException {
        ObjectMapper om = new ObjectMapper();
        SimpleModule m = new SimpleModule();
        m.addDeserializer(HumanReadableDuration.class, new HumanReadableDurationDeserializer());
        m.addSerializer(HumanReadableDuration.class, new HumanReadableDurationSerializer());
        om.registerModule(m);

        {
            HumanReadableDuration d = om.readValue(om.writeValueAsBytes(HumanReadableDuration.parse("7m")), HumanReadableDuration.class);
            Assertions.assertEquals(7, d.getDuration().toMinutes());
            Assertions.assertEquals(TimeUnit.MINUTES, d.getUnit());
            Assertions.assertEquals("7m", d.toString());
        }
    }
}
