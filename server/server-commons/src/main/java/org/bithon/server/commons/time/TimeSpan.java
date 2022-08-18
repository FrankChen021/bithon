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

package org.bithon.server.commons.time;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * @author frankchen
 * @date 2020-08-24 14:55:46
 */
public class TimeSpan {
    public TimeSpan(long milliseconds) {
        this.milliseconds = milliseconds;
    }

    public long getMilliseconds() {
        return milliseconds;
    }

    private final long milliseconds;

    public static TimeSpan nowInMinute() {
        return new TimeSpan(DateTimeUtils.align2Minute());
    }

    public static TimeSpan fromISO8601(String time) {
        return new TimeSpan(DateTimes.ISO_DATE_TIME.parse(time).getMillis());
    }

    public static TimeSpan of(long l) {
        return new TimeSpan(l);
    }

    public TimeSpan before(long value, TimeUnit timeUnit) {
        return new TimeSpan(milliseconds - timeUnit.toMillis(value));
    }

    public TimeSpan after(long value, TimeUnit timeUnit) {
        return new TimeSpan(milliseconds + timeUnit.toMillis(value));
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
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.ENGLISH).format(new Date(this.milliseconds));
    }

    /**
     * difference in millis
     */
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

    public static final long DAY_LENGTH_IN_MILLI = 24 * 3600 * 1000L;

    public TimeSpan truncate2Day() {
        return new TimeSpan(milliseconds / DAY_LENGTH_IN_MILLI * DAY_LENGTH_IN_MILLI);
    }

    public TimeSpan offset(TimeZone zone) {
        return new TimeSpan(milliseconds - zone.getOffset(milliseconds));
    }

    /*
    public void plus(int value, TimeUnit unit) {
        this.milliseconds += unit.toMillis(value);
    }*/

    public TimeSpan floor(Duration duration) {
        long milliSeconds = duration.getSeconds() * 1000;
        return new TimeSpan(this.milliseconds / milliSeconds * milliSeconds);
    }

    public TimeSpan ceil(Duration duration) {
        long milliSeconds = duration.getSeconds() * 1000;

        boolean hasModule = this.milliseconds % milliSeconds > 0;

        return new TimeSpan((this.milliseconds / milliSeconds + (hasModule ? 1 : 0)) * milliSeconds);
    }

    public TimeSpan minus(long millis) {
        return new TimeSpan(this.milliseconds - millis);
    }
}
