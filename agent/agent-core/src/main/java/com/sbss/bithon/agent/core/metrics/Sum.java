package com.sbss.bithon.agent.core.metrics;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a cumulative value and its value will be flushed after be accessed
 *
 * @author frank.chen021@outlook.com
 * @date 2021/2/23 9:18 下午
 */
public class Sum {
    private final AtomicLong value;

    public Sum() {
        this(0L);
    }

    public Sum(long initialValue) {
        value = new AtomicLong(initialValue);
    }

    public void incr() {
        value.incrementAndGet();
    }

    public void update(long delta) {
        value.addAndGet(delta);
    }

    public long get() {
        return value.getAndSet(0);
    }

    public long peek() {
        return value.get();
    }
}
