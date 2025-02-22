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
 * @date 2024/3/3 18:20
 */
public class HumanReadableNumberTest {
    @Test
    public void testBinaryFormat() {
        Assertions.assertEquals(5L * 1024, HumanReadableNumber.parse("5KiB"));
        Assertions.assertEquals(5L * 1024 * 1024, HumanReadableNumber.parse("5MiB"));
        Assertions.assertEquals(5L * 1024 * 1024 * 1024, HumanReadableNumber.parse("5GiB"));
        Assertions.assertEquals(5L * 1024 * 1024 * 1024 * 1024, HumanReadableNumber.parse("5TiB"));
        Assertions.assertEquals(5L * 1024 * 1024 * 1024 * 1024 * 1024, HumanReadableNumber.parse("5PiB"));
    }

    @Test
    public void testSimplifiedBinaryFormat() {
        Assertions.assertEquals(5L * 1024, HumanReadableNumber.parse("5Ki"));
        Assertions.assertEquals(5L * 1024 * 1024, HumanReadableNumber.parse("5Mi"));
        Assertions.assertEquals(5L * 1024 * 1024 * 1024, HumanReadableNumber.parse("5Gi"));
        Assertions.assertEquals(5L * 1024 * 1024 * 1024 * 1024, HumanReadableNumber.parse("5Ti"));
        Assertions.assertEquals(5L * 1024 * 1024 * 1024 * 1024 * 1024, HumanReadableNumber.parse("5Pi"));
    }

    @Test
    public void testDecimalFormat() {
        Assertions.assertEquals(5L * 1000, HumanReadableNumber.parse("5K"));
        Assertions.assertEquals(5L * 1000 * 1000, HumanReadableNumber.parse("5M"));
        Assertions.assertEquals(5L * 1000 * 1000 * 1000, HumanReadableNumber.parse("5G"));
        Assertions.assertEquals(5L * 1000 * 1000 * 1000 * 1000, HumanReadableNumber.parse("5T"));
        Assertions.assertEquals(5L * 1000 * 1000 * 1000 * 1000 * 1000, HumanReadableNumber.parse("5P"));
    }

    @Test
    public void testEqual() {
        Assertions.assertEquals(HumanReadableNumber.of("5K"), HumanReadableNumber.parse("5K"));
        Assertions.assertEquals(HumanReadableNumber.of("5K"), HumanReadableNumber.of("5K"));
        Assertions.assertEquals(HumanReadableNumber.of("5Ki"), HumanReadableNumber.of("5KiB"));
    }
}
