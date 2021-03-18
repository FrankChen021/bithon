package com.sbss.bithon.agent.core.metric;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a cumulative value.
 * In contrast to Counter, its value will NOTE be flushed after be accessed
 *
 * @author frank.chen021@outlook.com
 * @date 2021/2/23 9:19 下午
 */
public class Gauge {
    private final AtomicLong value;

    public Gauge() {
        this(0L);
    }

    public Gauge(long initialValue) {
        value = new AtomicLong(initialValue);
    }

    public void incr() {
        value.incrementAndGet();
    }

    public void add(long delta) {
        value.addAndGet(delta);
    }

    public long get() {
        return value.get();
    }
}
