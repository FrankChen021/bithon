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

package org.bithon.agent.observability.tracing.context;

import org.bithon.component.commons.tracing.SpanKind;

import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/7/17 16:34
 */
class LoggingTraceSpan implements ITraceSpan {

    private final LoggingTraceContext traceContext;
    private final String spanId;
    private final String parentSpanId;

    public LoggingTraceSpan(LoggingTraceContext traceContext, String parentSpanId, String spanId) {
        this.traceContext = traceContext;
        this.spanId = spanId;
        this.parentSpanId = parentSpanId;
    }

    @Override
    public ITraceSpan start() {
        traceContext.onSpanStarted(this);
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
        return this;
    }

    @Override
    public String component() {
        return null;
    }

    @Override
    public ITraceSpan component(String component) {
        return this;
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
    public ITraceSpan tag(Consumer<ITraceSpan> config) {
        return this;
    }

    @Override
    public String parentApplication() {
        return null;
    }

    @Override
    public ITraceSpan parentApplication(String sourceApp) {
        return this;
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
    public ITraceSpan method(String clazz, String method) {
        return this;
    }

    @Override
    public ITraceSpan clazz(String clazz) {
        return this;
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
        return traceContext.newSpan(this.spanId, traceContext.spanIdGenerator().newSpanId())
                           .component(name);
    }
}
