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

package org.bithon.server.common.utils.datetime;

import java.util.concurrent.TimeUnit;

/**
 * @author frankchen
 * @date 2020-03-24 22:31:38
 */
public class DateTimeUtils {

    public static final long MIN_LENGTH_IN_MILLI = 60 * 1000L;
    public static final long HOUR_LENGTH_IN_MILLI = 3600 * 1000L;
    public static final long DAY_LENGTH_IN_MILLI = 24 * 3600 * 1000L;

    public static long dropMilliseconds(long timestamp) {
        return timestamp / 1000 * 1000;
    }

    public static long align2Minute() {
        return align2Minute(1);
    }

    public static long align2Minute(int minutebase) {
        return System.currentTimeMillis() / (minutebase * MIN_LENGTH_IN_MILLI) * (minutebase * MIN_LENGTH_IN_MILLI);
    }

    public static long getDayStart() {
        return getDayStart(System.currentTimeMillis());
    }

    public static long getDayStart(long milliseconds) {
        return milliseconds / DAY_LENGTH_IN_MILLI * DAY_LENGTH_IN_MILLI;
    }

    public static long minute2MilliSec(int minute) {
        return minute * MIN_LENGTH_IN_MILLI;
    }

    public static long offset(long value, TimeUnit timeUnit) {
        switch (timeUnit) {
            case DAYS:
                return value * 3600L * 24 * 1000;

            case HOURS:
                return value * 3600L * 1000;

            case MINUTES:
                return value * 60 * 1000;

            case SECONDS:
                return value * 1000;

            case MILLISECONDS:
                return 0;

            default:
                throw new RuntimeException("not supported");
        }
    }
}
