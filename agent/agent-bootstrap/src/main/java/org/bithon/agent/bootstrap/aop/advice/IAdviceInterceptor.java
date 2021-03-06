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

import java.lang.reflect.Method;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/7/11 11:27
 */
public interface IAdviceInterceptor {

    /**
     * @return context
     */
    Object onMethodEnter(
        Method method,
        Object target,
        Object[] args
    );

    /**
     * only be called when the returning value of {@link #onMethodEnter(Method, Object, Object[])} is NOT NULL
     *
     * @param context the returning value of {@link #onMethodEnter(Method, Object, Object[])}
     */
    default Object onMethodExit(Method method,
                                Object target,
                                Object[] args,
                                Object returning,
                                Throwable exception,
                                Object context) {
        return returning;
    }
}
