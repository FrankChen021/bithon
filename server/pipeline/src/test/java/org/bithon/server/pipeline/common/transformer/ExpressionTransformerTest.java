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
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/3/14 21:30
 */
public class ExpressionTransformerTest {

    @Test
    public void testArithmeticExpression() throws JsonProcessingException {
        ITransformer transformer = new ExpressionTransformer("1+2", "bison", null);

        // deserialize from json to test deserialization
        ObjectMapper om = new ObjectMapper();
        String transformerText = om.writeValueAsString(transformer);
        ITransformer newTransformer = om.readValue(transformerText, ITransformer.class);

        InputRow row = new InputRow(new HashMap<>());
        newTransformer.transform(row);
        Assert.assertEquals(3L, row.getCol("bison"));
    }


    @Test
    public void test_ContainsExpression() throws JsonProcessingException {
        ITransformer transformer = new ExpressionTransformer("a contains 'bison'", "bison", null);

        // deserialize from json to test deserialization
        ObjectMapper om = new ObjectMapper();
        String transformerText = om.writeValueAsString(transformer);
        ITransformer newTransformer = om.readValue(transformerText, ITransformer.class);

        InputRow row = new InputRow(new HashMap<>(ImmutableMap.of("a", "bison is a kind of animal")));
        newTransformer.transform(row);
        Assert.assertEquals(true, row.getCol("bison"));
    }
}
