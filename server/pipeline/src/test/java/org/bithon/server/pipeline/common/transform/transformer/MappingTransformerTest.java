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

package org.bithon.server.pipeline.common.transform.transformer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.bithon.server.storage.datasource.input.InputRow;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;

/**
 * @author frank.chen021@outlook.com
 * @date 11/4/22 11:37 PM
 */
public class MappingTransformerTest {

    /**
     * mapping value of field 'o1'
     */
    @Test
    public void basicMapping() throws JsonProcessingException {
        MappingTransformer transformer = new MappingTransformer("o1",
                                                                ImmutableMap.of("v1", "v1-new",
                                                                                "v2", "v2-new"));

        // deserialize from json to test deserialization
        ObjectMapper om = new ObjectMapper();
        String transformerText = om.writeValueAsString(transformer);
        ITransformer newTransformer = om.readValue(transformerText, ITransformer.class);

        InputRow row1 = new InputRow(new HashMap<>(ImmutableMap.of("o1", "v1")));
        newTransformer.transform(row1);
        Assert.assertEquals("v1-new", row1.getCol("o1"));

        InputRow row2 = new InputRow(new HashMap<>(ImmutableMap.of("o1", "v2")));
        newTransformer.transform(row2);
        Assert.assertEquals("v2-new", row2.getCol("o1"));

        // v3 is not in the map, will map to original value
        InputRow row3 = new InputRow(new HashMap<>(ImmutableMap.of("o1", "v3")));
        newTransformer.transform(row3);
        Assert.assertEquals("v3", row3.getCol("o1"));
    }
}
