package com.sbss.bithon.agent.core.tracing.context;

import shaded.org.slf4j.LoggerFactory;

import java.lang.reflect.Executable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/5 8:49 下午
 */
public class TraceSpan {

    private final TraceContext traceContext;
    private final String spanId;
    private final String parentSpanId;
    private final Map<String, String> tags = new HashMap<>();
    private final Map<String, String> args = new HashMap<>();

    /**
     * in micro-seconds
     */
    private long startTime;
    private long endTime;
    private SpanKind kind;
    private String component;
    private String parentApplication;
    private String clazz;
    private String method;

    public TraceSpan(String spanId, String parentSpanId, TraceContext traceContext) {
        this.spanId = spanId;
        this.parentSpanId = parentSpanId;
        this.traceContext = traceContext;
    }

    public TraceContext context() {
        return traceContext;
    }

    public String traceId() {
        return traceContext.traceId();
    }

    public String spanId() {
        return spanId;
    }

    public String parentSpanId() {
        return parentSpanId;
    }

    public SpanKind kind() {
        return kind;
    }

    public TraceSpan kind(SpanKind kind) {
        this.kind = kind;
        return this;
    }

    public String component() {
        return component;
    }

    public TraceSpan component(String component) {
        this.component = component;
        return this;
    }

    public Map<String, String> tags() {
        return tags;
    }

    public TraceSpan tag(String name, String value) {
        if (value != null) {
            tags.put(name, value);
        }
        return this;
    }

    public TraceSpan tag(Throwable exception) {
        if (exception != null) {
            tags.put("exception", exception.toString());
        }
        return this;
    }

    public TraceSpan arg(String name, String value) {
        args.put(name, value);
        return this;
    }

    public Map<String, String> args() {
        return this.args;
    }

    public String parentApplication() {
        return parentApplication;
    }

    public TraceSpan parentApplication(String sourceApp) {
        this.parentApplication = sourceApp;
        return this;
    }

    public String clazz() {
        return clazz;
    }

    public TraceSpan clazz(Class<?> clazz) {
        this.clazz = clazz.getName();
        return this;
    }

    public String method() {
        return method;
    }

    public TraceSpan method(Executable method) {
        this.method = method.getName();
        return this;
    }

    public TraceSpan method(String method) {
        this.method = method;
        return this;
    }

    public long startTime() {
        return this.startTime;
    }

    public long endTime() {
        return this.endTime;
    }

    public TraceSpan newChildSpan(String name) {
        return traceContext.onSpanCreated(new TraceSpan(traceContext.spanIdGenerator().newSpanId(),
                                                        this.spanId,
                                                        this.traceContext)
                                              .component(name));
    }

    public TraceSpan start() {
        this.startTime = traceContext.clock().currentMicroseconds();
        return this;
    }

    public void finish() {
        this.endTime = context().clock().currentMicroseconds();
        try {
            this.traceContext.onSpanFinished(this);
        } catch (Throwable t) {
            LoggerFactory.getLogger(TraceSpan.class).error("Exception occured when finish a span", t);
        }
    }

    @Override
    public String toString() {
        return "TraceSpan[name=" + this.component +
               ", traceId=" + this.traceId() +
               ", spanId=" + this.spanId +
               ", parentId=" + this.parentSpanId +
               ", kind=" + this.kind +
               ", cost=" + (this.endTime - this.startTime) + "(micro seconds)" +
               "]";
    }
}
