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

package com.sbss.bithon.agent.boot.aop;

import shaded.net.bytebuddy.implementation.bind.annotation.AllArguments;
import shaded.net.bytebuddy.implementation.bind.annotation.Origin;
import shaded.net.bytebuddy.implementation.bind.annotation.RuntimeType;
import shaded.net.bytebuddy.implementation.bind.annotation.This;

import java.lang.reflect.Constructor;

/**
 * @author frankchen
 * @date 2020-12-31 22:21:36
 */
public class ConstructorAop {
    private static final IAopLogger log = BootstrapHelper.createAopLogger(ConstructorAop.class);

    private final AbstractInterceptor interceptor;

    public ConstructorAop(AbstractInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @RuntimeType
    public void onConstruct(@Origin Class<?> targetClass,
                            @Origin Constructor<?> constructor,
                            @This Object targetObject,
                            @AllArguments Object[] args) {
        try {
            interceptor.onConstruct(new AopContext(targetClass, constructor, targetObject, args));
        } catch (Exception e) {
            log.error(String.format("Error occurred during invoking %s.onConstruct()",
                                    interceptor.getClass().getSimpleName()),
                      e);
        }
    }
}
