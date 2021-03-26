package com.sbss.bithon.agent.core.metric.model;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/16
 */
public class Min implements ISimpleMetric {
    private final AtomicLong value = new AtomicLong(Long.MAX_VALUE);

    @Override
    public long update(long value) {
        return this.value.accumulateAndGet(value, Math::min);
    }

    public long get() {
        return this.value.getAndSet(Long.MAX_VALUE);
    }
}
