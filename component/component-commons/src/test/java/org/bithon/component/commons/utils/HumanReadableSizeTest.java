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

import org.junit.Assert;
import org.junit.Test;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/3/3 18:20
 */
public class HumanReadableSizeTest {
    @Test
    public void testBinaryFormat() {
        Assert.assertEquals(5L * 1024, HumanReadableSize.parse("5KiB"));
        Assert.assertEquals(5L * 1024 * 1024, HumanReadableSize.parse("5MiB"));
        Assert.assertEquals(5L * 1024 * 1024 * 1024, HumanReadableSize.parse("5GiB"));
        Assert.assertEquals(5L * 1024 * 1024 * 1024 * 1024, HumanReadableSize.parse("5TiB"));
        Assert.assertEquals(5L * 1024 * 1024 * 1024 * 1024 * 1024, HumanReadableSize.parse("5PiB"));
    }

    @Test
    public void testSimplifiedBinaryFormat() {
        Assert.assertEquals(5L * 1024, HumanReadableSize.parse("5Ki"));
        Assert.assertEquals(5L * 1024 * 1024, HumanReadableSize.parse("5Mi"));
        Assert.assertEquals(5L * 1024 * 1024 * 1024, HumanReadableSize.parse("5Gi"));
        Assert.assertEquals(5L * 1024 * 1024 * 1024 * 1024, HumanReadableSize.parse("5Ti"));
        Assert.assertEquals(5L * 1024 * 1024 * 1024 * 1024 * 1024, HumanReadableSize.parse("5Pi"));
    }

    @Test
    public void testDecimalFormat() {
        Assert.assertEquals(5L * 1000, HumanReadableSize.parse("5K"));
        Assert.assertEquals(5L * 1000 * 1000, HumanReadableSize.parse("5M"));
        Assert.assertEquals(5L * 1000 * 1000 * 1000, HumanReadableSize.parse("5G"));
        Assert.assertEquals(5L * 1000 * 1000 * 1000 * 1000, HumanReadableSize.parse("5T"));
        Assert.assertEquals(5L * 1000 * 1000 * 1000 * 1000 * 1000, HumanReadableSize.parse("5P"));
    }

    @Test
    public void testEqual() {
        Assert.assertEquals(HumanReadableSize.of("5K"), HumanReadableSize.parse("5K"));
        Assert.assertEquals(HumanReadableSize.of("5K"), HumanReadableSize.of("5K"));
        Assert.assertEquals(HumanReadableSize.of("5Ki"), HumanReadableSize.of("5KiB"));
    }
}
