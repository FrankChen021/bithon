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

package org.bithon.agent.plugin.grpc.client.interceptor;

import org.bithon.agent.instrumentation.aop.interceptor.declaration.IDynamicInterceptor;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceSpanFactory;
import org.bithon.component.commons.tracing.SpanKind;

import java.lang.reflect.Method;

/**
 * @author Frank Chen
 * @date 13/12/22 6:06 pm
 */
public class AbstractGrpcStubInterceptor extends IDynamicInterceptor {

    private final String component;

    protected AbstractGrpcStubInterceptor(String component) {
        this.component = component;
    }

    @Override
    public Object onMethodEnter(
        final Method method,
        final Object target,
        final Object[] args
    ) {
        ITraceSpan span = TraceSpanFactory.newSpan(component);
        if (span == null) {
            return null;
        }

        return span.method(method)
                   .kind(SpanKind.CLIENT)
                   .start();
    }

    @Override
    public Object onMethodExit(final Method method,
                               final Object target,
                               final Object[] args,
                               final Object returning,
                               final Throwable exception,
                               final Object context) {
        ((ITraceSpan) context).tag(exception).finish();
        return returning;
    }

    public static class BlockingStubInterceptor extends AbstractGrpcStubInterceptor {
        public BlockingStubInterceptor() {
            super("grpc-blocking-client");
        }
    }

    public static class FutureStubInterceptor extends AbstractGrpcStubInterceptor {
        public FutureStubInterceptor() {
            super("grpc-future-client");
        }
    }

    public static class AsyncStubInterceptor extends AbstractGrpcStubInterceptor {
        public AsyncStubInterceptor() {
            super("grpc-async-client");
        }
    }
}
