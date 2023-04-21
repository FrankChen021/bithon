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

package org.bithon.agent.instrumentation.aop.interceptor.installer;

import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.InstrumentationHelper;
import org.bithon.agent.instrumentation.aop.advice.AdviceAnnotation;
import org.bithon.agent.instrumentation.aop.advice.DynamicAopAdvice;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptorManager;
import org.bithon.agent.instrumentation.logging.ILogger;
import org.bithon.agent.instrumentation.logging.LoggerFactory;
import org.bithon.shaded.net.bytebuddy.agent.builder.AgentBuilder;
import org.bithon.shaded.net.bytebuddy.asm.Advice;
import org.bithon.shaded.net.bytebuddy.description.method.MethodDescription;
import org.bithon.shaded.net.bytebuddy.description.type.TypeDescription;
import org.bithon.shaded.net.bytebuddy.dynamic.DynamicType;
import org.bithon.shaded.net.bytebuddy.implementation.FieldAccessor;
import org.bithon.shaded.net.bytebuddy.jar.asm.Opcodes;
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
    private static final ILogger LOG = LoggerFactory.getLogger(DynamicInterceptorInstaller.class);
    private static final DynamicInterceptorInstaller INSTANCE = new DynamicInterceptorInstaller();

    public static DynamicInterceptorInstaller getInstance() {
        return INSTANCE;
    }

    /**
     * Install one interceptor
     */
    public void installOne(AopDescriptor descriptor) {
        new AgentBuilder.Default()
            .ignore(ElementMatchers.nameStartsWith("org.bithon.shaded.net.bytebuddy."))
            .disableClassFormatChanges()
            .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
            .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
            .type(ElementMatchers.named(descriptor.targetClass))
            .transform((builder, typeDescription, classLoader, javaModule, protectionDomain) -> install(descriptor, builder, classLoader))
            .with(InstrumentationHelper.getAopDebugger().withTypes(new HashSet<>(Collections.singletonList(descriptor.targetClass))))
            .installOn(InstrumentationHelper.getInstance());
    }

    /**
     * Install multiple interceptors at one time
     */
    public void install(Map<String, AopDescriptor> descriptors) {
        ElementMatcher<? super TypeDescription> typeMatcher = new NameMatcher<>(new StringSetMatcher(new HashSet<>(descriptors.keySet())));

        new AgentBuilder.Default()
            .ignore(ElementMatchers.nameStartsWith("org.bithon.shaded.net.bytebuddy."))
            .disableClassFormatChanges()
            .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
            .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
            .type(typeMatcher)
            .transform((DynamicType.Builder<?> builder,
                        TypeDescription typeDescription,
                        ClassLoader classLoader,
                        JavaModule javaModule,
                        ProtectionDomain protectionDomain) -> {

                AopDescriptor descriptor = descriptors.get(typeDescription.getTypeName());
                if (descriptor == null) {
                    // this must be an error
                    LOG.error("Can't find BeanAopDescriptor for [{}]", typeDescription.getTypeName());
                    return builder;
                }

                builder = builder.defineField(IBithonObject.INJECTED_FIELD_NAME, Object.class, Opcodes.ACC_PRIVATE | Opcodes.ACC_VOLATILE)
                                 .implement(IBithonObject.class)
                                 .intercept(FieldAccessor.ofField(IBithonObject.INJECTED_FIELD_NAME));

                return install(descriptor, builder, classLoader);
            }).with(InstrumentationHelper.getAopDebugger().withTypes(new HashSet<>(descriptors.keySet())))
            .installOn(InstrumentationHelper.getInstance());
    }

    /**
     *
     * @param classLoader The class loader that's going to load the target type
     */
    private DynamicType.Builder<?> install(AopDescriptor descriptor,
                                           DynamicType.Builder<?> builder,
                                           ClassLoader classLoader) {

        int interceptorIndex = InterceptorManager.INSTANCE.getOrCreateSupplier(descriptor.interceptorName, classLoader);
        LOG.info("Dynamic interceptor installed for [{}], index={}, name={}", descriptor.targetClass, interceptorIndex, descriptor.interceptorName);
        return builder.visit(InterceptorInstaller.newInstaller(Advice.withCustomMapping()
                                                                     .bind(AdviceAnnotation.InterceptorName.class, new AdviceAnnotation.InterceptorNameResolver(descriptor.interceptorName))
                                                                     .bind(AdviceAnnotation.InterceptorIndex.class, new AdviceAnnotation.InterceptorIndexResolver(interceptorIndex))
                                                                     .to(DynamicAopAdvice.class),
                                                               descriptor.methodMatcher));
    }

    public static class AopDescriptor {
        private final String targetClass;
        private final ElementMatcher.Junction<MethodDescription> methodMatcher;
        private final String interceptorName;

        public AopDescriptor(String targetClass,
                             ElementMatcher.Junction<MethodDescription> methodMatcher,
                             String interceptorName) {
            this.targetClass = targetClass;
            this.methodMatcher = methodMatcher;
            this.interceptorName = interceptorName;
        }

        public String getTargetClass() {
            return targetClass;
        }
    }
}
