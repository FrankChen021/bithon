package com.sbss.bithon.agent.core.plugin.aop.bootstrap;

/**
 * <pre>
 *     NOTE: subclass of this class MUST be declared as PUBLIC
 * </pre>
 *
 * @author frankchen
 * @date 2020-12-31 22:20:11
 */
public abstract class AbstractInterceptor {

    /**
     * @return false if this interceptor should not be loaded
     */
    public boolean initialize() throws Exception {
        return true;
    }

    public void onConstruct(Object constructedObject,
                            Object[] args) throws Exception {
    }

    public InterceptionDecision onMethodEnter(AopContext aopContext) throws Exception {
        return InterceptionDecision.CONTINUE;
    }

    /**
     * Called after execution of target intercepted method
     * If {@link #onMethodEnter} returns {@link InterceptionDecision#SKIP_LEAVE}, call of this method will be skipped
     */
    public void onMethodLeave(AopContext aopContext) throws Exception {
    }
}
