package com.sbss.bithon.agent.core.tracing.context;

import java.lang.reflect.Executable;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/4/7 8:44 下午
 */
public class TraceSpanBuilder {

    static class NoopSpan extends TraceSpan {
        public NoopSpan(String spanId,
                        String parentSpanId,
                        TraceContext traceContext) {
            super(spanId, parentSpanId, traceContext);
        }

        static NoopSpan INSTANCE = new NoopSpan(null, null, null);

        @Override
        public TraceSpan start() {
            return this;
        }

        @Override
        public void finish() {
        }

        @Override
        public TraceSpan tag(String name, String value) {
            return this;
        }

        @Override
        public TraceSpan tag(Throwable exception) {
            return this;
        }

        @Override
        public TraceSpan clazz(Class<?> clazz) {
            return this;
        }

        @Override
        public TraceSpan method(Executable method) {
            return this;
        }
    }

    public static TraceSpan build(String name) {
        TraceContext traceContext = TraceContextHolder.get();
        if (traceContext == null) {
            return NoopSpan.INSTANCE;
        }

        TraceSpan parentSpan = traceContext.currentSpan();
        if (parentSpan == null) {
            return NoopSpan.INSTANCE;
        }

        // create a span and save it in user-context
        return parentSpan.newChildSpan(name);
    }
}
