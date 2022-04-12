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

package org.bithon.server.storage.datasource.flatten;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.bithon.server.storage.datasource.input.IInputRow;
import org.bithon.server.storage.datasource.input.InputRow;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;

/**
 * @author Frank Chen
 * @date 12/4/22 12:18 AM
 */
public class TreePathFlattenerTest {

    @Test
    public void test() throws JsonProcessingException {
        TreePathFlattener flattener = new TreePathFlattener("f1", "tags.name");

        // serialization and deserialization
        ObjectMapper om = new ObjectMapper();
        String json = om.writeValueAsString(flattener);
        IFlattener newFlattener = om.readValue(json, IFlattener.class);

        IInputRow row1 = new InputRow(new HashMap<>(ImmutableMap.of(
            "tags", ImmutableMap.of("name", "Frank")
        )));
        newFlattener.flatten(row1);

        // 'tags.name' has been flattened as 'f1'
        Assert.assertEquals("Frank", row1.getCol("f1"));
    }

    @Test
    public void flattenTargetPathNotExist_LeafNotExist() throws JsonProcessingException {
        TreePathFlattener flattener = new TreePathFlattener("f1", "tags.no_exist");

        // serialization and deserialization
        ObjectMapper om = new ObjectMapper();
        String json = om.writeValueAsString(flattener);
        IFlattener newFlattener = om.readValue(json, IFlattener.class);

        IInputRow row1 = new InputRow(new HashMap<>(ImmutableMap.of(
            "tags", ImmutableMap.of("name", "Frank")
        )));
        newFlattener.flatten(row1);

        Assert.assertNull(row1.getCol("f1"));
    }

    @Test
    public void flattenTargetPathNotExist_NonLeafNotExist() throws JsonProcessingException {
        TreePathFlattener flattener = new TreePathFlattener("f1", "no_exist.name");

        // serialization and deserialization
        ObjectMapper om = new ObjectMapper();
        String json = om.writeValueAsString(flattener);
        IFlattener newFlattener = om.readValue(json, IFlattener.class);

        IInputRow row1 = new InputRow(new HashMap<>(ImmutableMap.of(
            "tags", ImmutableMap.of("name", "Frank")
        )));
        newFlattener.flatten(row1);

        Assert.assertNull(row1.getCol("f1"));
    }

    @Test
    public void flattenOneLevelPath() throws JsonProcessingException {
        TreePathFlattener flattener = new TreePathFlattener("f1", "tags");

        // serialization and deserialization
        ObjectMapper om = new ObjectMapper();
        String json = om.writeValueAsString(flattener);
        IFlattener newFlattener = om.readValue(json, IFlattener.class);

        IInputRow row1 = new InputRow(new HashMap<>(ImmutableMap.of(
            "tags", ImmutableMap.of("name", "Frank")
        )));
        newFlattener.flatten(row1);

        Assert.assertEquals(row1.getCol("tags"), row1.getCol("f1"));
    }
}
