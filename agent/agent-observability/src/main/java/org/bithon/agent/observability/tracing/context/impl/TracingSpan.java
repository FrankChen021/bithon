/*
 *    Copyright 2020 bithon.org
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.bithon.agent.observability.tracing.context.impl;

import org.bithon.agent.observability.tracing.context.ITraceContext;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.component.commons.exception.ExceptionUtils;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.tracing.SpanKind;
import org.bithon.component.commons.tracing.Tags;

import java.util.HashMap;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/5 8:49 下午
 */
class TracingSpan implements ITraceSpan {

    private final TracingContext tracingContext;
    private final String spanId;
    private final String parentSpanId;
    private final Map<String, String> tags = new HashMap<>();

    /**
     * in micro-seconds
     */
    private long startTime;
    private long endTime;
    private SpanKind kind = SpanKind.NONE;
    private String component;
    private String parentApplication;
    private String clazz;
    private String method;

    public TracingSpan(String spanId, String parentSpanId, TracingContext tracingContext) {
        this.spanId = spanId;
        this.parentSpanId = parentSpanId;
        this.tracingContext = tracingContext;
        this.endTime = 0;
    }

    @Override
    public ITraceContext context() {
        return tracingContext;
    }

    @Override
    public String traceId() {
        return tracingContext.traceId();
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
        return kind;
    }

    @Override
    public TracingSpan kind(SpanKind kind) {
        this.kind = kind;
        return this;
    }

    @Override
    public String component() {
        return component;
    }

    @Override
    public TracingSpan component(String component) {
        this.component = component;
        return this;
    }

    @Override
    public Map<String, String> tags() {
        return tags;
    }

    @Override
    public TracingSpan tag(String name, String value) {
        if (value != null) {
            tags.put(name, value);
        }
        return this;
    }

    @Override
    public TracingSpan tag(Throwable throwable) {
        if (throwable != null) {
            tags.put(Tags.Exception.TYPE, throwable.getClass().getName());
            tags.put(Tags.Exception.MESSAGE, throwable.getMessage());
            tags.put(Tags.Exception.STACKTRACE, ExceptionUtils.getStackTrace(throwable));
        }
        return this;
    }

    @Override
    public String parentApplication() {
        return parentApplication;
    }

    @Override
    public TracingSpan parentApplication(String sourceApp) {
        this.parentApplication = sourceApp;
        return this;
    }

    @Override
    public String clazz() {
        return clazz;
    }

    @Override
    public String method() {
        return method;
    }

    @Override
    public TracingSpan method(String clazz, String method) {
        this.clazz = clazz;
        this.method = method;
        return this;
    }

    @Override
    public ITraceSpan clazz(String clazz) {
        this.clazz = clazz;
        return this;
    }

    @Override
    public long startTime() {
        return this.startTime;
    }

    @Override
    public long endTime() {
        return this.endTime;
    }

    @Override
    public ITraceSpan newChildSpan(String name) {
        return tracingContext.newSpan(this.spanId,
                                      tracingContext.spanIdGenerator().newSpanId())
                             .component(name);
    }

    @Override
    public TracingSpan start() {
        this.startTime = tracingContext.clock().currentMicroseconds();
        this.tracingContext.onSpanStarted(this);
        return this;
    }

    @Override
    public void finish() {
        if (this.endTime != 0) {
            return;
        }
        this.endTime = context().clock().currentMicroseconds();
        try {
            this.tracingContext.onSpanFinished(this);
        } catch (Throwable t) {
            LoggerFactory.getLogger(TracingSpan.class).error("Exception occurred when finish a span", t);
        }
    }

    @Override
    public String toString() {
        return "TraceSpan[name=" + this.component +
                ", traceId=" + this.traceId() +
                ", spanId=" + this.spanId +
                ", parentId=" + this.parentSpanId +
                ", clazz=" + this.clazz +
                ", method=" + this.method +
                ", kind=" + this.kind +
                ", cost=" + (this.endTime - this.startTime) + "(micro seconds)" +
                "]";
    }
}
