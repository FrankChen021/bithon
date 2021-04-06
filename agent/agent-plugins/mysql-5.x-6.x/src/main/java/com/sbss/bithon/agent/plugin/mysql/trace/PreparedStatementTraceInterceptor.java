package com.sbss.bithon.agent.plugin.mysql.trace;

import com.sbss.bithon.agent.core.context.InterceptorContext;
import com.sbss.bithon.agent.boot.aop.AbstractInterceptor;
import com.sbss.bithon.agent.boot.aop.AopContext;
import com.sbss.bithon.agent.boot.aop.InterceptionDecision;
import com.sbss.bithon.agent.core.tracing.context.SpanKind;
import com.sbss.bithon.agent.core.tracing.context.TraceContext;
import com.sbss.bithon.agent.core.tracing.context.TraceContextHolder;
import com.sbss.bithon.agent.core.tracing.context.TraceSpan;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

/**
 * @author frankchen
 */
public class PreparedStatementTraceInterceptor extends AbstractInterceptor {
    private static final Logger log = LoggerFactory.getLogger(PreparedStatementTraceInterceptor.class);

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) {
        TraceContext traceContext = TraceContextHolder.get();
        if (traceContext == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        TraceSpan parentSpan = traceContext.currentSpan();
        if (parentSpan == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        // create a span and save it in user-context
        aopContext.setUserContext(parentSpan.newChildSpan("mysql")
                                            .clazz(aopContext.getTargetClass())
                                            .method(aopContext.getMethod())
                                            .kind(SpanKind.CLIENT)
                                            .start());

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        TraceSpan mysqlSpan = aopContext.castUserContextAs();
        if (mysqlSpan == null) {
            return;
        }
        try {
            String sql;
            if ((sql = (String) InterceptorContext.get(ConnectionTraceInterceptor.KEY)) != null) {
                mysqlSpan.tag("sql", sql);
            }
        } finally {
            try {
                mysqlSpan.finish();
            } catch (Exception e) {
                log.warn(e.getMessage(), e);
            }
        }
    }
}
