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

package org.bithon.server.datasource.query;

import lombok.Getter;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.utils.HumanReadableDuration;
import org.bithon.server.commons.time.TimeSpan;

import java.time.Duration;

/**
 * @author frank.chen021@outlook.com
 * @date 1/11/21 3:03 pm
 */
@Getter
public class Interval {
    private final TimeSpan startTime;
    private final TimeSpan endTime;

    /**
     * Can be null. If null, the step is the same as the window
     */
    private final HumanReadableDuration window;

    /**
     * In second
     * Can be null. For example, for the SELECT query, the step is not needed
     */
    private final Duration step;

    private final IExpression timestampColumn;

    private Interval(TimeSpan startTime, TimeSpan endTime, Duration step, HumanReadableDuration window, IExpression timestampColumn) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.step = step;
        this.window = window;
        this.timestampColumn = timestampColumn;
    }

    public static Interval of(String startISO8601, String endISO8601) {
        return of(TimeSpan.fromISO8601(startISO8601), TimeSpan.fromISO8601(endISO8601));
    }

    public static Interval of(TimeSpan start, TimeSpan end) {
        return of(start, end, null, new IdentifierExpression("timestamp"));
    }

    public static Interval of(TimeSpan start, TimeSpan end, Duration step, IExpression timestampColumn) {
        // Use step as window
        return new Interval(start, end, step, null, timestampColumn);
    }

    public static Interval of(TimeSpan start, TimeSpan end, Duration step, HumanReadableDuration window, IExpression timestampColumn) {
        return new Interval(start, end, step, window, timestampColumn);
    }

    /**
     * @return the length of interval in seconds
     */
    public int getTotalSeconds() {
        return (int) (endTime.getMilliseconds() - startTime.getMilliseconds()) / 1000;
    }

    public String toString() {
        return "Interval{" +
               "startTime=" + startTime.toISO8601() +
               ", endTime=" + endTime.toISO8601() +
               ", window=" + window +
               ", step=" + step +
               ", timestampColumn=" + timestampColumn +
               '}';
    }
}
