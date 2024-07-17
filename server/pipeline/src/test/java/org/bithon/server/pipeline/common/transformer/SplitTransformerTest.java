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
 * @date 12/4/22 12:06 AM
 */
public class SplitTransformerTest {

    @Test
    public void test() throws JsonProcessingException {
        ITransformer transformer = new SplitTransformer("o1",
                                                        ".",
                                                        new String[]{"database", "table"},
                                                        null);

        // deserialize from json to test deserialization
        ObjectMapper om = new ObjectMapper();
        String transformerText = om.writeValueAsString(transformer);
        ITransformer newTransformer = om.readValue(transformerText, ITransformer.class);

        InputRow row1 = new InputRow(new HashMap<>(ImmutableMap.of("o1", "default.user")));
        newTransformer.transform(row1);
        Assert.assertEquals("default", row1.getCol("database"));
        Assert.assertEquals("user", row1.getCol("table"));

        // field does not match
        InputRow row2 = new InputRow(new HashMap<>(ImmutableMap.of("o2", "default.user")));
        newTransformer.transform(row2);
        Assert.assertNull(row2.getCol("database"));
        Assert.assertNull(row2.getCol("table"));
    }

    @Test
    public void splitOnNestedObject() {
        SplitTransformer transformer = new SplitTransformer("tags.iterator", "/", new String[]{"current", "max"}, null);

        InputRow row1 = new InputRow(new HashMap<>());
        row1.updateColumn("tags", ImmutableMap.of("iterator", "1/5"));
        transformer.transform(row1);
        Assert.assertEquals("1", row1.getCol("current"));
        Assert.assertEquals("5", row1.getCol("max"));
    }

    @Test
    public void splitOnNestedObject_UsingPropertyAccessException() {
        SplitTransformer transformer = new SplitTransformer("tags['db.table']", "/", new String[]{"current", "max"}, null);

        InputRow row1 = new InputRow(new HashMap<>());
        row1.updateColumn("tags", ImmutableMap.of("db.table", "1/5"));
        transformer.transform(row1);
        Assert.assertEquals("1", row1.getCol("current"));
        Assert.assertEquals("5", row1.getCol("max"));
    }
}
