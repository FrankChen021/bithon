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
import org.bithon.server.storage.datasource.input.transformer.ChainTransformer;
import org.bithon.server.storage.datasource.input.transformer.ITransformer;
import org.bithon.server.storage.datasource.input.transformer.SplitterTransformer;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;

/**
 * @author Frank Chen
 * @date 12/4/22 11:01 AM
 */
public class ChainTransformerTest {

    @Test
    public void testOneTransformer() throws JsonProcessingException {
        ChainTransformer transformer = new ChainTransformer(new SplitterTransformer("f1", "\\.", "database", "table"));

        // serialization and deserialization
        ObjectMapper om = new ObjectMapper();
        String json = om.writeValueAsString(transformer);
        ITransformer newTransformer = om.readValue(json, ITransformer.class);

        InputRow row1 = new InputRow(new HashMap<>(ImmutableMap.of("f1", "default.user")));
        newTransformer.transform(row1);
        Assert.assertEquals("default", row1.getCol("database"));
        Assert.assertEquals("user", row1.getCol("table"));
    }

    /**
     * input:
     * f1 = default.tmp (d50f2e66-6283-4883-950f-2e6662833883)
     * <p>
     * output:
     * database: default
     * table: tmp
     * <p>
     * So, the transformers:
     * 1. split the value of 'f1' by space to get default.tmp
     * 2. split the 'default.tmp' by dot character to get the db name and table name
     */
    @Test
    public void testMultipleTransformers() throws JsonProcessingException {
        ChainTransformer transformer = new ChainTransformer(new SplitterTransformer("f1", " ", "fullTableName"),
                                                            new SplitterTransformer("fullTableName", "\\.", "database", "table")
        );

        // serialization and deserialization
        ObjectMapper om = new ObjectMapper();
        String json = om.writeValueAsString(transformer);
        ITransformer newTransformer = om.readValue(json, ITransformer.class);

        InputRow row1 = new InputRow(new HashMap<>(ImmutableMap.of("f1", "default.tmp (d50f2e66-6283-4883-950f-2e6662833883)")));
        newTransformer.transform(row1);
        Assert.assertEquals("default", row1.getCol("database"));
        Assert.assertEquals("tmp", row1.getCol("table"));
    }
}
