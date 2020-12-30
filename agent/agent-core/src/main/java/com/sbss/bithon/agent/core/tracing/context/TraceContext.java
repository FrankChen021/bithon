package com.sbss.bithon.agent.core.tracing.context;

import com.sbss.bithon.agent.core.tracing.Tracer;
import com.sbss.bithon.agent.core.tracing.context.impl.DefaultSpanIdGenerator;
import com.sbss.bithon.agent.core.tracing.propagation.injector.PropagationSetter;
import com.sbss.bithon.agent.core.tracing.report.ITraceReporter;
import com.sbss.bithon.agent.core.tracing.sampling.SamplingMode;
import com.sbss.bithon.agent.core.utils.time.Clock;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/5 8:48 下午
 */
public class TraceContext {

    private final ITraceReporter reporter;
    private final ITraceIdGenerator idGenerator;
    private final Stack<TraceSpan> spanStack = new Stack<>();
    private final List<TraceSpan> spans = new ArrayList<>();
    private final Clock clock = new Clock();
    private final String traceId;
    private final ISpanIdGenerator spanIdGenerator;
    private SamplingMode samplingMode;

    public TraceContext(String traceId,
                        ITraceReporter reporter,
                        ITraceIdGenerator idGenerator) {
        this.traceId = traceId;
        this.reporter = reporter;
        this.idGenerator = idGenerator;
        this.samplingMode = samplingMode;
        this.spanIdGenerator = new DefaultSpanIdGenerator();
        this.onSpanCreated(new TraceSpan(spanIdGenerator.newSpanId(), null, this).start());
    }

    public TraceContext(String traceId,
                        String spanId,
                        String parentSpanId,
                        ITraceReporter reporter,
                        ITraceIdGenerator idGenerator) {
        this.traceId = traceId;
        this.reporter = reporter;
        this.idGenerator = idGenerator;
        this.spanIdGenerator = new DefaultSpanIdGenerator();
        this.onSpanCreated(new TraceSpan(spanId, parentSpanId, this).start());
    }

    public String traceId() {
        return traceId;
    }

    public TraceSpan currentSpan() {
        return spanStack.peek();
    }

    public Clock clock() {
        return clock;
    }

    public ITraceIdGenerator traceIdGenerator() {
        return idGenerator;
    }

    public ISpanIdGenerator spanIdGenerator() {
        return spanIdGenerator;
    }

    public void finish() {
        if (!spanStack.isEmpty()) {
            // TODO: ERROR
        }
        this.reporter.report(this.spans);
    }

    TraceSpan onSpanCreated(TraceSpan span) {
        spanStack.push(span);
        spans.add(span);

        return span;
    }

    void onSpanFinished(TraceSpan traceSpan) {
        if (spanStack.isEmpty()) {
            // TODO: internal error
            return;
        }

        if (!spanStack.peek().equals(traceSpan)) {
            // TODO: internal error

            return;
        }

        spanStack.pop();
        if (spanStack.isEmpty()) {
            // TODO: report span
        }
    }

    public TraceContext samplingMode(SamplingMode mode) {
        this.samplingMode = mode;
        return this;
    }

    public <T> void propagate(T injectedTo, PropagationSetter<T> setter) {
        Tracer.get()
            .propagator()
            .inject(this, injectedTo, setter);
    }
}
