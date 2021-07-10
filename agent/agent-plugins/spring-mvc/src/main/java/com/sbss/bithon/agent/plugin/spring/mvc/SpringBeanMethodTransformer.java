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

package com.sbss.bithon.agent.plugin.spring.mvc;

import com.sbss.bithon.agent.core.plugin.InstrumentationHelper;
import com.sbss.bithon.agent.core.plugin.debug.TransformationDebugger;
import shaded.net.bytebuddy.agent.builder.AgentBuilder;
import shaded.net.bytebuddy.asm.Advice;
import shaded.net.bytebuddy.description.method.MethodDescription;
import shaded.net.bytebuddy.matcher.ElementMatcher;
import shaded.net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static shaded.net.bytebuddy.matcher.ElementMatchers.none;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/7/10 13:05
 */
public class SpringBeanMethodTransformer {

    private static final Set<String> INSTRUMENTED = new ConcurrentSkipListSet<>();

    public static void transform(String beanName, Object bean) {
        if (beanName == null || bean == null) {
            return;
        }

        Class<?> clazz = bean.getClass();
        if (clazz.isSynthetic()) {
            /*
              eg: org.springframework.boot.actuate.autoconfigure.metrics.KafkaMetricsAutoConfiguration$$Lambda$709/829537923
             */
            return;
        }

        if (!INSTRUMENTED.add(clazz.getName())) {
            return;
        }

        AgentBuilder agentBuilder = InstrumentationHelper.getBuilder();
        agentBuilder.ignore(none())
                    .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                    .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                    .type(ElementMatchers.is(clazz),
                          ElementMatchers.is(clazz.getClassLoader()))
                    .transform((builder, typeDescription, classLoader, javaModule) ->
                                   builder.visit(Advice.to(SpringBeanMethodAop.class)
                                                       .on(BeanMethodMatcher.INSTANCE)))
                    .with(new TransformationDebugger())
                    .installOn(InstrumentationHelper.getInstance());
    }

    static class BeanMethodMatcher implements ElementMatcher<MethodDescription> {
        static BeanMethodMatcher INSTANCE = new BeanMethodMatcher();

        private final Set<String> objectMethods = Stream.of(Object.class.getMethods())
                                                        .map(Method::getName)
                                                        .collect(Collectors.toSet());

        @Override
        public boolean matches(MethodDescription target) {
            if (!target.isPublic()
                || target.isConstructor()
                || target.isStatic()
                || target.isAbstract()
                || target.isNative()) {
                return false;
            }

            return !objectMethods.contains(target.getName());
        }
    }
}
