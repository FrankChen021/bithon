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

package org.bithon.server.storage.datasource;

import com.google.common.collect.ImmutableMap;
import org.bithon.server.storage.datasource.input.IInputRow;
import org.bithon.server.storage.datasource.input.InputRow;
import org.bithon.server.storage.datasource.input.TransformSpec;
import org.bithon.server.storage.datasource.input.filter.EqualFilter;
import org.bithon.server.storage.datasource.input.flatten.TreePathFlattener;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;

/**
 * @author frank.chen021@outlook.com
 * @date 11/4/22 11:37 PM
 */
public class TransformSpecTest {

    @Test
    public void test() {
        TransformSpec transformSpec = TransformSpec.builder()
                                                   .prefilter(new EqualFilter("appName", "bithon-server"))
                                                   .flatteners(Collections.singletonList(new TreePathFlattener("database", "tags.view")))
                                                   //.transformers()
                                                   .postfilter(new EqualFilter("database", "jvm-metrics"))
                                                   .build();

        IInputRow row = new InputRow(new HashMap<>(ImmutableMap.of(
            "appName", "bithon-server",
            "tags", ImmutableMap.of("view", "jvm-metrics")
        )));
        Assert.assertTrue(transformSpec.transform(row));

        // flattened property
        Assert.assertEquals("jvm-metrics", row.getCol("database"));
    }
}
