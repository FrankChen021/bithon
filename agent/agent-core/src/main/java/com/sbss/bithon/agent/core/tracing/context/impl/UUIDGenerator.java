package com.sbss.bithon.agent.core.tracing.context.impl;

import com.sbss.bithon.agent.core.tracing.context.ITraceIdGenerator;

import java.util.UUID;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/6 12:20 上午
 */
public class UUIDGenerator implements ITraceIdGenerator {
    @Override
    public String newTraceId() {
        return UUID.randomUUID().toString();
    }
}
