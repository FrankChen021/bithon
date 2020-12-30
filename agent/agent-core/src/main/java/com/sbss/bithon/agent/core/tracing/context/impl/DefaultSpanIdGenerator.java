package com.sbss.bithon.agent.core.tracing.context.impl;

import com.sbss.bithon.agent.core.tracing.context.ISpanIdGenerator;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/7 10:37 下午
 */
public class DefaultSpanIdGenerator implements ISpanIdGenerator {
    private final long base;
    private final AtomicInteger counter;

    public DefaultSpanIdGenerator() {
        base = (System.currentTimeMillis() / 1000) << 20;
        counter = new AtomicInteger(1);
    }

    @Override
    public String newSpanId() {
        return String.valueOf(base | counter.getAndIncrement());
    }
}
