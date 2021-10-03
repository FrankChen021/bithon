/*
 *    Copyright 2020 bithon.cn
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.sbss.bithon.agent.bootstrap.aop;

import shaded.net.bytebuddy.implementation.bind.annotation.AllArguments;
import shaded.net.bytebuddy.implementation.bind.annotation.Morph;
import shaded.net.bytebuddy.implementation.bind.annotation.Origin;
import shaded.net.bytebuddy.implementation.bind.annotation.RuntimeType;
import shaded.net.bytebuddy.implementation.bind.annotation.This;
import shaded.net.bytebuddy.pool.TypePool;

import java.lang.reflect.Method;
import java.util.Map;


/**
 * @author frankchen
 * @date 2021-02-18 20:20
 */
public class BootstrapMethodAop {
    /**
     * assigned by {@link com.sbss.bithon.agent.core.plugin.loader.BootstrapInterceptorInstaller#generateAopClass(Map, TypePool, String, String, com.sbss.bithon.agent.core.aop.descriptor.MethodPointCutDescriptor)}
     */
    private static String INTERCEPTOR_CLASS_NAME;

    private static AbstractInterceptor INTERCEPTOR;
    private static IAopLogger log;

    @RuntimeType
    public static Object intercept(@Origin Class<?> targetClass,
                                   @Morph ISuperMethod superMethod,
                                   @This(optional = true) Object target,
                                   @Origin Method method,
                                   @AllArguments Object[] args) throws Exception {
        AbstractInterceptor interceptor = ensureInterceptor();
        if (interceptor == null) {
            return superMethod.invoke(args);
        }

        return AroundMethodAopImpl.intercept(log,
                                             INTERCEPTOR,
                                             targetClass,
                                             superMethod,
                                             target,
                                             method,
                                             args);
    }

    private static AbstractInterceptor ensureInterceptor() {
        if (INTERCEPTOR != null) {
            return INTERCEPTOR;
        }

        log = BootstrapHelper.createAopLogger(BootstrapMethodAop.class);

        try {
            // load class out of sync to eliminate potential dead lock
            Class<?> interceptorClass = Class.forName(INTERCEPTOR_CLASS_NAME,
                                                      true,
                                                      BootstrapHelper.getPluginClassLoader());
            synchronized (INTERCEPTOR_CLASS_NAME) {
                //double check
                if (INTERCEPTOR != null) {
                    return INTERCEPTOR;
                }

                INTERCEPTOR = (AbstractInterceptor) interceptorClass.newInstance();
            }
            INTERCEPTOR.initialize();

        } catch (Exception e) {
            log.error(String.format("Failed to instantiate interceptor [%s]", INTERCEPTOR_CLASS_NAME), e);
        }
        return INTERCEPTOR;
    }
}

