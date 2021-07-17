package com.sbss.bithon.agent.core.tracing.context;

import java.lang.reflect.Executable;
import java.util.Collections;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/7/17 16:34
 */
class NoopTraceSpan implements ITraceSpan {

    static NoopTraceSpan INSTANCE = new NoopTraceSpan(null, null, null);

    private final NoopTraceContext traceContext;
    private final String spanId;
    private final String parentSpanId;

    public NoopTraceSpan(NoopTraceContext traceContext, String spanId, String parentSpanId) {
        this.traceContext = traceContext;
        this.spanId = spanId;
        this.parentSpanId = parentSpanId;
    }

    @Override
    public ITraceSpan start() {
        return this;
    }

    @Override
    public void finish() {
        traceContext.onSpanFinished(this);
    }

    @Override
    public ITraceContext context() {
        return traceContext;
    }

    @Override
    public String spanId() {
        return spanId;
    }

    @Override
    public String parentSpanId() {
        return parentSpanId;
    }

    @Override
    public SpanKind kind() {
        return null;
    }

    @Override
    public ITraceSpan kind(SpanKind kind) {
        return null;
    }

    @Override
    public String component() {
        return null;
    }

    @Override
    public ITraceSpan component(String component) {
        return null;
    }

    @Override
    public Map<String, String> tags() {
        return Collections.emptyMap();
    }

    @Override
    public ITraceSpan tag(String name, String value) {
        return this;
    }

    @Override
    public ITraceSpan tag(Throwable exception) {
        return this;
    }

    @Override
    public ITraceSpan arg(String name, String value) {
        return null;
    }

    @Override
    public Map<String, String> args() {
        return null;
    }

    @Override
    public String parentApplication() {
        return null;
    }

    @Override
    public ITraceSpan parentApplication(String sourceApp) {
        return null;
    }

    @Override
    public String clazz() {
        return null;
    }

    @Override
    public String method() {
        return null;
    }

    @Override
    public ITraceSpan method(Executable method) {
        return this;
    }

    @Override
    public ITraceSpan method(String method) {
        return null;
    }

    @Override
    public long startTime() {
        return 0;
    }

    @Override
    public long endTime() {
        return 0;
    }

    @Override
    public ITraceSpan newChildSpan(String name) {
        return traceContext.onSpanCreated(new NoopTraceSpan(this.traceContext,
                                                            traceContext.spanIdGenerator().newSpanId(),
                                                            this.spanId)
                                              .component(name));
    }
}
