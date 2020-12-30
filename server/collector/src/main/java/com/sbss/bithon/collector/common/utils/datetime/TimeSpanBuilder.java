package com.sbss.bithon.collector.common.utils.datetime;

import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * @author frankchen
 * @Date 2020-08-26 16:32:14
 */

public class TimeSpanBuilder {

    private long milliseconds;

    public TimeSpanBuilder(long value) {
        this.milliseconds = value;
    }

    public TimeSpanBuilder before(long value, TimeUnit timeUnit) {
        milliseconds -= DateTimeUtils.offset(value, timeUnit);
        return this;
    }

    public TimeSpanBuilder after(long value, TimeUnit timeUnit) {
        milliseconds += DateTimeUtils.offset(value, timeUnit);
        return this;
    }

    public long getMilliseconds() {
        return milliseconds;
    }

    public TimeSpanBuilder offset(TimeZone zone) {
        milliseconds -= zone.getOffset(milliseconds);
        return this;
    }

    public TimeSpan build() {
        return new TimeSpan(milliseconds);
    }
}
