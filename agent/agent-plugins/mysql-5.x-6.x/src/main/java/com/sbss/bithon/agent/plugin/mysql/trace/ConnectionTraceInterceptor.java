package com.sbss.bithon.agent.plugin.mysql.trace;

import com.sbss.bithon.agent.boot.aop.AbstractInterceptor;
import com.sbss.bithon.agent.boot.aop.AopContext;
import com.sbss.bithon.agent.boot.aop.InterceptionDecision;
import com.sbss.bithon.agent.core.context.InterceptorContext;

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
