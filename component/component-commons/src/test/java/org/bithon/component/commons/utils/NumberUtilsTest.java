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


package org.bithon.component.commons.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author frank.chen021@outlook.com
 * @date 14/4/22 3:46 PM
 */
public class NumberUtilsTest {

    @Test
    public void testHexString() {
        Assertions.assertEquals("ff", NumberUtils.toHexString(new byte[]{-1}));

        Assertions.assertEquals("00", NumberUtils.toHexString(new byte[]{0}));

        Assertions.assertEquals("01", NumberUtils.toHexString(new byte[]{1}));

        Assertions.assertEquals("1f", NumberUtils.toHexString(new byte[]{0x1F}));

        Assertions.assertEquals("1b", NumberUtils.toHexString(new byte[]{0x1b}));
    }
}
