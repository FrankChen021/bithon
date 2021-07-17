/*
 *    Copyright 2020 bithon.cn
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

package com.sbss.bithon.agent.core.tracing.context;

import com.sbss.bithon.agent.core.tracing.Tracer;
import com.sbss.bithon.agent.core.tracing.propagation.injector.PropagationSetter;
import com.sbss.bithon.agent.core.tracing.report.ITraceReporter;
import com.sbss.bithon.agent.core.tracing.sampling.SamplingMode;
import com.sbss.bithon.agent.core.utils.time.Clock;

import java.util.Stack;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/7/17 15:38
 */
public class NoopTraceContext implements ITraceContext {

    private final ITraceReporter noopTraceReporter = spans -> {
    };
    private final Clock clock = new Clock();

    private final ITraceIdGenerator traceIdGenerator;
    private final ISpanIdGenerator spanIdGenerator;
    private final Stack<ITraceSpan> spanStack = new Stack<>();

    public NoopTraceContext(ITraceIdGenerator traceIdGenerator,
                            ISpanIdGenerator spanIdGenerator) {
        this.traceIdGenerator = traceIdGenerator;
        this.spanIdGenerator = spanIdGenerator;
    }

    @Override
    public String traceId() {
        return null;
    }

    @Override
    public ITraceSpan currentSpan() {
        return spanStack.peek();
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
    public ITraceIdGenerator traceIdGenerator() {
        return traceIdGenerator;
    }

    @Override
    public ISpanIdGenerator spanIdGenerator() {
        return spanIdGenerator;
    }

    @Override
    public void finish() {
    }

    @Override
    public ITraceContext samplingMode(SamplingMode mode) {
        return null;
    }

    @Override
    public <T> void propagate(T injectedTo, PropagationSetter<T> setter) {
        Tracer.get().propagator().inject(this, injectedTo, setter);
    }

    ITraceSpan onSpanCreated(ITraceSpan span) {
        spanStack.push(span);
        return span;
    }

    void onSpanFinished(ITraceSpan traceSpan) {
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
}
