package com.sbss.bithon.agent.core.interceptor;

import shaded.net.bytebuddy.implementation.bind.annotation.AllArguments;
import shaded.net.bytebuddy.implementation.bind.annotation.RuntimeType;
import shaded.net.bytebuddy.implementation.bind.annotation.This;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

/**
 * Description : 构造器增强切面, 在构造器调用时, 执行切面方法 <br>
 * Date: 17/12/5
 *
 * @author 马至远
 */
public class ConstructorInterceptor implements IMethodInterceptor {
    private static final Logger log = LoggerFactory.getLogger(ConstructorInterceptor.class);

    /**
     * 在构造器切面, 也可以复用eventCallback, 只需要执行其init方法就可以
     */
    private AbstractMethodIntercepted eventCallback;

    @RuntimeType
    public void onConstruct(@This Object obj,
                            @AllArguments Object[] args) {
        // 这里执行init其实是构造器执行完成之后, 所以这里的异常, 一定是agent本身产生的异常, 需要捕捉记录
        try {
            eventCallback.onConstruct(obj, args);
        } catch (Exception e) {
            log.error(String.format("Error occurred during invoking %s.init()",
                                    eventCallback.getClass().getSimpleName()),
                      e);
        }
    }

    public void setEventCallback(AbstractMethodIntercepted eventCallback) {
        this.eventCallback = eventCallback;
    }
}
