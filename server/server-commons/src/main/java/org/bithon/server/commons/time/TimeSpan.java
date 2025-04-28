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

import org.bithon.component.commons.utils.HumanReadableDuration;

import java.sql.Timestamp;
import java.text.ParseException;
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

    public long getSeconds() {
        return milliseconds / 1000;
    }

    private final long milliseconds;

    public static TimeSpan now() {
        return new TimeSpan(System.currentTimeMillis());
    }

    /**
     * @param time e.g. 2020-08-24T14:55:46.000+08:00
     */
    public static TimeSpan fromISO8601(String time) {
        return new TimeSpan(DateTimes.ISO_DATE_TIME.parse(time).getMillis());
    }

    public static TimeSpan of(long l) {
        return new TimeSpan(l);
    }

    public TimeSpan before(long value, TimeUnit timeUnit) {
        return new TimeSpan(milliseconds - timeUnit.toMillis(value));
    }

    public TimeSpan before(HumanReadableDuration duration) {
        return before(duration.getDuration());
    }

    public TimeSpan before(Duration duration) {
        return new TimeSpan(milliseconds - duration.toMillis());
    }

    public TimeSpan after(long value, TimeUnit timeUnit) {
        return new TimeSpan(milliseconds + timeUnit.toMillis(value));
    }

    public TimeSpan after(Duration duration) {
        return new TimeSpan(milliseconds + duration.toMillis());
    }

    public Timestamp toTimestamp() {
        return new Timestamp(milliseconds);
    }

    public String format(String format) {
        return format(format, null);
    }

    public String format(String format, TimeZone tz) {
        SimpleDateFormat df = new SimpleDateFormat(format, Locale.ENGLISH);
        if (tz != null) {
            df.setTimeZone(tz);
        }
        return df.format(new Date(milliseconds));
    }

    public String toISO8601() {
        return format("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    }

    public String toISO8601(TimeZone tz) {
        return format("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", tz);
    }

    public static TimeSpan fromString(String dateTime, String format, TimeZone timeZone) throws ParseException {
        SimpleDateFormat df = new SimpleDateFormat(format, Locale.ENGLISH);
        df.setTimeZone(timeZone);
        return new TimeSpan(df.parse(dateTime).getTime());
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

    public TimeSpan offset(TimeZone zone) {
        return new TimeSpan(milliseconds - zone.getOffset(milliseconds));
    }

    public TimeSpan floor(Duration duration) {
        long milliSeconds = duration.getSeconds() * 1000;
        return new TimeSpan(this.milliseconds / milliSeconds * milliSeconds);
    }

    public TimeSpan ceil(Duration duration) {
        long milliSeconds = duration.getSeconds() * 1000;

        return new TimeSpan((this.milliseconds + milliSeconds) / milliSeconds * milliSeconds);
    }

    public TimeSpan minus(long millis) {
        return new TimeSpan(this.milliseconds - millis);
    }
}
