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

    @Test
    public void testToHexStringLong() {
        // Test zero
        Assertions.assertEquals("0000000000000000", NumberUtils.toHexString(0L));
        
        // Test small positive numbers
        Assertions.assertEquals("0000000000000001", NumberUtils.toHexString(1L));
        Assertions.assertEquals("00000000000000ff", NumberUtils.toHexString(255L));
        Assertions.assertEquals("0000000000001000", NumberUtils.toHexString(4096L));
        
        // Test maximum positive long
        Assertions.assertEquals("7fffffffffffffff", NumberUtils.toHexString(Long.MAX_VALUE));
        
        // Test negative numbers
        Assertions.assertEquals("ffffffffffffffff", NumberUtils.toHexString(-1L));
        Assertions.assertEquals("ffffffffffffff00", NumberUtils.toHexString(-256L));
        Assertions.assertEquals("8000000000000000", NumberUtils.toHexString(Long.MIN_VALUE));
        
        // Test some specific values
        Assertions.assertEquals("0000000000001234", NumberUtils.toHexString(0x1234L));
        Assertions.assertEquals("0000000012345678", NumberUtils.toHexString(0x12345678L));
        Assertions.assertEquals("123456789abcdef0", NumberUtils.toHexString(0x123456789abcdef0L));
        
        // Test that all 16 characters are always present (leading zeros)
        Assertions.assertEquals(16, NumberUtils.toHexString(1L).length());
        Assertions.assertEquals(16, NumberUtils.toHexString(0L).length());
        Assertions.assertEquals(16, NumberUtils.toHexString(Long.MAX_VALUE).length());
        Assertions.assertEquals(16, NumberUtils.toHexString(Long.MIN_VALUE).length());
    }
}
