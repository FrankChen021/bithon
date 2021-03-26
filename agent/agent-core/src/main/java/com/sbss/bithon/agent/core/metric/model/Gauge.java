package com.sbss.bithon.agent.core.metric.model;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a cumulative value.
 * In contrast to Counter, its value will NOTE be flushed after be accessed
 *
 * @author frank.chen021@outlook.com
 * @date 2021/2/23 9:19 下午
 */
public class Gauge implements ISimpleMetric {
    private final AtomicLong value;

    public Gauge() {
        this(0L);
    }

    public Gauge(long initialValue) {
        value = new AtomicLong(initialValue);
    }

    @Override
    public long update(long delta) {
        return value.addAndGet(delta);
    }

    public long get() {
        return value.get();
    }
}
