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

import org.bithon.agent.observability.tracing.Tracer;
import org.bithon.agent.observability.tracing.context.propagation.PropagationSetter;
import org.bithon.agent.observability.tracing.context.propagation.TraceMode;
import org.bithon.agent.observability.tracing.id.ISpanIdGenerator;
import org.bithon.agent.observability.tracing.reporter.ITraceReporter;
import org.bithon.component.commons.time.Clock;

import java.util.Stack;

/**
 *
 * trace context for logging and propagation
 *
 * @author frank.chen021@outlook.com
 * @date 2021/7/17 15:38
 */
public class PropagationTraceContext implements ITraceContext {

    private final ITraceReporter noopTraceReporter = spans -> {
    };
    private final Clock clock = new Clock();

    private final ISpanIdGenerator spanIdGenerator;
    private final Stack<ITraceSpan> spanStack = new Stack<>();
    private final String traceId;
    private boolean finished = false;

    public PropagationTraceContext(String traceId, ISpanIdGenerator spanIdGenerator) {
        this.traceId = traceId;
        this.spanIdGenerator = spanIdGenerator;
    }

    @Override
    public TraceMode traceMode() {
        return TraceMode.PROPAGATION;
    }

    @Override
    public String traceId() {
        return traceId;
    }

    @Override
    public ITraceSpan currentSpan() {
        return spanStack.isEmpty() ? null : spanStack.peek();
    }

    @Override
    public Clock clock() {
        return clock;
    }

    @Override
    public ITraceReporter reporter() {
        return noopTraceReporter;
    }

    @Override
    public ITraceContext reporter(ITraceReporter reporter) {
        return this;
    }

    @Override
    public ISpanIdGenerator spanIdGenerator() {
        return spanIdGenerator;
    }

    @Override
    public ITraceSpan newSpan(String parentSpanId, String spanId) {
        PropagationTraceSpan span = new PropagationTraceSpan(this, parentSpanId, spanId);
        this.onSpanCreated(span);
        return span;
    }

    @Override
    public void finish() {
        if (!spanStack.isEmpty()) {
            // TODO: error
            spanStack.clear();
        }
        this.finished = true;
    }

    @Override
    public <T> void propagate(T injectedTo, PropagationSetter<T> setter) {
        Tracer.get().propagator().inject(this, injectedTo, setter);
    }

    @Override
    public boolean finished() {
        return this.finished;
    }

    private void onSpanCreated(ITraceSpan span) {
        spanStack.push(span);
    }

    void onSpanStarted(ITraceSpan span) {
        TraceContextListener.getInstance().onSpanStarted(span);
    }

    void onSpanFinished(ITraceSpan span) {
        if (spanStack.isEmpty()) {
            // TODO: internal error
            TraceContextListener.getInstance().onSpanFinished(span);
            return;
        }

        if (!spanStack.peek().equals(span)) {
            // TODO: internal error
            TraceContextListener.getInstance().onSpanFinished(span);
            return;
        }

        spanStack.pop();
        if (spanStack.isEmpty()) {
            // TODO: report span
            TraceContextListener.getInstance().onSpanFinished(span);
            return;
        }

        TraceContextListener.getInstance().onSpanFinished(span);
    }
}
