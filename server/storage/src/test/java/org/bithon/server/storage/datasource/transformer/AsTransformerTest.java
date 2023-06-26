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

package org.bithon.server.storage.datasource.transformer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.bithon.server.storage.datasource.input.InputRow;
import org.bithon.server.storage.datasource.input.transformer.AsTransformer;
import org.bithon.server.storage.datasource.input.transformer.ITransformer;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/6/26 22:15
 */
public class AsTransformerTest {
    @Test
    public void test() throws JsonProcessingException {
        AsTransformer transformer = new AsTransformer("a", "a1");

        // deserialize from json to test deserialization
        ObjectMapper om = new ObjectMapper();
        String transformerText = om.writeValueAsString(transformer);
        ITransformer newTransformer = om.readValue(transformerText, ITransformer.class);

        InputRow row1 = new InputRow(new HashMap<>(ImmutableMap.of("a", "default")));
        newTransformer.transform(row1);
        Assert.assertEquals("default", row1.getCol("a"));
        Assert.assertEquals("default", row1.getCol("a1"));
    }
}
