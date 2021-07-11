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

package com.sbss.bithon.agent.plugin.tomcat.interceptor;

import com.sbss.bithon.agent.bootstrap.aop.AbstractInterceptor;
import com.sbss.bithon.agent.bootstrap.aop.AopContext;
import com.sbss.bithon.agent.bootstrap.aop.InterceptionDecision;
import com.sbss.bithon.agent.core.context.InterceptorContext;
import com.sbss.bithon.agent.core.metric.domain.web.RequestUriFilter;
import com.sbss.bithon.agent.core.metric.domain.web.UserAgentFilter;
import com.sbss.bithon.agent.core.tracing.Tracer;
import com.sbss.bithon.agent.core.tracing.context.SpanKind;
import com.sbss.bithon.agent.core.tracing.context.TraceContext;
import com.sbss.bithon.agent.core.tracing.context.TraceContextHolder;
import com.sbss.bithon.agent.core.tracing.context.TraceSpan;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;

/**
 * implement Tracing
 *
 * @author frankchen
 */
public class StandardHostValveInvoke extends AbstractInterceptor {

    private UserAgentFilter userAgentFilter;
    private RequestUriFilter uriFilter;

    @Override
    public boolean initialize() {
        userAgentFilter = new UserAgentFilter();
        uriFilter = new RequestUriFilter();
        return true;
    }

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) {
        Request request = (Request) aopContext.getArgs()[0];

        if (uriFilter.isFiltered(request.getRequestURI())
            || userAgentFilter.isFiltered(request.getHeader("User-Agent"))) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        InterceptorContext.set(InterceptorContext.KEY_URI, request.getRequestURI());

        TraceContext traceContext = Tracer.get()
                                          .propagator()
                                          .extract(request, (carrier, key) -> carrier.getHeader(key));
        if (traceContext != null) {
            TraceContextHolder.set(traceContext);
            InterceptorContext.set(InterceptorContext.KEY_TRACEID, traceContext.traceId());

            traceContext.currentSpan()
                        .component("tomcat")
                        .tag("uri", request.getRequestURI())
                        .method(aopContext.getMethod())
                        .kind(SpanKind.SERVER)
                        .start();
        }

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        InterceptorContext.remove(InterceptorContext.KEY_URI);

        TraceContext traceContext = null;
        TraceSpan span = null;
        try {
            traceContext = TraceContextHolder.get();
            if (traceContext == null) {
                return;
            }
            span = traceContext.currentSpan();
            if (span == null) {
                // TODO: ERROR
                return;
            }

            Response response = (Response) aopContext.getArgs()[1];
            span.tag("status", Integer.toString(response.getStatus()));
            if (aopContext.hasException()) {
                span.tag("exception", aopContext.getException().toString());
            }
        } finally {
            try {
                if (span != null) {
                    span.finish();
                }
            } catch (Exception ignored) {
            }
            if (traceContext != null) {
                traceContext.finish();
            }
            try {
                TraceContextHolder.remove();
            } catch (Exception ignored) {
            }
        }
    }
}
