package com.sbss.bithon.agent.core.plugin.aop;

import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import shaded.net.bytebuddy.implementation.bind.annotation.AllArguments;
import shaded.net.bytebuddy.implementation.bind.annotation.RuntimeType;
import shaded.net.bytebuddy.implementation.bind.annotation.This;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

/**
 * @author frankchen
 * @date 2020-12-31 22:21:36
 */
public class ConstructorAop {
    private static final Logger log = LoggerFactory.getLogger(ConstructorAop.class);

    private final AbstractInterceptor interceptor;

    public ConstructorAop(AbstractInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @RuntimeType
    public void onConstruct(@This Object obj,
                            @AllArguments Object[] args) {
        try {
            interceptor.onConstruct(obj, args);
        } catch (Exception e) {
            log.error(String.format("Error occurred during invoking %s.init()",
                                    interceptor.getClass().getSimpleName()),
                      e);
        }
    }
}
