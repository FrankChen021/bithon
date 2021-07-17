/*
 *    Copyright 2020 bithon.cn
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

package com.sbss.bithon.agent.plugin.httpclient.apache.trace;


import com.sbss.bithon.agent.bootstrap.aop.AbstractInterceptor;
import com.sbss.bithon.agent.bootstrap.aop.AopContext;
import com.sbss.bithon.agent.bootstrap.aop.InterceptionDecision;
import com.sbss.bithon.agent.core.context.AgentContext;
import com.sbss.bithon.agent.core.context.InterceptorContext;
import com.sbss.bithon.agent.core.tracing.Tracer;
import com.sbss.bithon.agent.core.tracing.context.ITraceContext;
import com.sbss.bithon.agent.core.tracing.context.SpanKind;
import com.sbss.bithon.agent.core.tracing.context.TraceContextHolder;
import com.sbss.bithon.agent.core.tracing.context.TraceSpan;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
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
        httpRequest.setHeader(InterceptorContext.HEADER_SRC_APPLICATION_NAME, srcApplication);

        //
        // Trace
        //
        ITraceContext tracer = TraceContextHolder.get();
        if (tracer == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        TraceSpan parentSpan = tracer.currentSpan();
        if (parentSpan == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        // create a span and save it in user-context
        TraceSpan thisSpan = parentSpan.newChildSpan("httpClient")
                                       .method(aopContext.getMethod())
                                       .kind(SpanKind.CLIENT)
                                       .tag("uri", httpRequest.getRequestLine().getUri())
                                       .start();
        aopContext.setUserContext(thisSpan);

        // propagate tracing
        Tracer.get()
              .propagator()
              .inject(tracer, httpRequest, (request, key, value) -> request.setHeader(key, value));

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void onMethodLeave(AopContext context) {
        TraceSpan thisSpan = (TraceSpan) context.getUserContext();
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
