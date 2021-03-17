package com.sbss.bithon.agent.plugin.tomcat.interceptor;

import com.sbss.bithon.agent.core.context.InterceptorContext;
import com.sbss.bithon.agent.core.metrics.web.RequestUriFilter;
import com.sbss.bithon.agent.core.metrics.web.UserAgentFilter;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.InterceptionDecision;
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
                        .clazz(aopContext.getTargetClass())
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
            try {
                if (traceContext != null) {
                    traceContext.finish();
                }
            } catch (Exception ignored) {
            }
            try {
                TraceContextHolder.remove();
            } catch (Exception ignored) {
            }
        }
    }
}
