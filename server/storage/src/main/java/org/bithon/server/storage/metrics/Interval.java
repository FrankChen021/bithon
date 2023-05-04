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

package org.bithon.server.storage.metrics;

import lombok.Getter;
import org.bithon.server.commons.time.TimeSpan;

/**
 * @author frank.chen021@outlook.com
 * @date 1/11/21 3:03 pm
 */

public class Interval {
    private final TimeSpan startTime;
    private final TimeSpan endTime;

    /**
     * in second
     */
    @Getter
    private final Integer step;

    private Interval(TimeSpan startTime, TimeSpan endTime, Integer step) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.step = step;
    }

    public static Interval of(TimeSpan start, TimeSpan end) {
        return of(start, end, null);
    }

    public static Interval of(TimeSpan start, TimeSpan end, Integer step) {
        return new Interval(start, end, step);
    }

    public TimeSpan getStartTime() {
        return startTime;
    }

    public TimeSpan getEndTime() {
        return endTime;
    }

    /**
     * @return the length of interval in seconds
     */
    public int getTotalLength() {
        return (int) (endTime.getMilliseconds() - startTime.getMilliseconds()) / 1000;
    }
}
