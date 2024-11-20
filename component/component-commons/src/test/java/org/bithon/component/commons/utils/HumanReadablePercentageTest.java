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
 * @author Frank Chen
 * @date 22/1/24 10:21 am
 */
public class HumanReadablePercentageTest {

    @Test
    public void testNull() {
        Assert.assertThrows(Preconditions.InvalidValueException.class, () -> HumanReadablePercentage.of(null));
    }

    @Test
    public void testEmpty() {
        Assert.assertThrows(Preconditions.InvalidValueException.class, () -> HumanReadablePercentage.of(""));
        Assert.assertThrows(Preconditions.InvalidValueException.class, () -> HumanReadablePercentage.of(" "));
        Assert.assertThrows(Preconditions.InvalidValueException.class, () -> HumanReadablePercentage.of("\n"));
    }

    @Test
    public void testNoDigitLeadCharacters() {
        Assert.assertThrows(Preconditions.InvalidValueException.class, () -> HumanReadablePercentage.of(" a"));
        Assert.assertThrows(Preconditions.InvalidValueException.class, () -> HumanReadablePercentage.of("\n."));
    }

    @Test
    public void testInvalidUnit() {
        Assert.assertThrows(Preconditions.InvalidValueException.class, () -> HumanReadablePercentage.of("5M "));
        Assert.assertThrows(Preconditions.InvalidValueException.class, () -> HumanReadablePercentage.of("6a\t"));
        Assert.assertThrows(Preconditions.InvalidValueException.class, () -> HumanReadablePercentage.of("7z\t"));
    }

    @Test
    public void testLengthNotMet() {
        Assert.assertThrows(Preconditions.InvalidValueException.class, () -> HumanReadablePercentage.of("5"));
    }

    @Test
    public void testMalformedNumber() {
        Assert.assertThrows(Preconditions.InvalidValueException.class, () -> HumanReadablePercentage.of("6a%"));
    }

    @Test
    public void test() {
        Assert.assertEquals(0, HumanReadablePercentage.of("0%").getFraction(), 0.00000000001);
        Assert.assertEquals(0.05, HumanReadablePercentage.of("5 %").getFraction(), 0.00000000001);
        Assert.assertEquals(0.5, HumanReadablePercentage.of("50%").getFraction(), 0.00000000001);
        Assert.assertEquals(5, HumanReadablePercentage.of("500%").getFraction(), 0.00000000001);
        Assert.assertEquals(50, HumanReadablePercentage.of("5000%").getFraction(), 0.00000000001);
    }

    @Test
    public void testNegative() {
        Assert.assertEquals(0, HumanReadablePercentage.of("-0 %").getFraction(), 0.00000000001);
        Assert.assertEquals(-0.05, HumanReadablePercentage.of("-5 %").getFraction(), 0.00000000001);
    }
}
