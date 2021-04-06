package com.sbss.bithon.agent.plugin.lettuce.interceptor;


import com.sbss.bithon.agent.boot.aop.AbstractInterceptor;
import com.sbss.bithon.agent.boot.aop.AopContext;
import com.sbss.bithon.agent.boot.aop.IBithonObject;

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
