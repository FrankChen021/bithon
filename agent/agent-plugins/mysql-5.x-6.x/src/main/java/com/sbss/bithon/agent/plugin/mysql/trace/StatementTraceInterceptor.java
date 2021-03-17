package com.sbss.bithon.agent.plugin.mysql.trace;

import com.sbss.bithon.agent.core.context.InterceptorContext;
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
public class StatementTraceInterceptor extends AbstractInterceptor {
    private static final Logger log = LoggerFactory.getLogger(StatementTraceInterceptor.class);

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) {

        // TODO: filter "select @@session.tx_read_only"

        TraceContext traceContext = TraceContextHolder.get();
        if (traceContext == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        TraceSpan parentSpan = traceContext.currentSpan();
        if (parentSpan == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        aopContext.setUserContext(parentSpan
                                      .newChildSpan("mysql")
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
//            if (context.getArgs() != null && context.getArgs().length > 0 && needIgnore(context.getArgs()[0].toString())) {
//                return;
//            }

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
