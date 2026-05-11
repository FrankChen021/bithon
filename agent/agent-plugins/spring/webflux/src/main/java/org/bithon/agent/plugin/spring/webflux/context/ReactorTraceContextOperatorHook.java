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

package org.bithon.agent.plugin.spring.webflux.context;

import org.bithon.agent.observability.tracing.context.ITraceContext;
import org.bithon.agent.observability.tracing.context.TraceContextHolder;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Operators;
import reactor.util.context.Context;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Propagates the current Bithon trace context to Reactor signal callbacks.
 */
public final class ReactorTraceContextOperatorHook {
    private static final String HOOK_KEY = "bithon-trace-context";
    public static final Object TRACE_CONTEXT_KEY = ReactorTraceContextOperatorHook.class;
    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);

    private ReactorTraceContextOperatorHook() {
    }

    public static void install() {
        if (!INSTALLED.compareAndSet(false, true)) {
            return;
        }

        Hooks.onEachOperator(HOOK_KEY, Operators.lift((scannable, actual) -> new TraceContextSubscriber<>(actual)));
    }

    private static class TraceContextSubscriber<T> implements CoreSubscriber<T> {
        private final CoreSubscriber<? super T> delegate;

        private TraceContextSubscriber(CoreSubscriber<? super T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Context currentContext() {
            return delegate.currentContext();
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            withTraceContext(() -> delegate.onSubscribe(subscription));
        }

        @Override
        public void onNext(T value) {
            withTraceContext(() -> delegate.onNext(value));
        }

        @Override
        public void onError(Throwable throwable) {
            withTraceContext(() -> delegate.onError(throwable));
        }

        @Override
        public void onComplete() {
            withTraceContext(delegate::onComplete);
        }

        private void withTraceContext(Runnable runnable) {
            Object value = currentContext().getOrDefault(TRACE_CONTEXT_KEY, null);
            if (!(value instanceof ITraceContext)) {
                runnable.run();
                return;
            }

            ITraceContext traceContext = (ITraceContext) value;
            if (traceContext.finished()) {
                runnable.run();
                return;
            }

            ITraceContext previous = TraceContextHolder.current();
            TraceContextHolder.attach(traceContext);
            try {
                runnable.run();
            } finally {
                restore(previous);
            }
        }
    }

    private static void restore(ITraceContext previous) {
        if (previous == null) {
            TraceContextHolder.detach();
        } else {
            TraceContextHolder.attach(previous);
        }
    }
}
