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

package org.bithon.agent.bootstrap.aop.advice;

import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.BootstrapHelper;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Frank Chen
 * @date 18/2/22 8:04 PM
 */
public class InterceptorManager {

    private static final Map<String, AbstractInterceptor> INTERCEPTORS = new ConcurrentHashMap<>();

    public static AbstractInterceptor getOrCreateInterceptor(String interceptorClassName) {
        AbstractInterceptor interceptor = INTERCEPTORS.get(interceptorClassName);
        if (interceptor != null) {
            return interceptor;
        }

        try {
            // load class out of sync to eliminate potential dead lock
            Class<?> interceptorClass = Class.forName(interceptorClassName,
                                                      true,
                                                      BootstrapHelper.getPluginClassLoader());
            synchronized (InterceptorManager.class) {
                //double check
                interceptor = INTERCEPTORS.get(interceptorClassName);
                if (interceptor != null) {
                    return interceptor;
                }

                interceptor = (AbstractInterceptor) interceptorClass.newInstance();
            }
            interceptor.initialize();

            INTERCEPTORS.put(interceptorClassName, interceptor);

        } catch (Exception e) {
            BootstrapHelper.createAopLogger(InterceptorManager.class)
                           .error(String.format(Locale.ENGLISH, "Failed to instantiate interceptor [%s]", interceptorClassName), e);
        }
        return interceptor;
    }
}
