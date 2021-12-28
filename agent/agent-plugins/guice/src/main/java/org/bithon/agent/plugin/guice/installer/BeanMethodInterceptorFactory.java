/*
 *    Copyright 2020 bithon.org
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

package org.bithon.agent.plugin.guice.installer;

import org.bithon.agent.bootstrap.aop.BootstrapHelper;
import org.bithon.agent.bootstrap.aop.advice.IAdviceInterceptor;
import shaded.net.bytebuddy.asm.Advice;

import java.util.Locale;

/**
 * Classes of spring beans are re-transformed after these classes are loaded,
 * so we have to use {@link Advice} to intercept methods
 * <p>
 * <b>Important Note</b>
 * This class will be injected into bootstrap class loader,
 * ALL its dependencies must be in bootstrap class loader too
 *
 * @author frank.chen021@outlook.com
 * @date 2021/7/10 16:45
 */
public class BeanMethodInterceptorFactory {

    private static final String INTERCEPTOR_CLASS_NAME = "org.bithon.agent.plugin.guice.installer.BeanMethodInterceptorImpl";

    private static IAdviceInterceptor interceptorInstance;

    public static IAdviceInterceptor getOrCreate() {
        if (interceptorInstance != null) {
            return interceptorInstance;
        }

        try {
            // load class out of sync to eliminate potential dead lock
            Class<?> interceptorClass = Class.forName(INTERCEPTOR_CLASS_NAME,
                                                      true,
                                                      BootstrapHelper.getPluginClassLoader());
            synchronized (BeanMethodInterceptorFactory.class) {
                //double check
                if (interceptorInstance != null) {
                    return interceptorInstance;
                }

                interceptorInstance = (IAdviceInterceptor) interceptorClass.newInstance();
            }

        } catch (Exception e) {
            BootstrapHelper.createAopLogger(BeanMethodInterceptorFactory.class)
                           .error(String.format(Locale.ENGLISH, "Failed to create interceptor [%s]", INTERCEPTOR_CLASS_NAME), e);
        }
        return interceptorInstance;
    }
}
