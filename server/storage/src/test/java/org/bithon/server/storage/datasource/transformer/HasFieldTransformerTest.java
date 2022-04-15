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
import org.bithon.server.storage.datasource.input.transformer.HasFieldTransformer;
import org.bithon.server.storage.datasource.input.transformer.ITransformer;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;

/**
 * @author Frank Chen
 * @date 14/4/22 5:26 PM
 */
public class HasFieldTransformerTest {

    @Test
    public void testHasField() throws JsonProcessingException {
        HasFieldTransformer transformer = new HasFieldTransformer("f1",
                                                                  "r1",
                                                                  1,
                                                                  0);

        // serialization and deserialization
        ObjectMapper om = new ObjectMapper();
        String json = om.writeValueAsString(transformer);
        ITransformer newTransformer = om.readValue(json, ITransformer.class);

        // input row has such field
        {
            InputRow row1 = new InputRow(new HashMap<>(ImmutableMap.of("f1", "default.user")));
            newTransformer.transform(row1);
            Assert.assertEquals(1, row1.getCol("r1"));
        }

        // input row does not have such field
        {
            InputRow row2 = new InputRow(new HashMap<>(ImmutableMap.of("no-f1", "default.user")));
            newTransformer.transform(row2);
            Assert.assertEquals(0, row2.getCol("r1"));
        }
    }
}
