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

package org.bithon.agent.plugin.spring.webflux.gateway.aop;

import org.bithon.agent.bootstrap.aop.IBithonObject;
import org.bithon.agent.bootstrap.aop.advice.IAdviceInterceptor;
import org.bithon.agent.core.tracing.context.ITraceContext;
import org.bithon.agent.core.tracing.context.ITraceSpan;
import org.bithon.agent.core.tracing.context.TraceContextHolder;
import org.bithon.agent.plugin.spring.webflux.context.HttpServerContext;
import org.springframework.http.server.reactive.AbstractServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;

/**
 * NOTE:
 * Any update of class/package name of this class must be manually reflected to {@link BeanMethodInterceptorFactory#INTERCEPTOR_CLASS_NAME},
 * or the Bean interception WON'T WORK
 *
 * @author frank.chen021@outlook.com
 * @date 2021/7/10 18:46
 */
public class FilterMethodInterceptorImpl implements IAdviceInterceptor {

    @Override
    public Object onMethodEnter(
        final Method method,
        final Object target,
        final Object[] args
    ) {
        ServerWebExchange exchange = (ServerWebExchange) args[0];

        // ReactorHttpHandlerAdapter#apply creates an object of AbstractServerHttpRequest
        if (!(exchange.getRequest() instanceof AbstractServerHttpRequest)) {
            return null;
        }

        // the request object on exchange is type of HttpServerOperation
        // see ReactorHttpHandlerAdapter#apply
        Object nativeRequest = ((AbstractServerHttpRequest) exchange.getRequest()).getNativeRequest();
        if (!(nativeRequest instanceof IBithonObject)) {
            return null;
        }

        HttpServerContext ctx = (HttpServerContext) ((IBithonObject) nativeRequest).getInjectedObject();
        ITraceContext traceContext = ctx.getTraceContext();
        if (traceContext == null) {
            return null;
        }

        // set to thread local for following calls such as HttpClientFinalizer
        TraceContextHolder.set(traceContext);

        return traceContext.currentSpan()
                           .newChildSpan("gateway-filter")
                           .method(method)
                           .start();
    }

    @Override
    public Object onMethodExit(final Method method,
                               final Object target,
                               final Object[] args,
                               final Object returning,
                               final Throwable exception,
                               final Object context) {
        TraceContextHolder.remove();

        ITraceSpan span = (ITraceSpan) context;
        if (span == null) {
            // in case of exception in the Enter interceptor
            return returning;
        }
        if (exception != null) {
            span.tag(exception).finish();
            return returning;
        }

        //noinspection unchecked
        Mono<Void> originalReturning = (Mono<Void>) returning;
        //noinspection deprecation
        return originalReturning.doAfterSuccessOrError((success, error) -> ((ITraceSpan) context).tag(error).finish());
    }
}
