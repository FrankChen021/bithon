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

package org.bithon.agent.plugin.tomcat.interceptor;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.InterceptionDecision;
import org.bithon.agent.core.context.AgentContext;
import org.bithon.agent.core.context.InterceptorContext;
import org.bithon.agent.core.metric.domain.web.HttpIncomingFilter;
import org.bithon.agent.core.tracing.Tracer;
import org.bithon.agent.core.tracing.config.TraceConfig;
import org.bithon.agent.core.tracing.context.ITraceContext;
import org.bithon.agent.core.tracing.context.ITraceSpan;
import org.bithon.agent.core.tracing.context.SpanKind;
import org.bithon.agent.core.tracing.context.TraceContextHolder;

/**
 * {@link org.apache.catalina.core.StandardHostValve#invoke(Request, Response)}
 *
 * @author frankchen
 */
public class StandardHostValveInvoke extends AbstractInterceptor {

    private HttpIncomingFilter requestFilter;
    private TraceConfig traceConfig;

    @Override
    public boolean initialize() {
        requestFilter = new HttpIncomingFilter();
        traceConfig = AgentContext.getInstance()
                                  .getAgentConfiguration()
                                  .getConfig(TraceConfig.class);

        return true;
    }

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) {
        Request request = (Request) aopContext.getArgs()[0];

        if (requestFilter.shouldBeExcluded(request.getRequestURI(), request.getHeader("User-Agent"))) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        InterceptorContext.set(InterceptorContext.KEY_URI, request.getRequestURI());

        ITraceContext traceContext = Tracer.get()
                                           .propagator()
                                           .extract(request, (carrier, key) -> carrier.getHeader(key));

        TraceContextHolder.set(traceContext);
        traceContext.currentSpan()
                    .component("tomcat")
                    .tag("uri", request.getRequestURI())
                    .tag("method", request.getMethod())
                    .tag((span) -> traceConfig.getHeaders().forEach((header) -> span.tag("header." + header, request.getHeader(header))))
                    .method(aopContext.getMethod())
                    .kind(SpanKind.SERVER)
                    .start();

        aopContext.setUserContext(traceContext);

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        InterceptorContext.remove(InterceptorContext.KEY_URI);

        ITraceContext traceContext = aopContext.castUserContextAs();
        ITraceSpan span = null;
        try {
            if (traceContext == null) {
                //exception occurs in 'Enter'
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
            try {
                traceContext.finish();
            } catch (Exception ignored) {
            }
            try {
                TraceContextHolder.remove();
            } catch (Exception ignored) {
            }
        }
    }
}
