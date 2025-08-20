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
import org.bithon.agent.observability.tracing.reporter.BatchReporter;
import org.bithon.agent.observability.tracing.reporter.ITraceReporter;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.time.Clock;
import org.bithon.component.commons.utils.StringUtils;

import java.util.Stack;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/5 8:48 下午
 */
public class TracingContext implements ITraceContext {

    private static final boolean IS_DEBUG_ENABLED = ConfigurationManager.getInstance().getConfig(TraceConfig.class).isDebug();

    private final Stack<ITraceSpan> spanStack = new Stack<>();
    private final Clock clock;

    /**
     * The traceId of this context.
     */
    private final String traceId;
    private final ISpanIdGenerator spanIdGenerator;
    private final ITraceReporter reporter;

    /**
     * Trace state that received from upstream service and needs to be propagated to the downstream service.
     */
    private TraceState traceState;

    private boolean finished = false;

    public TracingContext(String traceId,
                          ISpanIdGenerator spanIdGenerator) {
        this(traceId, spanIdGenerator, new Clock());
    }

    private TracingContext(String traceId,
                           ISpanIdGenerator spanIdGenerator,
                           Clock clock) {
        this.traceId = traceId;
        this.spanIdGenerator = spanIdGenerator;
        this.clock = clock;
        this.reporter = new BatchReporter(Tracer.get().reporter());
    }

    @Override
    public ITraceContext traceState(TraceState attributes) {
        this.traceState = attributes;
        return this;
    }

    @Override
    public TraceState traceState() {
        return traceState;
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
    public ISpanIdGenerator spanIdGenerator() {
        return spanIdGenerator;
    }

    @Override
    public ITraceSpan newSpan(String parentSpanId, String spanId) {
        ITraceSpan span = new TracingSpan(spanId, parentSpanId, this);

        spanStack.push(span);

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

            this.spanStack.clear();
            this.finished = true;
            return;
        }

        // Allow this method to be re-entered
        if (!this.finished) {
            this.finished = true;

            this.reporter.flush();
        }
    }

    void onSpanStarted(TracingSpan span) {
        TraceContextListener.getInstance().onSpanStarted(span);
    }

    void onSpanFinished(TracingSpan span) {
        try {
            TraceContextListener.getInstance().onSpanFinished(span);
        } catch (Throwable t) {
            LoggerFactory.getLogger(TracingContext.class)
                         .warn("Exception occurred when notifying span finished: " + span, t);
        }

        // Report the span
        this.reporter.report(span);

        if (spanStack.isEmpty() || !spanStack.peek().equals(span)) {
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
        } else {
            spanStack.pop();
        }
    }

    @Override
    public <T> void propagate(T injectedTo, PropagationSetter<T> setter) {
        Tracer.get()
              .propagator()
              .inject(this, injectedTo, setter);
    }

    @Override
    public ITraceContext copy() {
        return new TracingContext(this.traceId,
                                  this.spanIdGenerator,
                                  // For all copied trace context that has the same traceId,
                                  // use the same clock to ensure the microsecond calculation is based on the same time base
                                  this.clock);
    }

    @Override
    public boolean finished() {
        return this.finished;
    }
}
