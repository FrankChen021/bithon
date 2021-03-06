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

package org.bithon.agent.core.tracing.context;

import org.bithon.component.commons.logging.LoggerFactory;

import java.lang.reflect.Executable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/5 8:49 下午
 */
class TraceSpan implements ITraceSpan {

    private final TraceContext traceContext;
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

    public TraceSpan(String spanId, String parentSpanId, TraceContext traceContext) {
        this.spanId = spanId;
        this.parentSpanId = parentSpanId;
        this.traceContext = traceContext;
        this.endTime = 0;
    }

    @Override
    public ITraceContext context() {
        return traceContext;
    }

    @Override
    public String traceId() {
        return traceContext.traceId();
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
    public TraceSpan kind(SpanKind kind) {
        this.kind = kind;
        return this;
    }

    @Override
    public String component() {
        return component;
    }

    @Override
    public TraceSpan component(String component) {
        this.component = component;
        return this;
    }

    @Override
    public Map<String, String> tags() {
        return tags;
    }

    @Override
    public TraceSpan tag(String name, String value) {
        if (value != null) {
            tags.put(name, value);
        }
        return this;
    }

    @Override
    public TraceSpan tag(Throwable exception) {
        if (exception != null) {
            tags.put("exception", exception.toString());
        }
        return this;
    }

    @Override
    public String parentApplication() {
        return parentApplication;
    }

    @Override
    public TraceSpan parentApplication(String sourceApp) {
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
    public TraceSpan method(Executable method) {
        this.method = method.getName();
        this.clazz = method.getDeclaringClass().getName();
        return this;
    }

    @Override
    public TraceSpan method(String method) {
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
        return traceContext.newSpan(this.spanId,
                                    traceContext.spanIdGenerator().newSpanId())
                           .component(name);
    }

    @Override
    public TraceSpan start() {
        this.startTime = traceContext.clock().currentMicroseconds();
        this.traceContext.onSpanStarted(this);
        return this;
    }

    @Override
    public void finish() {
        if (this.endTime != 0) {
            return;
        }
        this.endTime = context().clock().currentMicroseconds();
        try {
            this.traceContext.onSpanFinished(this);
        } catch (Throwable t) {
            LoggerFactory.getLogger(TraceSpan.class).error("Exception occurred when finish a span", t);
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
