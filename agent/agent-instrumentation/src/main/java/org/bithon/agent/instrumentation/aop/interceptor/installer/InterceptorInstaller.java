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
import org.bithon.agent.instrumentation.aop.advice.AfterAdvice;
import org.bithon.agent.instrumentation.aop.advice.AroundAdvice;
import org.bithon.agent.instrumentation.aop.advice.AroundConstructorAdvice;
import org.bithon.agent.instrumentation.aop.advice.BeforeAdvice;
import org.bithon.agent.instrumentation.aop.advice.ConstructorAfterAdvice;
import org.bithon.agent.instrumentation.aop.advice.ReplacementAdvice;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptorManager;
import org.bithon.agent.instrumentation.aop.interceptor.descriptor.Descriptors;
import org.bithon.agent.instrumentation.aop.interceptor.descriptor.MethodPointCutDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.descriptor.MethodType;
import org.bithon.agent.instrumentation.logging.ILogger;
import org.bithon.agent.instrumentation.logging.LoggerFactory;
import org.bithon.shaded.net.bytebuddy.agent.builder.AgentBuilder;
import org.bithon.shaded.net.bytebuddy.asm.Advice;
import org.bithon.shaded.net.bytebuddy.asm.AsmVisitorWrapper;
import org.bithon.shaded.net.bytebuddy.description.method.MethodDescription;
import org.bithon.shaded.net.bytebuddy.description.type.TypeDescription;
import org.bithon.shaded.net.bytebuddy.dynamic.DynamicType;
import org.bithon.shaded.net.bytebuddy.implementation.FieldAccessor;
import org.bithon.shaded.net.bytebuddy.implementation.StubMethod;
import org.bithon.shaded.net.bytebuddy.jar.asm.Opcodes;
import org.bithon.shaded.net.bytebuddy.matcher.ElementMatcher;
import org.bithon.shaded.net.bytebuddy.matcher.ElementMatchers;
import org.bithon.shaded.net.bytebuddy.utility.JavaModule;

import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Set;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/24 9:24 下午
 */
public class InterceptorInstaller {

    private final ILogger log = LoggerFactory.getLogger(InterceptorInstaller.class);
    private final Descriptors descriptors;

    public InterceptorInstaller(Descriptors descriptors) {
        this.descriptors = descriptors;
    }

    public void installOn(Instrumentation inst) {
        final Set<String> types = new HashSet<>(descriptors.getTypes());

        AgentBuilder agentBuilder = new AgentBuilder.Default()
            .assureReadEdgeFromAndTo(inst, IBithonObject.class)
            .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
            .ignore(new AgentBuilder.RawMatcher.ForElementMatchers(ElementMatchers.nameStartsWith("org.bithon.shaded.net.bytebuddy.").or(ElementMatchers.isSynthetic())))
            .type(target -> types.contains(target.getActualName()))
            .transform((DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader, JavaModule javaModule, ProtectionDomain protectionDomain) -> {
                //
                // get interceptor def for target class
                //
                String type = typeDescription.getTypeName();
                Descriptors.Descriptor descriptor = descriptors.get(type);
                if (descriptor == null) {
                    // this must be something wrong
                    log.error("Error to transform [{}] for the descriptor is not found", type);
                    return builder;
                }

                if (descriptor.getPrecondition() != null
                    && !descriptor.getPrecondition().matches(classLoader, typeDescription)) {
                    log.info("Interceptor for class [{}] not installed because precondition [{}] not satisfied",
                             typeDescription.getName(),
                             descriptor.getPrecondition().toString());
                    return builder;
                }

                //
                // Transform target class to a type of IBithonObject
                //
                if (typeDescription.isInterface()) {
                    log.warn("Attempt to install interceptors on interface [{}]. This is not supported.", typeDescription.getName());
                    return builder;
                } else if (!typeDescription.isAssignableTo(IBithonObject.class)) {
                    // define an object field on this class to hold objects across interceptors for state sharing
                    builder = builder.defineField(IBithonObject.INJECTED_FIELD_NAME, Object.class, Opcodes.ACC_PRIVATE | Opcodes.ACC_VOLATILE)
                                     .implement(IBithonObject.class)
                                     .intercept(FieldAccessor.ofField(IBithonObject.INJECTED_FIELD_NAME));
                }

                //
                // install interceptors for the current matched type
                //
                for (Descriptors.MethodPointCuts mp : descriptor.getMethodPointCuts()) {
                    // Run checkers first to see if an interceptor can be installed
                    if (mp.getPrecondition() != null && !mp.getPrecondition().matches(classLoader, typeDescription)) {
                        log.info("[{}] Interceptor for class [{}] not installed because precondition [{}] not satisfied",
                                 mp.getPlugin(),
                                 typeDescription.getName(),
                                 mp.getPrecondition().toString());
                        continue;
                    }

                    builder = new Installer(builder,
                                            typeDescription,
                                            classLoader,
                                            log).install(mp.getPlugin(), mp.getMethodInterceptors());
                }

                return builder;
            });

        // Under UT mode, the debugger might be NULL
        if (InstrumentationHelper.getAopDebugger() != null) {
            // Listener is always installed to catch ERRORS even if the debugger is not enabled
            agentBuilder = agentBuilder.with(InstrumentationHelper.getAopDebugger().withTypes(types));
        }

        agentBuilder.installOn(inst);
    }

