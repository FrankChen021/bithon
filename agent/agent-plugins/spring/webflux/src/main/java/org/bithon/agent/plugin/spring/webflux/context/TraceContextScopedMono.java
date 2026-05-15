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
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;
import reactor.util.context.Context;

/**
 * Stores Bithon's trace context in Reactor Context so ReactorTraceContextOperatorHook
 * can restore it during signal callbacks.
 */
public class TraceContextScopedMono<T> extends Mono<T> {
    private final Mono<T> source;
    private final ITraceContext traceContext;

    public static <T> Mono<T> wrap(Mono<T> source, ITraceContext traceContext) {
        if (traceContext == null) {
            return source;
        }
        return new TraceContextScopedMono<>(source, traceContext);
    }

    private TraceContextScopedMono(Mono<T> source, ITraceContext traceContext) {
        this.source = source;
        this.traceContext = traceContext;
    }

    @Override
    public void subscribe(CoreSubscriber<? super T> actual) {
        ITraceContext previous = TraceContextHolder.current();
        TraceContextHolder.attach(traceContext);
        try {
            if (source == null) {
                Operators.error(actual, new NullPointerException());
                return;
            }

            source.subscribe(new ContextWritingSubscriber<>(actual, traceContext));
        } catch (Throwable throwable) {
            Operators.error(actual, throwable);
        } finally {
            restore(previous);
        }
    }

    private void restore(ITraceContext previous) {
        if (previous == null) {
            TraceContextHolder.detach();
        } else {
            TraceContextHolder.attach(previous);
        }
    }

    private static class ContextWritingSubscriber<T> implements CoreSubscriber<T> {
        private final CoreSubscriber<? super T> delegate;
        private final ITraceContext traceContext;

        private ContextWritingSubscriber(CoreSubscriber<? super T> delegate, ITraceContext traceContext) {
            this.delegate = delegate;
            this.traceContext = traceContext;
        }

        @Override
        public Context currentContext() {
            return delegate.currentContext()
                           .put(ReactorTraceContextOperatorHook.TRACE_CONTEXT_KEY, traceContext);
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            delegate.onSubscribe(subscription);
        }

        @Override
        public void onNext(T value) {
            delegate.onNext(value);
        }

        @Override
        public void onError(Throwable throwable) {
            delegate.onError(throwable);
        }

        @Override
        public void onComplete() {
            delegate.onComplete();
        }
    }
}
