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

import java.util.concurrent.TimeUnit;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/2/11 11:54
 */
public class HumanReadableDurationTest {

    @Test
    public void testNanoSeconds() {
        HumanReadableDuration duration = HumanReadableDuration.parse("5ns");
        Assertions.assertEquals(5, duration.getDuration().getNano());
        Assertions.assertEquals(TimeUnit.NANOSECONDS, duration.getUnit());
        Assertions.assertEquals("5ns", duration.toString());
    }

    @Test
    public void testMicroSeconds() {
        HumanReadableDuration duration = HumanReadableDuration.parse("5us");
        Assertions.assertEquals(5000, duration.getDuration().getNano());
        Assertions.assertEquals(TimeUnit.MICROSECONDS, duration.getUnit());
        Assertions.assertEquals("5us", duration.toString());
    }

    @Test
    public void testMilliSeconds() {
        HumanReadableDuration duration = HumanReadableDuration.parse("56ms");
        Assertions.assertEquals(56, duration.getDuration().toMillis());
        Assertions.assertEquals(TimeUnit.MILLISECONDS, duration.getUnit());
        Assertions.assertEquals("56ms", duration.toString());
    }

    @Test
    public void testSecond() {
        HumanReadableDuration duration = HumanReadableDuration.parse("5s");
        Assertions.assertEquals(5, duration.getDuration().getSeconds());
        Assertions.assertEquals(TimeUnit.SECONDS, duration.getUnit());
        Assertions.assertEquals("5s", duration.toString());
    }

    @Test
    public void testMinute() {
        HumanReadableDuration duration = HumanReadableDuration.parse("5m");
        Assertions.assertEquals(5, duration.getDuration().toMinutes());
        Assertions.assertEquals(TimeUnit.MINUTES, duration.getUnit());
        Assertions.assertEquals("5m", duration.toString());
    }

    @Test
    public void testHour() {
        HumanReadableDuration duration = HumanReadableDuration.parse("7h");
        Assertions.assertEquals(7, duration.getDuration().toHours());
        Assertions.assertEquals(TimeUnit.HOURS, duration.getUnit());
        Assertions.assertEquals("7h", duration.toString());
    }

    @Test
    public void testDay() {
        HumanReadableDuration duration = HumanReadableDuration.parse("7d");
        Assertions.assertEquals(7, duration.getDuration().toDays());
        Assertions.assertEquals(TimeUnit.DAYS, duration.getUnit());
        Assertions.assertEquals("7d", duration.toString());

        duration = HumanReadableDuration.parse("173d");
        Assertions.assertEquals(173, duration.getDuration().toDays());
        Assertions.assertEquals(TimeUnit.DAYS, duration.getUnit());
        Assertions.assertEquals("173d", duration.toString());
    }

    @Test
    public void testInvalidFormat() {
        Assertions.assertThrows(RuntimeException.class, () -> HumanReadableDuration.parse("6"));
        Assertions.assertThrows(RuntimeException.class, () -> HumanReadableDuration.parse("7a"));
        Assertions.assertThrows(RuntimeException.class, () -> HumanReadableDuration.parse("dd"));
        Assertions.assertThrows(RuntimeException.class, () -> HumanReadableDuration.parse("ns"));
        Assertions.assertThrows(RuntimeException.class, () -> HumanReadableDuration.parse("ms"));
        Assertions.assertThrows(RuntimeException.class, () -> HumanReadableDuration.parse("us"));
        Assertions.assertThrows(RuntimeException.class, () -> HumanReadableDuration.parse(" us "));
        Assertions.assertThrows(RuntimeException.class, () -> HumanReadableDuration.parse("-us"));
        Assertions.assertThrows(RuntimeException.class, () -> HumanReadableDuration.parse("-s"));
    }

    @Test
    public void testNegative() {
        HumanReadableDuration duration = HumanReadableDuration.parse("-6s");
        Assertions.assertEquals(-6, duration.getDuration().getSeconds());
    }
}
