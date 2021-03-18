package com.sbss.bithon.agent.core.metric;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/16
 */
public class Max {
    private final AtomicLong value = new AtomicLong(Long.MIN_VALUE);

    public void update(long value) {
        this.value.getAndAccumulate(value, Math::max);
    }

    public long get() {
        return this.value.getAndSet(Long.MIN_VALUE);
    }
}
