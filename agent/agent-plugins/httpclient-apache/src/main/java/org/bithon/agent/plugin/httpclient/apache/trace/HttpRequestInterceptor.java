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

package org.bithon.agent.plugin.httpclient.apache.trace;


import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.InterceptionDecision;
import org.bithon.agent.core.context.AgentContext;
import org.bithon.agent.core.tracing.context.ITraceSpan;
import org.bithon.agent.core.tracing.context.SpanKind;
import org.bithon.agent.core.tracing.context.TraceSpanFactory;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

/**
 * @author frankchen
 */
public class HttpRequestInterceptor extends AbstractInterceptor {
    private static final Logger log = LoggerFactory.getLogger(HttpRequestInterceptor.class);

    private String srcApplication;

    @Override
    public boolean initialize() throws Exception {
        srcApplication = AgentContext.getInstance().getAppInstance().getAppName();
        return super.initialize();
    }

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) {
        HttpRequest httpRequest = (HttpRequest) aopContext.getArgs()[0];

        //
        // Trace
        //
        ITraceSpan span = TraceSpanFactory.newSpan("httpClient");
        if (span == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        // create a span and save it in user-context
        aopContext.setUserContext(span.method(aopContext.getMethod())
                                      .kind(SpanKind.CLIENT)
                                      .tag("uri", httpRequest.getRequestLine().getUri())
                                      .propagate(httpRequest,
                                                 (request, key, value) -> request.setHeader(key, value))
                                      .start());

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void onMethodLeave(AopContext context) {
        ITraceSpan thisSpan = context.castUserContextAs();
        if (thisSpan == null) {
            return;
        }

        try {
            HttpResponse response = context.castReturningAs();
            thisSpan.tag("status", Integer.toString(response.getStatusLine().getStatusCode()));
            if (context.hasException()) {
                thisSpan.tag("exception", context.getException().getClass().getSimpleName());
            }
        } finally {
            try {
                thisSpan.finish();
            } catch (Exception e) {
                log.warn("error to finish span", e);
            }
        }
    }
}
