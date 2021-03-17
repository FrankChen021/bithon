package com.sbss.bithon.agent.plugin.mysql.trace;

import com.sbss.bithon.agent.core.context.InterceptorContext;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.InterceptionDecision;

/**
 * @author frankchen
 */
public class ConnectionTraceInterceptor extends AbstractInterceptor {

    public static final String KEY = "sql";

    @Override
    public InterceptionDecision onMethodEnter(AopContext context) {
        if (context.getArgs() != null && context.getArgs().length > 0) {
            InterceptorContext.set(KEY, context.getArgs()[0].toString());
            return InterceptionDecision.CONTINUE;
        }

        return InterceptionDecision.SKIP_LEAVE;
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        //InterceptorContext.remove(KEY);
    }
}
