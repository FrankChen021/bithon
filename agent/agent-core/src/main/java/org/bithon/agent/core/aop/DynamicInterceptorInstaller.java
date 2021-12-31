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

package org.bithon.agent.core.aop;

import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import shaded.net.bytebuddy.agent.builder.AgentBuilder;
import shaded.net.bytebuddy.asm.Advice;
import shaded.net.bytebuddy.description.method.MethodDescription;
import shaded.net.bytebuddy.description.type.TypeDescription;
import shaded.net.bytebuddy.dynamic.DynamicType;
import shaded.net.bytebuddy.matcher.ElementMatcher;
import shaded.net.bytebuddy.matcher.ElementMatchers;
import shaded.net.bytebuddy.matcher.NameMatcher;
import shaded.net.bytebuddy.matcher.StringSetMatcher;
import shaded.net.bytebuddy.utility.JavaModule;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/7/10 13:05
 */
public class DynamicInterceptorInstaller {
    private static final ILogAdaptor log = LoggerFactory.getLogger(DynamicInterceptorInstaller.class);
    private static final DynamicInterceptorInstaller INSTANCE = new DynamicInterceptorInstaller();

    private final Set<String> installedClassList = new ConcurrentSkipListSet<>();

    public static DynamicInterceptorInstaller getInstance() {
        return INSTANCE;
    }

    private DynamicType.Builder<?> install(AopDescriptor descriptor, DynamicType.Builder<?> builder) {
        if (installedClassList.add(descriptor.getTargetClass())) {
            log.info("Dynamically install interceptor for [{}]", descriptor.targetClass);
            return builder.visit(descriptor.advice.on(descriptor.methodMatcher));
        }
        return builder;
    }

    public void installOne(AopDescriptor descriptor) {
        new AgentBuilder.Default().ignore(ElementMatchers.nameStartsWith("shaded."))
                                  .disableClassFormatChanges()
                                  .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                                  .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                                  .type(ElementMatchers.named(descriptor.targetClass))
                                  .transform((builder, typeDescription, classLoader, javaModule) -> install(descriptor, builder))
                                  .with(new AopDebugger(new HashSet<>(Collections.singletonList(descriptor.getTargetClass()))))
                                  .installOn(InstrumentationHelper.getInstance());
    }

    public void install(Map<String, AopDescriptor> descriptors) {
        ElementMatcher<? super TypeDescription> typeMatcher = new NameMatcher<>(new StringSetMatcher(new HashSet<>(descriptors.keySet())));

        new AgentBuilder.Default().ignore(ElementMatchers.nameStartsWith("shaded."))
                                  .disableClassFormatChanges()
                                  .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                                  .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                                  .type(typeMatcher)
                                  .transform((DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader, JavaModule javaModule) -> {
                                      AopDescriptor descriptor = descriptors.get(typeDescription.getTypeName());
                                      if (descriptor == null) {
                                          // this must be an error
                                          log.error("Can't find BeanAopDescriptor for [{}]", typeDescription.getTypeName());
                                          return builder;
                                      }

                                      return install(descriptor, builder);

                                  })
                                  .with(new AopDebugger(new HashSet<>(descriptors.keySet())))
                                  .installOn(InstrumentationHelper.getInstance());
    }

    public static class AopDescriptor {
        private final String targetClass;
        private final Advice advice;
        private final ElementMatcher<? super MethodDescription> methodMatcher;

        public AopDescriptor(String targetClass, Advice advice, ElementMatcher<? super MethodDescription> methodMatcher) {
            this.targetClass = targetClass;
            this.advice = advice;
            this.methodMatcher = methodMatcher;
        }

        public String getTargetClass() {
            return targetClass;
        }

        public Advice getAdvice() {
            return advice;
        }

        public ElementMatcher<? super MethodDescription> getMethodMatcher() {
            return methodMatcher;
        }
    }
}
