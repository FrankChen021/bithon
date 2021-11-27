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

package org.bithon.agent.plugin.spring.webflux.interceptor;

import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.IBithonObject;
import org.bithon.agent.bootstrap.aop.InterceptionDecision;
import org.bithon.agent.core.tracing.context.ITraceSpan;
import org.bithon.agent.plugin.spring.webflux.context.HttpClientContext;
import org.reactivestreams.Publisher;
import reactor.netty.Connection;
import reactor.netty.http.client.HttpClientResponse;

import java.util.function.BiFunction;

/**
 * see reactor.netty.http.client.HttpClientFinalizer#responseConnection
 *
 * @author Frank Chen
 * @date 27/11/21 1:57 pm
 */
public class HttpClientFinalizer$ResponseConnection extends AbstractInterceptor {

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) {
        IBithonObject bithonObject = aopContext.castTargetAs();

        // injected by HttpClientFinalizer$Send's onMethodLeave
        HttpClientContext clientContext = (HttpClientContext) bithonObject.getInjectedObject();
        if (clientContext == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        final ITraceSpan httpClientSpan = clientContext.getSpan();

        //noinspection unchecked,rawtypes
        BiFunction<? super HttpClientResponse, ? super Connection, ? extends Publisher> originalReceiver
            = (BiFunction<? super HttpClientResponse, ? super Connection, ? extends Publisher>) aopContext.getArgs()[0];

        //
        //replace original publisher so that we can stop the span
        //
        //noinspection rawtypes
        aopContext.getArgs()[0] = (BiFunction<HttpClientResponse, Connection, Publisher>) (httpClientResponse, connection) -> {
            //noinspection rawtypes
            Publisher publisher = originalReceiver.apply(httpClientResponse, connection);

            httpClientSpan.tag("status", String.valueOf(httpClientResponse.status().code()))
                          .finish();

            return publisher;
        };

        return InterceptionDecision.SKIP_LEAVE;
    }
}
