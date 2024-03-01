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

import java.util.concurrent.TimeUnit;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/2/11 11:54
 */
public class HumanReadableDurationTest {
    @Test
    public void testSecond() {
        HumanReadableDuration duration = HumanReadableDuration.parse("5s");
        Assert.assertEquals(5, duration.getDuration().getSeconds());
        Assert.assertEquals(TimeUnit.SECONDS, duration.getUnit());
        Assert.assertEquals("5s", duration.toString());
    }

    @Test
    public void testMinute() {
        HumanReadableDuration duration = HumanReadableDuration.parse("5m");
        Assert.assertEquals(5, duration.getDuration().toMinutes());
        Assert.assertEquals(TimeUnit.MINUTES, duration.getUnit());
        Assert.assertEquals("5m", duration.toString());
    }

    @Test
    public void testHour() {
        HumanReadableDuration duration = HumanReadableDuration.parse("7h");
        Assert.assertEquals(7, duration.getDuration().toHours());
        Assert.assertEquals(TimeUnit.HOURS, duration.getUnit());
        Assert.assertEquals("7h", duration.toString());
    }

    @Test
    public void testDay() {
        HumanReadableDuration duration = HumanReadableDuration.parse("7d");
        Assert.assertEquals(7, duration.getDuration().toDays());
        Assert.assertEquals(TimeUnit.DAYS, duration.getUnit());
        Assert.assertEquals("7d", duration.toString());

        duration = HumanReadableDuration.parse("173d");
        Assert.assertEquals(173, duration.getDuration().toDays());
        Assert.assertEquals(TimeUnit.DAYS, duration.getUnit());
        Assert.assertEquals("173d", duration.toString());
    }

    @Test
    public void testInvalidFormat() {
        Assert.assertThrows(RuntimeException.class, () -> HumanReadableDuration.parse("6"));
        Assert.assertThrows(RuntimeException.class, () -> HumanReadableDuration.parse("7a"));
        Assert.assertThrows(RuntimeException.class, () -> HumanReadableDuration.parse("dd"));
    }


    @Test
    public void testNegative() {
        HumanReadableDuration duration = HumanReadableDuration.parse("-6s");
        Assert.assertEquals(-6, duration.getDuration().getSeconds());
    }
}
