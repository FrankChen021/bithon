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

import org.bithon.server.storage.datasource.input.transformer.ITransformer;
import org.bithon.server.storage.datasource.input.transformer.ProbabilisticSamplerTransform;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Frank Chen
 * @date 25/1/24 5:27 pm
 */
public class ProbabilisticSamplerTransformTest {

    @Test
    public void test() {
        Assert.assertFalse(new ProbabilisticSamplerTransform("0%").transform(null));
        Assert.assertTrue(new ProbabilisticSamplerTransform("100%").transform(null));
    }

    @Test
    public void testProbability() {
        int count = 0;
        ITransformer transformer = new ProbabilisticSamplerTransform("1%");
        for (int i = 0; i < 200; i++) {
            if (transformer.transform(null)) {
                count++;
            }
        }
        Assert.assertTrue(count > 0);
    }
}
