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

package org.bithon.server.metric.storage;

import org.bithon.server.common.utils.datetime.TimeSpan;

/**
 * @author Frank Chen
 * @date 1/11/21 3:03 pm
 */

public class Interval {
    private final TimeSpan startTime;
    private final TimeSpan endTime;

    /**
     * in second
     */
    private final int granularity;

    private Interval(TimeSpan startTime, TimeSpan endTime, int step) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.granularity = step;
    }

    public static Interval of(TimeSpan start, TimeSpan end) {
        return of(start, end, getGranularity(start, end));
    }

    public static Interval of(TimeSpan start, TimeSpan end, int step) {
        return new Interval(start, end, step);
    }

    /**
     * TODO: interval should be consistent with retention rules
     */
    private static int getGranularity(TimeSpan start, TimeSpan end) {
        long length = end.diff(start) / 1000;
        if (length >= 7 * 24 * 3600) {
            return 15 * 60;
        }
        if (length >= 3 * 24 * 3600) {
            return 10 * 60;
        }
        if (length >= 24 * 3600) {
            return 5 * 60;
        }
        if (length >= 12 * 3600) {
            return 60;
        }
        if (length >= 6 * 3600) {
            return 30;
        }
        return 10;
    }

    public TimeSpan getStartTime() {
        return startTime;
    }

    public TimeSpan getEndTime() {
        return endTime;
    }

    /**
     * @return query granularity in seconds
     */
    public int getGranularity() {
        return granularity;
    }
}