    public static class Installer {
        private final TypeDescription typeDescription;
        private final ClassLoader classLoader;
        private DynamicType.Builder<?> builder;

        private final ILogger log;

        /**
         * @param classLoader can be NULL. If is NULL, it's a Bootstrap class loader
         */
        public Installer(DynamicType.Builder<?> builder,
                         TypeDescription typeDescription,
                         ClassLoader classLoader,
                         ILogger log) {
            this.builder = builder;
            this.typeDescription = typeDescription;
            this.classLoader = classLoader;
            this.log = log;
        }

        public DynamicType.Builder<?> install(String providerName, MethodPointCutDescriptor... mps) {
            for (MethodPointCutDescriptor pointCut : mps) {
                log.info("Install interceptor [{}#{}] to [{}#{}]",
                         providerName,
                         getSimpleClassName(pointCut.getInterceptorClassName()),
                         getSimpleClassName(typeDescription.getName()),
                         pointCut);
                install(pointCut);
            }

            return builder;
        }

        private void install(MethodPointCutDescriptor descriptor) {
            if (descriptor.getInterceptorType() == null) {
                log.error("Interceptor [{}] not installed due to interceptor type is null.", descriptor.getInterceptorClassName());
                return;
            }

            int supplierIndex = InterceptorManager.INSTANCE.getOrCreateSupplier(descriptor.getInterceptorClassName(), classLoader);
            AdviceAnnotation.InterceptorNameResolver nameResolver = new AdviceAnnotation.InterceptorNameResolver(supplierIndex, descriptor.getInterceptorClassName());
            AdviceAnnotation.InterceptorIndexResolver indexResolver = new AdviceAnnotation.InterceptorIndexResolver(supplierIndex);

            switch (descriptor.getInterceptorType()) {
                case BEFORE:
                    builder = builder.visit(newInstaller(Advice.withCustomMapping()
                                                               .bind(AdviceAnnotation.InterceptorName.class, nameResolver)
                                                               .bind(AdviceAnnotation.InterceptorIndex.class, indexResolver)
                                                               .to(BeforeAdvice.class),
                                                         descriptor.getMethodMatcher()));
                    break;
                case AFTER: {
                    Class<?> adviceClazz = descriptor.getMethodType() == MethodType.NON_CONSTRUCTOR ? AfterAdvice.class : ConstructorAfterAdvice.class;

                    builder = builder.visit(newInstaller(Advice.withCustomMapping()
                                                               .bind(AdviceAnnotation.InterceptorName.class, nameResolver)
                                                               .bind(AdviceAnnotation.InterceptorIndex.class, indexResolver)
                                                               .to(adviceClazz),
                                                         descriptor.getMethodMatcher()));
                }
                break;

                case AROUND: {
                    Class<?> adviceClazz = descriptor.getMethodType() == MethodType.NON_CONSTRUCTOR ? AroundAdvice.class : AroundConstructorAdvice.class;

                    builder = builder.visit(newInstaller(Advice.withCustomMapping()
                                                               .bind(AdviceAnnotation.InterceptorName.class, nameResolver)
                                                               .bind(AdviceAnnotation.InterceptorIndex.class, indexResolver)
                                                               .to(adviceClazz),
                                                         descriptor.getMethodMatcher()));
                }
                break;

                case REPLACEMENT:
                    if (classLoader == null) {
                        log.error("REPLACEMENT on JDK class [{}] is not allowed", typeDescription.getName());
                        return;
                    }
                    builder = builder.method(descriptor.getMethodMatcher())
                                     .intercept(Advice.withCustomMapping()
                                                      .bind(AdviceAnnotation.InterceptorName.class, nameResolver)
                                                      .bind(AdviceAnnotation.InterceptorIndex.class, indexResolver)
                                                      .to(ReplacementAdvice.class)
                                                      .wrap(StubMethod.INSTANCE));
                    break;

                default:
                    log.warn("Interceptor[{}] ignored due to unknown method type {}",
                             descriptor.getInterceptorClassName(),
                             descriptor.getInterceptorType().name());
                    break;
            }
        }
    }

    private static String getSimpleClassName(String qualifiedClassName) {
        int dot = qualifiedClassName.lastIndexOf('.');
        return dot == -1 ? qualifiedClassName : qualifiedClassName.substring(dot + 1);
    }

    public static AsmVisitorWrapper newInstaller(Advice advice, ElementMatcher<? super MethodDescription> matcher) {
        return advice.on(matcher);
    }
}
