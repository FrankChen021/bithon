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

package org.bithon.agent.plugin.bithon.sdk.tracing;

import org.bithon.agent.observability.tracing.context.ITraceContext;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceContextHolder;
import org.bithon.agent.observability.tracing.context.TraceMode;
import org.bithon.agent.sdk.expt.SdkException;
import org.bithon.agent.sdk.tracing.ISpan;
import org.bithon.agent.sdk.tracing.ITraceScope;
import org.bithon.agent.sdk.tracing.TracingMode;
import org.bithon.agent.sdk.tracing.impl.NoopSpan;

/**
 * Implementation of TraceScope that bridges SDK interface with agent internals
 *
 * @author frank.chen021@outlook.com
 * @date 2025/09/09 15:32
 */
public class TraceScopeImpl implements ITraceScope {
    private final ITraceContext context;
    private final ISpan rootSpan;
    private boolean attached = false;

    public TraceScopeImpl(ITraceContext context, ITraceSpan rootSpan) {
        this.context = context;
        this.rootSpan = new SpanImpl(rootSpan);
    }

    @Override
    public String currentTraceId() {
        return context != null ? context.traceId() : "";
    }

    @Override
    public TracingMode tracingMode() {
        if (context == null) {
            return TracingMode.LOGGING;
        }
        TraceMode mode = context.traceMode();
        return mode == TraceMode.TRACING ? TracingMode.TRACING : TracingMode.LOGGING;
    }

    @Override
    public ITraceScope attach() {
        return attach(true); // Default behavior: start span
    }

    @Override
    public ITraceScope attach(boolean startSpan) {
        ITraceContext currentContext = TraceContextHolder.current();
        if (currentContext != null) {
            throw new SdkException("There's already a trace context(traceId=%s) attached to the current thread.", currentContext.traceId());
        }

        if (!attached && context != null) {
            TraceContextHolder.attach(context);
            attached = true;

            // Start the root span if requested and available
            if (startSpan) {
                rootSpan.start();
            }
        }
        return this;
    }

    @Override
    public void detach() {
        if (attached) {
            TraceContextHolder.detach();
            attached = false;
        }
    }

    @Override
    public ISpan currentSpan() {
        if (!attached) {
            return rootSpan;
        }

        // If already attached, return the current span from the context
        ITraceContext currentContext = TraceContextHolder.current();
        return currentContext == null ? NoopSpan.INSTANCE : new SpanImpl(currentContext.currentSpan());
    }

    @Override
    public void close() {
        detach();

        rootSpan.finish();
        if (context != null) {
            context.finish();
        }
    }
}
