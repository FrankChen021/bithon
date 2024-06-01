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

package org.bithon.component.commons.uuid;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author frank.chen021@outlook.com
 * @date 14/5/24 10:00 pm
 */
public class UUIDv7GeneratorTest {
    @Test
    public void test() {
        UUIDv7Generator v7 = UUIDv7Generator.create(UUIDv7Generator.INCREMENT_TYPE_PLUS_N);
        for (int i = 0; i < 1000; i++) {
            UUID uuid = v7.generate();
            String compact = uuid.toCompactFormat();
            String full = uuid.toUUIDFormat();
System.out.println(compact);
            Assert.assertEquals(32, compact.length());
            Assert.assertEquals(36, full.length());
            Assert.assertEquals(compact, full.replace("-", ""));
        }
    }
}
