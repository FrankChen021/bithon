package com.sbss.bithon.agent.plugin.apache.httpclient.trace;


import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.InterceptionDecision;
import com.sbss.bithon.agent.core.tracing.Tracer;
import com.sbss.bithon.agent.core.tracing.context.SpanKind;
import com.sbss.bithon.agent.core.tracing.context.TraceContext;
import com.sbss.bithon.agent.core.tracing.context.TraceContextHolder;
import com.sbss.bithon.agent.core.tracing.context.TraceSpan;
import com.sbss.bithon.agent.core.tracing.propagation.injector.PropagationSetter;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

/**
 * @author frankchen
 */
public class HttpRequestInterceptor extends AbstractInterceptor {
    private static final Logger log = LoggerFactory.getLogger(HttpRequestInterceptor.class);

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) {
        TraceContext tracer = TraceContextHolder.get();
        if (tracer == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        TraceSpan parentSpan = tracer.currentSpan();
        if (parentSpan == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        // create a span and save it in user-context
        HttpRequest httpRequest = (HttpRequest) aopContext.getArgs()[0];
        TraceSpan thisSpan = parentSpan.newChildSpan("httpClient")
            .clazz(aopContext.getTargetClass())
            .method(aopContext.getMethod())
            .kind(SpanKind.CLIENT)
            .tag("uri", httpRequest.getRequestLine().getUri())
            .start();
        aopContext.setUserContext(thisSpan);

        // propagate tracing
        Tracer.get()
            .propagator()
            .inject(tracer, httpRequest, (PropagationSetter<HttpRequest>) (request, key, value) -> {
                if (!request.containsHeader(key)) {
                    request.addHeader(key, value);
                }
            });

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void onMethodLeave(AopContext context) {
        TraceSpan thisSpan = (TraceSpan) context.getUserContext();
        if (thisSpan == null) {
            return;
        }

        try {
            HttpResponse response = (HttpResponse) context.castReturningAs();
            thisSpan.tag("status", Integer.toString(response.getStatusLine().getStatusCode()));
            if (context.hasException()) {
                thisSpan.tag("exception", context.getException().getClass().getSimpleName());
            }
        } finally {
            try {
                thisSpan.finish();
            } catch (Exception ignored) {
                log.warn("error to finish span", ignored);
            }
        }
    }
}
