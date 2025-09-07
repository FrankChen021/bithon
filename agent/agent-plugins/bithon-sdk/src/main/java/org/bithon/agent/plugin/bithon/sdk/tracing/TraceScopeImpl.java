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
    private final long attachedThreadId;

    public TraceScopeImpl(ITraceContext context, ITraceSpan rootSpan) {
        this.context = context;
        this.rootSpan = new SpanImpl(rootSpan);

        TraceContextHolder.attach(context);
        this.attachedThreadId = Thread.currentThread().getId();
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
    public ISpan currentSpan() {
        ITraceContext currentContext = TraceContextHolder.current();
        return currentContext == null ? NoopSpan.INSTANCE : new SpanImpl(currentContext.currentSpan());
    }

    @Override
    public void close() {
        rootSpan.finish();
        if (context != null) {
            context.finish();
        }

        if (Thread.currentThread().getId() != attachedThreadId) {
            throw new SdkException("TraceScope is created in thread(id=%d), but is being closed from another thread(id=%d).",
                                   attachedThreadId,
                                   Thread.currentThread().getId());
        }
        TraceContextHolder.detach();
    }
}
