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

package org.bithon.agent.plugin.httpclient.jdk.interceptor;

import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AroundInterceptor;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceSpanFactory;
import org.bithon.component.commons.tracing.Tags;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * {@link HttpURLConnection#connect()}
 *
 * @author Frank Chen
 * @date 25/10/23 4:49 pm
 */
public class HttpURLConnection$Connect extends AroundInterceptor {
    @Override
    public InterceptionDecision before(AopContext aopContext) {
        ITraceSpan span = TraceSpanFactory.newSpan("httpclient");
        if (span == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        HttpURLConnection connection = aopContext.getTargetAs();
        URL url = connection.getURL();

        /*
         * starts a span which will be finished after HttpClient.parseHttp
         */
        aopContext.setUserContext(span.method(aopContext.getTargetClass(), aopContext.getMethod())
                                      // Since this span does not propagate the tracing context to next hop,
                                      // it's not marked as SpanKind.CLIENT
                                      .tag(Tags.Http.CLIENT, "jdk")
                                      .tag(Tags.Net.PEER, url.getPort() == -1 ? url.getHost() : (url.getHost() + ":" + url.getPort()))
                                      // No need to write URL and method to avoid repetition
                                      //.tag(Tags.Http.URL, connection.getURL().toString())
                                      //.tag(Tags.Http.METHOD, connection.getRequestMethod())
                                      .start());

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void after(AopContext aopContext) {
        ITraceSpan span = aopContext.getUserContextAs();
        span.tag(aopContext.getException()).finish();
    }
}
