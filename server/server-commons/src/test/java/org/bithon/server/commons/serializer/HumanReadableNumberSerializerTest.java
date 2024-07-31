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
import org.junit.Assert;
import org.junit.Test;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/3/3 20:13
 */
public class HumanReadableNumberSerializerTest {

    @Test
    public void test_Deserialization() throws JsonProcessingException {
        ObjectMapper om = new ObjectMapper();
        SimpleModule m = new SimpleModule();
        m.addDeserializer(HumanReadableNumber.class, new HumanReadableSizeDeserializer());
        m.addSerializer(HumanReadableNumber.class, new HumanReadableSizeSerializer());
        om.registerModule(m);

        Assert.assertEquals("5G", om.readValue(om.writeValueAsString(HumanReadableNumber.of("5G")), HumanReadableNumber.class)
                                    .toString());

        Assert.assertEquals("5Gi", om.readValue(om.writeValueAsString(HumanReadableNumber.of("5Gi")), HumanReadableNumber.class)
                                     .toString());

        Assert.assertEquals("5GiB", om.readValue(om.writeValueAsString(HumanReadableNumber.of("5GiB")), HumanReadableNumber.class)
                                      .toString());
    }
}
