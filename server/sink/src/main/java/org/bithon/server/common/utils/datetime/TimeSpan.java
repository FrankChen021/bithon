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

import lombok.AllArgsConstructor;
import lombok.Data;
import org.bithon.server.metric.parser.DateTimes;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * @author frankchen
 * @date 2020-08-24 14:55:46
 */
@Data
@AllArgsConstructor
public class TimeSpan {

    private long milliseconds;

    public static TimeSpan nowInMinute() {
        return new TimeSpan(DateTimeUtils.align2Minute());
    }

    public static TimeSpan fromISO8601(String time) {
        return new TimeSpan(DateTimes.ISO_DATE_TIME.parse(time).getMillis());
    }

    public TimeSpan before(long value, TimeUnit timeUnit) {
        return new TimeSpan(milliseconds - DateTimeUtils.offset(value, timeUnit));
    }

    public TimeSpan after(long value, TimeUnit timeUnit) {
        return new TimeSpan(milliseconds + DateTimeUtils.offset(value, timeUnit));
    }

    public Timestamp toTimestamp() {
        return new Timestamp(milliseconds);
    }

    public Date toDate() {
        return new Date(milliseconds);
    }

    public String toString(String format) {
        return new SimpleDateFormat(format, Locale.ENGLISH).format(new Date(milliseconds));
    }

    public String toISO8601() {
        return DateTimeUtils.toISO8601(this.milliseconds);
    }

    public long diff(TimeSpan timeSpan) {
        return this.getMilliseconds() - timeSpan.getMilliseconds();
    }

    public long toSeconds() {
        return this.milliseconds / 1000;
    }

    @Override
    public boolean equals(Object right) {
        if (right instanceof TimeSpan) {
            return ((TimeSpan) right).milliseconds == this.milliseconds;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return (int) milliseconds;
    }

    public TimeSpan floor(Duration duration) {
        long milliSeconds = duration.getSeconds() * 1000;
        return new TimeSpan(this.milliseconds / milliSeconds * milliSeconds);
    }

    public TimeSpan ceil(Duration duration) {
        long milliSeconds = duration.getSeconds() * 1000;

        boolean hasModule = this.milliseconds % milliSeconds > 0;

        return new TimeSpan((this.milliseconds / milliSeconds + (hasModule ? 1 : 0)) * milliSeconds);
    }
}
