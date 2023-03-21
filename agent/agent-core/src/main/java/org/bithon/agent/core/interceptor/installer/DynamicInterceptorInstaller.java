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

package org.bithon.agent.core.interceptor.installer;

import org.bithon.agent.instrumentation.aop.InstrumentationHelper;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.shaded.net.bytebuddy.agent.builder.AgentBuilder;
import org.bithon.shaded.net.bytebuddy.asm.Advice;
import org.bithon.shaded.net.bytebuddy.description.method.MethodDescription;
import org.bithon.shaded.net.bytebuddy.description.type.TypeDescription;
import org.bithon.shaded.net.bytebuddy.dynamic.DynamicType;
import org.bithon.shaded.net.bytebuddy.matcher.ElementMatcher;
import org.bithon.shaded.net.bytebuddy.matcher.ElementMatchers;
import org.bithon.shaded.net.bytebuddy.matcher.NameMatcher;
import org.bithon.shaded.net.bytebuddy.matcher.StringSetMatcher;
import org.bithon.shaded.net.bytebuddy.utility.JavaModule;

import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/7/10 13:05
 */
public class DynamicInterceptorInstaller {
    private static final ILogAdaptor LOG = LoggerFactory.getLogger(DynamicInterceptorInstaller.class);
    private static final DynamicInterceptorInstaller INSTANCE = new DynamicInterceptorInstaller();

    public static DynamicInterceptorInstaller getInstance() {
        return INSTANCE;
    }

    private DynamicType.Builder<?> install(AopDescriptor descriptor,
                                           DynamicType.Builder<?> builder) {

        LOG.info("Dynamically install interceptor for [{}]", descriptor.getTargetClass());
        return builder.visit(descriptor.getAdvice().on(descriptor.getMethodMatcher()));
    }

    /**
     * Install one interceptor
     */
    public void installOne(AopDescriptor descriptor) {
        AgentBuilder agentBuilder =
            new AgentBuilder.Default().ignore(ElementMatchers.nameStartsWith("org.bithon.shaded."))
                                      .disableClassFormatChanges()
                                      .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                                      .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                                      .type(ElementMatchers.named(descriptor.targetClass))
                                      .transform((builder, typeDescription, classLoader, javaModule, protectionDomain) -> install(descriptor, builder));
        if (InstrumentationHelper.getAopDebugger().isEnabled()) {
            agentBuilder = agentBuilder.with(InstrumentationHelper.getAopDebugger()
                                                                  .withTypes(new HashSet<>(Collections.singletonList(descriptor.getTargetClass()))));
        }
        agentBuilder.installOn(InstrumentationHelper.getInstance());
    }

    /**
     * Install multiple interceptors at one time
     */
    public void install(Map<String, AopDescriptor> descriptors) {
        ElementMatcher<? super TypeDescription> typeMatcher = new NameMatcher<>(new StringSetMatcher(new HashSet<>(descriptors.keySet())));

        AgentBuilder agentBuilder =
            new AgentBuilder.Default().ignore(ElementMatchers.nameStartsWith("org.bithon.shaded."))
                                      .disableClassFormatChanges()
                                      .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                                      .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                                      .type(typeMatcher)
                                      .transform((DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader, JavaModule javaModule, ProtectionDomain protectionDomain) -> {

                                          AopDescriptor descriptor = descriptors.get(typeDescription.getTypeName());
                                          if (descriptor == null) {
                                              // this must be an error
                                              LOG.error("Can't find BeanAopDescriptor for [{}]", typeDescription.getTypeName());
                                              return builder;
                                          }

                                          return install(descriptor, builder);
                                      });

        if (InstrumentationHelper.getAopDebugger().isEnabled()) {
            agentBuilder = agentBuilder.with(InstrumentationHelper.getAopDebugger()
                                                                  .withTypes(new HashSet<>(descriptors.keySet())));
        }
        agentBuilder.installOn(InstrumentationHelper.getInstance());
    }

    public static class AopDescriptor {
        private final String targetClass;
        private final Advice advice;
        private final ElementMatcher.Junction<MethodDescription> methodMatcher;

        public AopDescriptor(String targetClass,
                             Advice advice,
                             ElementMatcher.Junction<MethodDescription> methodMatcher) {
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

        public ElementMatcher.Junction<MethodDescription> getMethodMatcher() {
            return methodMatcher;
        }
    }
}
