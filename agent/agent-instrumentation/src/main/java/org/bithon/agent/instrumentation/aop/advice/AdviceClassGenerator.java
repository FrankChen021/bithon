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

package org.bithon.agent.instrumentation.aop.advice;


import org.bithon.agent.instrumentation.aop.InstrumentationHelper;
import org.bithon.shaded.net.bytebuddy.ByteBuddy;
import org.bithon.shaded.net.bytebuddy.dynamic.DynamicType;
import org.bithon.shaded.net.bytebuddy.dynamic.loading.ClassInjector;
import org.bithon.shaded.net.bytebuddy.matcher.ElementMatchers;

import java.util.HashMap;
import java.util.Map;

/**
 * Create an Aop class based on a class template.
 * Such template can be one of following:
 * {@link DynamicAopAdvice}
 * {@link ConstructorAfterAdvice}
 * {@link AroundAdvice}
 * {@link ReplacementAdvice}
 *
 * @author frankchen
 * @date 2021-02-18 19:23
 */
public class AdviceClassGenerator {

    public static DynamicType.Unloaded<?> generateAdviceClass(Class<?> aopTemplateClass,
                                                              String targetAopClassName,
                                                              String interceptorClassName,
                                                              boolean debug) {
        DynamicType.Unloaded<?> aopClassType = new ByteBuddy().redefine(aopTemplateClass)
                                                              .name(targetAopClassName)
                                                              // Assign interceptor name to field
                                                              .field(ElementMatchers.named("INTERCEPTOR_CLASS_NAME"))
                                                              .value(interceptorClassName)
                                                              .make();

        if (debug) {
            InstrumentationHelper.getAopDebugger().saveClazz(aopClassType);
        }

        return aopClassType;
    }

    public static ClassInjector.UsingUnsafe.Factory inject(DynamicType.Unloaded<?>... types) {
        Map<String, byte[]> typeMap = new HashMap<>();
        for (DynamicType.Unloaded<?> type : types) {
            typeMap.put(type.getTypeDescription().getTypeName(), type.getBytes());
        }
        return inject(typeMap);
    }

    private static ClassInjector.UsingUnsafe.Factory inject(Map<? extends String, byte[]> types) {
        ClassInjector.UsingUnsafe.Factory factory = ClassInjector.UsingUnsafe.Factory.resolve(InstrumentationHelper.getInstance());
        factory.make(null, null).injectRaw(types);
        return factory;
    }
}