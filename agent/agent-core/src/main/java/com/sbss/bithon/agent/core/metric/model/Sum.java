package com.sbss.bithon.agent.core.metric.model;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a cumulative value and its value will be flushed after be accessed
 *
 * @author frank.chen021@outlook.com
 * @date 2021/2/23 9:18 下午
 */
public class Sum implements ISimpleMetric {
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

    @Override
    public long update(long delta) {
        if (delta != 0) {
            return value.addAndGet(delta);
        }
        return value.get();
    }

    public long get() {
        return value.getAndSet(0);
    }

    public long peek() {
        return value.get();
    }
}
