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

import java.lang.reflect.Method;

/**
 * @author frankchen
 * @date 2020-12-31 22:22:05
 */
public class MethodAop {

    private static final IAopLogger LOGGER = BootstrapHelper.createAopLogger(MethodAop.class);

    private final Object interceptor;

    public MethodAop(Object interceptor) {
        this.interceptor = interceptor;
    }

    @RuntimeType
    public Object intercept(@Origin Class<?> targetClass,
                            @Origin Method targetMethod,
                            @Morph ISuperMethod superMethod,
                            @This(optional = true) Object targetObject,
                            @AllArguments Object[] args) throws Exception {
        return AroundMethodAop.intercept(LOGGER,
                                         (AbstractInterceptor) interceptor,
                                         targetClass,
                                         superMethod,
                                         targetObject,
                                         targetMethod,
                                         args);
    }
}
