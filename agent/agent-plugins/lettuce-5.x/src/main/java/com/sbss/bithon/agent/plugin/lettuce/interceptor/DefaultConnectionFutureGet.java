package com.sbss.bithon.agent.plugin.lettuce.interceptor;

import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.IBithonObject;

/**
 * @author frankchen
 */
public class DefaultConnectionFutureGet extends AbstractInterceptor {

    @Override
    public void onMethodLeave(AopContext aopContext) {
        Object result = aopContext.getReturning();
        if (result instanceof IBithonObject && aopContext.getTarget() instanceof IBithonObject) {
            ((IBithonObject) result).setInjectedObject(((IBithonObject) aopContext.getTarget()).getInjectedObject());
        }
    }
}
