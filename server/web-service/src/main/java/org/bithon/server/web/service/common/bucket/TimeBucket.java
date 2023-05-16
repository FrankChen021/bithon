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

package org.bithon.server.web.service.common.bucket;

import lombok.Data;
import org.bithon.server.commons.time.TimeSpan;

/**
 * @author Frank Chen
 * @date 4/5/23 5:21 pm
 */
@Data
public class TimeBucket {
    /**
     * number of buckets
     */
    final int count;

    /**
     * length of bucket in second
     */
    final int length;

    public TimeBucket(int count, int length) {
        this.count = count;
        this.length = length;
    }

    public int getCount() {
        return count;
    }

    public int getLength() {
        return length;
    }

    /**
     * Requirement
     * 1. The minimal length of each bucket is 1 minute
     *
     * <p>
     * @param bucketCount the count of buckets
     * @param startTimestamp in millisecond
     * @param endTimestamp   in millisecond
     * @return the length of a bucket in second
     */
    public static TimeBucket calculate(long startTimestamp, long endTimestamp, int bucketCount) {
        int seconds = (int) ((endTimestamp - startTimestamp) / 1000);
        if (seconds <= 60) {
            return new TimeBucket(1, 60);
        }

        int minute = (int) ((endTimestamp - startTimestamp) / 1000 / 60);
        int hour = (int) Math.ceil(minute / 60.0);

        int[] steps = {1, 5, 10, 15, 30, 60, 150, 180, 360, 720, 1440};
        int stepIndex = 0;
        int step = 1;
        while (minute / step > bucketCount) {
            stepIndex++;
            if (stepIndex < steps.length) {
                step = steps[stepIndex];
            } else {
                // if exceeding the predefined steps, increase 1 day at least
                step += 1440;
            }
        }

        int m = hour % step;
        int hourPerStep = (hour / step + (m > 0 ? 1 : 0));

        // the last 60 is the max bucket number
        int length = hourPerStep * step * 3600 / 60;
        int mod = seconds % length;
        return new TimeBucket(seconds / length + (mod > 0 ? 1 : 0), length);
    }

    /**
     * The minimal length of each bucket is 10s
     */
    public static int calculate(TimeSpan start, TimeSpan end) {
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
}
