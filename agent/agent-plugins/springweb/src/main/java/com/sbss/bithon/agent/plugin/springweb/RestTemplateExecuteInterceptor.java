package com.sbss.bithon.agent.plugin.springweb;

import com.sbss.bithon.agent.boot.aop.AbstractInterceptor;
import com.sbss.bithon.agent.boot.aop.AopContext;
import com.sbss.bithon.agent.boot.aop.InterceptionDecision;
import com.sbss.bithon.agent.core.tracing.context.SpanKind;
import com.sbss.bithon.agent.core.tracing.context.TraceContext;
import com.sbss.bithon.agent.core.tracing.context.TraceContextHolder;
import com.sbss.bithon.agent.core.tracing.context.TraceSpan;

import java.net.URI;

/**
 * @author frankchen
 * @date 2021-02-16 14:36
 */
public class RestTemplateExecuteInterceptor extends AbstractInterceptor {
    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) {
        TraceContext traceContext = TraceContextHolder.get();
        if (traceContext == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }
        TraceSpan span = traceContext.currentSpan();
        if (span == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        String uri = null;
        Object obj = aopContext.getArgs()[0];
        if (obj instanceof String) {
            uri = (String) obj;
        } else if (obj instanceof URI) {
            uri = obj.toString();
        }

        aopContext.setUserContext(span.newChildSpan("restTemplate")
                                      .clazz(aopContext.getTargetClass())
                                      .method(aopContext.getMethod())
                                      .kind(SpanKind.CLIENT)
                                      .tag("uri", uri)
                                      .start());

        return InterceptionDecision.CONTINUE;
    }


    @Override
    public void onMethodLeave(AopContext aopContext) {
        TraceSpan span = (TraceSpan) aopContext.getUserContext();
        if (span == null) {
            return;
        }
        span.finish();
    }
}
