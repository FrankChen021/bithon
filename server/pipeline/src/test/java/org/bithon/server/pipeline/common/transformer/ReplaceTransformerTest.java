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

/**
 * @author frank.chen021@outlook.com
 * @date 2024/3/14 21:07
 */
public class ReplaceTransformerTest {

    @Test
    public void testSerializationAndDeserialization() throws JsonProcessingException {
        ITransformer transformer = new ReplaceTransformer("col1",
                                                          "bison",
                                                          "bithon",
                                                          null);

        // deserialize from JSON to test deserialization
        ObjectMapper om = new ObjectMapper();
        String transformerText = om.writeValueAsString(transformer);
        ITransformer newTransformer = om.readValue(transformerText, ITransformer.class);

        InputRow row = new InputRow(new HashMap<>(ImmutableMap.of("col1", "bison is bison")));
        newTransformer.transform(row);
        Assertions.assertEquals("bithon is bithon", row.getCol("col1"));
    }

    @Test
    public void test_WhereConditionTrue() throws JsonProcessingException {
        ITransformer transformer = new ReplaceTransformer("col1",
                                                          "bison",
                                                          "bithon",
                                                          "col2 = 'a'");

        // deserialize from JSON to test deserialization
        ObjectMapper om = new ObjectMapper();
        String transformerText = om.writeValueAsString(transformer);
        ITransformer newTransformer = om.readValue(transformerText, ITransformer.class);

        {
            InputRow row = new InputRow(new HashMap<>(ImmutableMap.of("col1", "bison is bison",
                                                                      "col2", "a")));
            newTransformer.transform(row);
            Assertions.assertEquals("bithon is bithon", row.getCol("col1"));
        }

        {
            InputRow row = new InputRow(new HashMap<>(ImmutableMap.of("col1", "bison is bison",
                                                                      // does not meet the 'where' clause
                                                                      "col2", "aaaaa")));
            newTransformer.transform(row);
            Assertions.assertEquals("bison is bison", row.getCol("col1"));
        }

    }
}
