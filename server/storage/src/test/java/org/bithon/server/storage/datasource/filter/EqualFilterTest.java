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

package org.bithon.server.storage.datasource.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.bithon.server.storage.datasource.input.InputRow;
import org.bithon.server.storage.datasource.input.filter.EqualFilter;
import org.bithon.server.storage.datasource.input.filter.IInputRowFilter;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Frank Chen
 * @date 11/4/22 11:48 PM
 */
public class EqualFilterTest {

    @Test
    public void test() throws JsonProcessingException {
        EqualFilter filter = new EqualFilter("f1", 1);

        ObjectMapper om = new ObjectMapper();
        String json = om.writeValueAsString(filter);
        IInputRowFilter newFilter = om.readValue(json, IInputRowFilter.class);

        Assert.assertTrue(newFilter.shouldInclude(new InputRow(ImmutableMap.of("f1", 1))));
        Assert.assertFalse(newFilter.shouldInclude(new InputRow(ImmutableMap.of("f1", 2))));

        // field not exist
        Assert.assertFalse(newFilter.shouldInclude(new InputRow(ImmutableMap.of("f2", 2))));
    }
}
