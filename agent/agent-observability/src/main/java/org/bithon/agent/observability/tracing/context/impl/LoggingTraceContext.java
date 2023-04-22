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

import org.bithon.agent.instrumentation.expt.AgentException;
import org.bithon.agent.observability.tracing.Tracer;
import org.bithon.agent.observability.tracing.context.ITraceContext;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceContextListener;
import org.bithon.agent.observability.tracing.context.TraceMode;
import org.bithon.agent.observability.tracing.context.propagation.PropagationSetter;
import org.bithon.agent.observability.tracing.id.ISpanIdGenerator;
import org.bithon.agent.observability.tracing.reporter.ITraceReporter;
import org.bithon.component.commons.time.Clock;

/**
 * trace context for logging
 *
 * @author frank.chen021@outlook.com
 * @date 2021/7/17 15:38
 */
public class LoggingTraceContext implements ITraceContext {

    //private final static ITraceReporter noopTraceReporter = spans -> {
    //};
    private final Clock clock = new Clock();

    private final ISpanIdGenerator spanIdGenerator;
    private ITraceSpan rootSpan;
    private final String traceId;

    public LoggingTraceContext(String traceId, ISpanIdGenerator spanIdGenerator) {
        this.traceId = traceId;
        this.spanIdGenerator = spanIdGenerator;
    }

    @Override
    public TraceMode traceMode() {
        return TraceMode.LOGGING;
    }

    @Override
    public String traceId() {
        return traceId;
    }

    @Override
    public ITraceSpan currentSpan() {
        return rootSpan;
    }

    @Override
    public Clock clock() {
        return clock;
    }

    @Override
    public ITraceReporter reporter() {
        return null;
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
        if (rootSpan != null) {
            throw new AgentException("Can't create span");
        }
        rootSpan = new LoggingTraceSpan(this, parentSpanId, spanId);
        return rootSpan;
    }

    @Override
    public void finish() {
    }

    @Override
    public <T> void propagate(T injectedTo, PropagationSetter<T> setter) {
        Tracer.get().propagator().inject(this, injectedTo, setter);
    }

    @Override
    public ITraceContext copy() {
        return new LoggingTraceContext(this.traceId, this.spanIdGenerator);
    }

    void onSpanStarted(ITraceSpan span) {
        TraceContextListener.getInstance().onSpanStarted(span);
    }

    void onSpanFinished(ITraceSpan span) {

        if (!rootSpan.equals(span)) {
            // TODO: internal error
            TraceContextListener.getInstance().onSpanFinished(span);
            return;
        }

        this.rootSpan = null;
        TraceContextListener.getInstance().onSpanFinished(span);
    }
}
