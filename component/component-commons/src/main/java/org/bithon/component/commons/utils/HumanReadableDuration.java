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

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/2/11 11:38
 */
public class HumanReadableDuration {
    private final String text;
    private final Duration duration;
    private final TimeUnit unit;

    public HumanReadableDuration(String durationText,
                                 Duration duration,
                                 TimeUnit unit) {
        this.text = durationText;
        this.duration = duration;
        this.unit = unit;
    }

    public Duration getDuration() {
        return duration;
    }

    public TimeUnit getUnit() {
        return unit;
    }

    @Override
    public String toString() {
        return text;
    }

    public static HumanReadableDuration of(int duration, TimeUnit timeUnit) {
        if (timeUnit == TimeUnit.SECONDS) {
            return new HumanReadableDuration(duration + "s", Duration.ofSeconds(duration), timeUnit);
        }
        if (timeUnit == TimeUnit.MINUTES) {
            return new HumanReadableDuration(duration + "m", Duration.ofMinutes(duration), timeUnit);
        }
        if (timeUnit == TimeUnit.HOURS) {
            return new HumanReadableDuration(duration + "h", Duration.ofHours(duration), timeUnit);
        }
        if (timeUnit == TimeUnit.DAYS) {
            return new HumanReadableDuration(duration + "d", Duration.ofDays(duration), timeUnit);
        }
        throw new RuntimeException("Invalid timeunit");
    }

    public static HumanReadableDuration parse(String durationText) {
        Preconditions.checkNotNull(durationText, "durationText can not be null.");
        durationText = durationText.trim();
        Preconditions.checkIfTrue(durationText.length() >= 2, "[%s] is not a valid format of duration", durationText);

        TimeUnit timeUnit;
        char unit = durationText.charAt(durationText.length() - 1);
        switch (unit) {
            case 's':
            case 'S':
                timeUnit = TimeUnit.SECONDS;
                break;

            case 'm':
            case 'M':
                timeUnit = TimeUnit.MINUTES;
                break;

            case 'h':
            case 'H':
                timeUnit = TimeUnit.HOURS;
                break;

            case 'd':
            case 'D':
                timeUnit = TimeUnit.DAYS;
                break;

            default:
                throw new UnsupportedOperationException(unit + " is not supported");
        }

        int val = 0;
        for (int i = 0, len = durationText.length() - 1; i < len; i++) {
            char chr = durationText.charAt(i);
            if (!Character.isDigit(chr)) {
                throw new RuntimeException("Invalid duration: " + durationText);
            }

            int v = val + chr - '0';
            if (v < val) {
                throw new RuntimeException("The number is out of range of Integer");
            }
            val = v;
        }

        return new HumanReadableDuration(durationText,
                                         Duration.ofSeconds(timeUnit.toSeconds(val)),
                                         timeUnit);
    }
}
