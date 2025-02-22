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
 * human-readable duration format like: 1s, 2m, 3h, 4d
 * <p>
 * The supported units are: s, m, h, d(case-sensitive), representing second, minute, hour, day respectively.
 *
 * @author frank.chen021@outlook.com
 * @date 2024/2/11 11:38
 */
public class HumanReadableDuration extends Number {
    public static final HumanReadableDuration DURATION_1_MINUTE = HumanReadableDuration.of(1, TimeUnit.MINUTES);
    public static final HumanReadableDuration DURATION_30_MINUTE = HumanReadableDuration.of(30, TimeUnit.MINUTES);

    private final String text;
    private final Duration duration;
    private final TimeUnit unit;

    private HumanReadableDuration(String durationText,
                                  Duration duration,
                                  TimeUnit unit) {
        this.text = durationText;
        this.duration = duration;
        this.unit = unit;
    }

    public boolean isNegative() {
        return duration.isNegative();
    }

    public boolean isZero() {
        return duration.isZero();
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
        if (timeUnit == TimeUnit.NANOSECONDS) {
            return new HumanReadableDuration(duration + "ns", Duration.ofNanos(duration), timeUnit);
        }
        if (timeUnit == TimeUnit.MICROSECONDS) {
            return new HumanReadableDuration(duration + "us", Duration.ofNanos(timeUnit.toNanos(duration * 1000L)), timeUnit);
        }
        if (timeUnit == TimeUnit.MILLISECONDS) {
            return new HumanReadableDuration(duration + "ms", Duration.ofMillis(duration), timeUnit);
        }
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
        Preconditions.checkIfTrue(!durationText.isEmpty(), "duration can't be empty");
        Preconditions.checkIfTrue(durationText.length() >= 2, "[%s] is not a valid format of duration", durationText);

        int lastIndex = durationText.length() - 1;
        TimeUnit timeUnit;
        char unit = durationText.charAt(lastIndex);
        switch (unit) {
            case 's':
                if (durationText.length() > 2) {
                    switch (durationText.charAt(lastIndex - 1)) {
                        case 'n':
                            lastIndex--;
                            timeUnit = TimeUnit.NANOSECONDS;
                            break;
                        case 'u':
                            lastIndex--;
                            timeUnit = TimeUnit.MICROSECONDS;
                            break;
                        case 'm':
                            lastIndex--;
                            timeUnit = TimeUnit.MILLISECONDS;
                            break;
                        default:
                            timeUnit = TimeUnit.SECONDS;
                            break;
                    }
                    break;
                } else {
                    timeUnit = TimeUnit.SECONDS;
                }

                break;

            case 'm':
                timeUnit = TimeUnit.MINUTES;
                break;

            case 'h':
                timeUnit = TimeUnit.HOURS;
                break;

            case 'd':
                timeUnit = TimeUnit.DAYS;
                break;

            default:
                throw new UnsupportedOperationException(unit + " is not supported");
        }

        boolean negative = durationText.charAt(0) == '-';
        int start = negative ? 1 : 0;

        Preconditions.checkIfTrue(start < lastIndex, "Invalid duration format: " + durationText);

        int val = 0;
        for (int i = start; i < lastIndex; i++) {
            char chr = durationText.charAt(i);
            if (!Character.isDigit(chr)) {
                throw new RuntimeException(StringUtils.format("Invalid character [%c] found in the duration formatted text: ", chr, durationText));
            }

            int v = val * 10 + chr - '0';
            if (v < val) {
                throw new RuntimeException("The number is out of range of Integer");
            }
            val = v;
        }
        if (negative) {
            val = -val;
        }

        return new HumanReadableDuration(durationText,
                                         Duration.ofNanos(timeUnit.toNanos(val)),
                                         timeUnit);
    }

    @Override
    public int intValue() {
        return (int) duration.getSeconds();
    }

    @Override
    public long longValue() {
        return duration.getSeconds();
    }

    @Override
    public float floatValue() {
        return duration.getSeconds();
    }

    @Override
    public double doubleValue() {
        return duration.getSeconds();
    }
}
