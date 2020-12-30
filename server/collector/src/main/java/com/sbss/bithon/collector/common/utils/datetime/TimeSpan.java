package com.sbss.bithon.collector.common.utils.datetime;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.joda.time.DateTime;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
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
        return new TimeSpan(DateTime.parse(time).getMillis());
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
        return new SimpleDateFormat(format).format(new Date(milliseconds));
    }

    public String toISO8601() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date(milliseconds));
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
}
