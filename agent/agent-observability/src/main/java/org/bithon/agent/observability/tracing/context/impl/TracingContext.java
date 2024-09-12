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

import org.bithon.agent.configuration.ConfigurationManager;
import org.bithon.agent.observability.tracing.Tracer;
import org.bithon.agent.observability.tracing.config.TraceConfig;
import org.bithon.agent.observability.tracing.context.ITraceContext;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceContextListener;
import org.bithon.agent.observability.tracing.context.TraceMode;
import org.bithon.agent.observability.tracing.context.TraceState;
import org.bithon.agent.observability.tracing.context.propagation.PropagationSetter;
import org.bithon.agent.observability.tracing.id.ISpanIdGenerator;
import org.bithon.agent.observability.tracing.reporter.ITraceReporter;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.time.Clock;
import org.bithon.component.commons.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/5 8:48 下午
 */
public class TracingContext implements ITraceContext {

    private static final boolean IS_DEBUG_ENABLED = ConfigurationManager.getInstance().getConfig(TraceConfig.class).isDebug();

    private final Stack<ITraceSpan> spanStack = new Stack<>();
    private final List<ITraceSpan> spans = new ArrayList<>();
    private final Clock clock = new Clock();
    private final String traceId;
    private final ISpanIdGenerator spanIdGenerator;
    private ITraceReporter reporter;
    private TraceState attributes;

    private boolean finished = false;

    public TracingContext(String traceId,
                          ISpanIdGenerator spanIdGenerator) {
        this.traceId = traceId;
        this.spanIdGenerator = spanIdGenerator;
    }

    @Override
    public ITraceContext traceState(TraceState attributes) {
        this.attributes = attributes;
        return this;
    }

    @Override
    public TraceState traceState() {
        return attributes;
    }

    @Override
    public TraceMode traceMode() {
        return TraceMode.TRACING;
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
        return reporter;
    }

    @Override
    public ITraceContext reporter(ITraceReporter reporter) {
        this.reporter = reporter;
        return this;
    }

    @Override
    public ISpanIdGenerator spanIdGenerator() {
        return spanIdGenerator;
    }

    @Override
    public ITraceSpan newSpan(String parentSpanId, String spanId) {
        ITraceSpan span = new TracingSpan(spanId, parentSpanId, this);
        this.onSpanCreated(span);
        return span;
    }

    @Override
    public void finish() {
        if (!spanStack.isEmpty()) {
            if (IS_DEBUG_ENABLED) {
                LoggerFactory.getLogger(TracingContext.class)
                             .warn(StringUtils.format("TraceContext does not finish correctly. "
                                                      + "[%d] spans are still remained unfinished. This IS a bug.\nRemained spans: \n%s",
                                                      spanStack.size(),
                                                      spanStack),
                                   new RuntimeException("Bug Detected"));
            } else {
                LoggerFactory.getLogger(TracingContext.class).warn("TraceContext does not finish correctly. "
                                                                   + "[{}] spans are still remained unfinished. This IS a bug.\n"
                                                                   + "Please adding -Dbithon.tracing.debug=true parameter to your application to turn on the span life time message to debug. \nRemained spans: \n{}",
                                                                   spanStack.size(),
                                                                   spanStack);
            }

            this.spans.clear();
            this.spanStack.clear();
            this.finished = true;
            return;
        }

        // Allow this method to be re-entered
        if (this.finished) {
            return;
        }

        // Mark the context as FINISHED first to prevent user code to access spans in the implementation of 'report' below
        this.finished = true;
        try {
            this.reporter.report(this.spans);
        } catch (Throwable e) {
            LoggerFactory.getLogger(TracingContext.class).error("Exception occurred when finish a context", e);
        } finally {
            // Clear to allow this method to re-enter
            this.spans.clear();
        }
    }

    private void onSpanCreated(ITraceSpan span) {
        spanStack.push(span);
        spans.add(span);
    }

    void onSpanStarted(TracingSpan span) {
        TraceContextListener.getInstance().onSpanStarted(span);
    }

    void onSpanFinished(TracingSpan span) {

        if (spanStack.isEmpty()) {
            TraceContextListener.getInstance().onSpanFinished(span);

            if (IS_DEBUG_ENABLED) {
                LoggerFactory.getLogger(TracingContext.class)
                             .warn(StringUtils.format("Try to finish a span which is not in the stack. This IS a bug.\nCurrent span: \n%s",
                                                      span),
                                   new RuntimeException("Bug Detected"));
            } else {
                LoggerFactory.getLogger(TracingContext.class).warn("Try to finish a span which is not in the stack. This IS a bug.\n"
                                                                   + "Please adding -Dbithon.tracing.debug=true parameter to your application to turn on the span life time message to debug. \nCurrent span: \n{}",
                                                                   span);
            }
            return;
        }

        if (!spanStack.peek().equals(span)) {
            TraceContextListener.getInstance().onSpanFinished(span);

            if (IS_DEBUG_ENABLED) {
                LoggerFactory.getLogger(TracingContext.class)
                             .warn(StringUtils.format("Try to finish a span which does not match the span in the stack. This IS a bug.\nCurrent span: \n%s, \n Unfinished Spans:\n%s",
                                                      span,
                                                      spanStack),
                                   new RuntimeException("Bug Detected"));
            } else {
                LoggerFactory.getLogger(TracingContext.class).warn("Try to finish a span which is not in the stack. This IS a bug.\n"
                                                                   + "Please adding -Dbithon.tracing.debug=true parameter to your application to turn on the span life time message to debug. \nCurrent span: \n{}, Unfinished Spans:\n{}",
                                                                   span,
                                                                   spanStack);
            }
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

    @Override
    public <T> void propagate(T injectedTo, PropagationSetter<T> setter) {
        Tracer.get()
              .propagator()
              .inject(this, injectedTo, setter);
    }

    @Override
    public ITraceContext copy() {
        return new TracingContext(this.traceId, this.spanIdGenerator).reporter(this.reporter);
    }

    @Override
    public boolean finished() {
        return this.finished;
    }
}
