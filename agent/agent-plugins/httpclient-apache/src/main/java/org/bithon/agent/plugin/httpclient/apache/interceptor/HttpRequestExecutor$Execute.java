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

package org.bithon.agent.plugin.httpclient.apache.interceptor;


import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.InterceptionDecision;
import org.bithon.agent.core.tracing.context.ITraceSpan;
import org.bithon.agent.core.tracing.context.SpanKind;
import org.bithon.agent.core.tracing.context.Tags;
import org.bithon.agent.core.tracing.context.TraceSpanFactory;

/**
 * @author frankchen
 */
public class HttpRequestExecutor$Execute extends AbstractInterceptor {

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) {
        HttpRequest httpRequest = (HttpRequest) aopContext.getArgs()[0];

        //
        // Trace
        //
        ITraceSpan span = TraceSpanFactory.newSpan("apache-httpClient");
        if (span == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        String uri;
        if (httpRequest instanceof HttpRequestWrapper) {
            // this getUri returns the full URI with host
            uri = ((HttpRequestWrapper) httpRequest).getOriginal().getRequestLine().getUri();
        } else {
            // this getUri returns the path and parameters, no host is included
            uri = httpRequest.getRequestLine().getUri();
        }

        // create a span and save it in user-context
        aopContext.setUserContext(span.method(aopContext.getMethod())
                                      .kind(SpanKind.CLIENT)
                                      .tag(Tags.URI, uri)
                                      .tag(Tags.HTTP_METHOD, httpRequest.getRequestLine().getMethod())
                                      .propagate(httpRequest, (request, key, value) -> request.setHeader(key, value))
                                      .start());

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void onMethodLeave(AopContext context) {
        ITraceSpan thisSpan = context.castUserContextAs();
        if (thisSpan == null) {
            // in case of exception in above
            return;
        }

        HttpResponse response = context.castReturningAs();
        String status = response.getStatusLine() == null ? "-1" : Integer.toString(response.getStatusLine().getStatusCode());
        thisSpan.tag(Tags.STATUS, status).tag(context.getException()).finish();
    }
}
