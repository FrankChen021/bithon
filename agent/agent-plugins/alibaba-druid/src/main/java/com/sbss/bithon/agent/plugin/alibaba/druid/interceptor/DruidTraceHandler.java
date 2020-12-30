package com.sbss.bithon.agent.plugin.alibaba.druid.interceptor;

import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.InterceptionDecision;
import com.sbss.bithon.agent.core.tracing.context.SpanKind;
import com.sbss.bithon.agent.core.tracing.context.TraceContext;
import com.sbss.bithon.agent.core.tracing.context.TraceContextHolder;
import com.sbss.bithon.agent.core.tracing.context.TraceSpan;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

/**
 * @author frankchen
 */
public class DruidTraceHandler extends AbstractInterceptor {
    private static final Logger log = LoggerFactory.getLogger(DruidTraceHandler.class);

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
        TraceSpan thisSpan = parentSpan.newChildSpan("Druid")
            .clazz(aopContext.getTargetClass())
            .method(aopContext.getMethod())
            .kind(SpanKind.CLIENT)
            //TODO:
            //.tag("db", )
            .start();
        aopContext.setUserContext(thisSpan);

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        TraceSpan thisSpan = (TraceSpan) aopContext.getUserContext();
        if (thisSpan == null) {
            return;
        }

        try {
            if (aopContext.hasException()) {
                thisSpan.tag("exception", aopContext.getException().getClass().getSimpleName());
            }
            if (aopContext.getArgs() != null && aopContext.getArgs().length > 0) {
                thisSpan.tag("sql", aopContext.getArgs()[0].toString());
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
