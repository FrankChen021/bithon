package com.sbss.bithon.agent.core.tracing.context;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/5 8:51 下午
 */
public enum SpanKind {
    CLIENT,
    SERVER,
    PRODUCER,
    CONSUMER;

    private SpanKind() {
    }
}
