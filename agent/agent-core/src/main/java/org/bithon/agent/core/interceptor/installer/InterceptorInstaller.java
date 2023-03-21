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

import org.bithon.agent.core.interceptor.AopDebugger;
import org.bithon.agent.core.interceptor.descriptor.Descriptors;
import org.bithon.agent.core.interceptor.descriptor.MethodPointCutDescriptor;
import org.bithon.agent.core.interceptor.descriptor.MethodType;
import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.advice.AdviceAnnotation;
import org.bithon.agent.instrumentation.aop.advice.AfterAdvice;
import org.bithon.agent.instrumentation.aop.advice.AroundAdvice;
import org.bithon.agent.instrumentation.aop.advice.BeforeAdvice;
import org.bithon.agent.instrumentation.aop.advice.ConstructorAfterAdvice;
import org.bithon.agent.instrumentation.aop.advice.ReplacementAdvice;
import org.bithon.agent.instrumentation.aop.interceptor.IInterceptor;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptorManager;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.shaded.net.bytebuddy.agent.builder.AgentBuilder;
import org.bithon.shaded.net.bytebuddy.asm.Advice;
import org.bithon.shaded.net.bytebuddy.description.method.MethodDescription;
import org.bithon.shaded.net.bytebuddy.description.type.TypeDescription;
import org.bithon.shaded.net.bytebuddy.dynamic.DynamicType;
import org.bithon.shaded.net.bytebuddy.implementation.FieldAccessor;
import org.bithon.shaded.net.bytebuddy.implementation.Implementation;
import org.bithon.shaded.net.bytebuddy.implementation.MethodCall;
import org.bithon.shaded.net.bytebuddy.implementation.StubMethod;
import org.bithon.shaded.net.bytebuddy.jar.asm.Opcodes;
import org.bithon.shaded.net.bytebuddy.matcher.ElementMatchers;
import org.bithon.shaded.net.bytebuddy.matcher.NameMatcher;
import org.bithon.shaded.net.bytebuddy.matcher.StringSetMatcher;
import org.bithon.shaded.net.bytebuddy.utility.JavaModule;
import org.bithon.shaded.net.bytebuddy.utility.RandomString;

import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Set;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/24 9:24 下午
 */
public class InterceptorInstaller {
    private final Descriptors descriptors;

    public InterceptorInstaller(Descriptors descriptors) {
        this.descriptors = descriptors;
    }

    public void installOn(Instrumentation inst) {
        Set<String> types = new HashSet<>(descriptors.getTypes());

        new AgentBuilder
            .Default()
            .assureReadEdgeFromAndTo(inst, IBithonObject.class)
            .ignore(new AgentBuilder.RawMatcher.ForElementMatchers(ElementMatchers.nameStartsWith("org.bithon.shaded.").or(ElementMatchers.isSynthetic())))
            .type(new NameMatcher<>(new StringSetMatcher(types)))
            .transform((DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader, JavaModule javaModule, ProtectionDomain protectionDomain) -> {
                //
                // get interceptor def for target class
                //
                String type = typeDescription.getTypeName();
                Descriptors.Descriptor descriptor = descriptors.get(type);
                if (descriptor == null) {
                    // this must be something wrong
                    LoggerFactory.getLogger(InterceptorInstaller.class).error("Error to transform [{}] for the descriptor is not found", type);
                    return builder;
                }

                //
                // Transform target class to type of IBithonObject
                //
                if (!typeDescription.isAssignableTo(IBithonObject.class)) {
                    // define an object field on this class to hold objects across interceptors for state sharing
                    builder = builder.defineField(IBithonObject.INJECTED_FIELD_NAME, Object.class, Opcodes.ACC_PRIVATE | Opcodes.ACC_VOLATILE)
                                     .implement(IBithonObject.class)
                                     .intercept(FieldAccessor.ofField(IBithonObject.INJECTED_FIELD_NAME));
                }

                //
                // install interceptors for current matched type
                //
                for (Descriptors.MethodPointCuts mp : descriptor.getMethodPointCuts()) {
                    //
                    // Run checkers first to see if an interceptor can be installed
                    //
                    if (mp.getPrecondition() != null) {
                        if (!mp.getPrecondition().canInstall(mp.getPlugin(), classLoader, typeDescription)) {
                            return builder;
                        }
                    }

                    builder = new Installer(builder,
                                            typeDescription,
                                            classLoader).install(mp.getPlugin(), mp.getMethodInterceptors());
                }

                return builder;
            })
            .with(new AopDebugger(types)).installOn(inst);
    }

    public static class Installer {
        private final MethodDescription getInterceptorMethod;
        private final TypeDescription typeDescription;
        private final ClassLoader classLoader;
        private DynamicType.Builder<?> builder;
        private Implementation.Composable interceptorInitializers;

        private final TypeDescription interceptorTypeDescription = new TypeDescription.ForLoadedType(IInterceptor.class);
        private final ILogAdaptor log = LoggerFactory.getLogger(InterceptorInstaller.class);

        /**
         * @param classLoader can be NULL. If is NULL, it's Bootstrap class loader
         */
        public Installer(DynamicType.Builder<?> builder,
                         TypeDescription typeDescription,
                         ClassLoader classLoader) {
            this.builder = builder;
            this.typeDescription = typeDescription;
            this.classLoader = classLoader;

            getInterceptorMethod = new TypeDescription.ForLoadedType(InterceptorManager.class)
                .getDeclaredMethods()
                .filter(ElementMatchers.named("getInterceptor"))
                .getOnly();
        }

        public DynamicType.Builder<?> install(String providerName, MethodPointCutDescriptor... mps) {
            for (MethodPointCutDescriptor pointCut : mps) {
                log.info("Install interceptor [{}#{}] to [{}#{}]",
                         providerName,
                         StringUtils.getSimpleClassName(pointCut.getInterceptorClassName()),
                         StringUtils.getSimpleClassName(typeDescription.getName()),
                         pointCut);
                install(pointCut);
            }

            // create initializers for all injected interceptor fields
            if (interceptorInitializers != null) {
                builder = builder.invokable(ElementMatchers.isTypeInitializer())
                                 .intercept(interceptorInitializers);
            }

            return builder;
        }

        private void install(MethodPointCutDescriptor pointCutDescriptor) {
            // Add a field to hold the interceptor object
            String fieldName = "intcep" + StringUtils.getSimpleClassName(pointCutDescriptor.getInterceptorClassName()) + "_" + RandomString.make();
            builder = builder.defineField(fieldName, interceptorTypeDescription, Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC);

            // generate assignment to the static field
            MethodCall.FieldSetting interceptorFieldInitializer = MethodCall.invoke(getInterceptorMethod)
                                                                            // 1st argument
                                                                            .with(pointCutDescriptor.getInterceptorClassName())
                                                                            // 2nd argument
                                                                            .with(typeDescription)
                                                                            // assignment
                                                                            .setsField(ElementMatchers.named(fieldName));
            if (interceptorInitializers == null) {
                interceptorInitializers = interceptorFieldInitializer;
            } else {
                // Chain initializers for multiple interceptors in one target class together
                interceptorInitializers = interceptorInitializers.andThen(interceptorFieldInitializer);
            }

            switch (pointCutDescriptor.getInterceptorType()) {
                case BEFORE:
                    builder = builder.visit(Advice.withCustomMapping()
                                                  .bind(AdviceAnnotation.Interceptor.class,
                                                        new AdviceAnnotation.InterceptorResolver(typeDescription, fieldName))
                                                  .bind(AdviceAnnotation.TargetMethod.class, new AdviceAnnotation.TargetMethodResolver())
                                                  .to(BeforeAdvice.class)
                                                  .on(pointCutDescriptor.getMethodMatcher()));
                    break;
                case AFTER:
                    Class<?> adviceClazz = pointCutDescriptor.getMethodType() == MethodType.NON_CONSTRUCTOR ?
                                           AfterAdvice.class : ConstructorAfterAdvice.class;

                    builder = builder.visit(Advice.withCustomMapping()
                                                  .bind(AdviceAnnotation.Interceptor.class,
                                                        new AdviceAnnotation.InterceptorResolver(typeDescription, fieldName))
                                                  .bind(AdviceAnnotation.TargetMethod.class, new AdviceAnnotation.TargetMethodResolver())
                                                  .to(adviceClazz)
                                                  .on(pointCutDescriptor.getMethodMatcher()));
                    break;
                case AROUND:
                    builder = builder.visit(Advice.withCustomMapping()
                                                  .bind(AdviceAnnotation.Interceptor.class,
                                                        new AdviceAnnotation.InterceptorResolver(typeDescription, fieldName))
                                                  .bind(AdviceAnnotation.TargetMethod.class, new AdviceAnnotation.TargetMethodResolver())
                                                  .to(AroundAdvice.class)
                                                  .on(pointCutDescriptor.getMethodMatcher()));
                    break;
                case REPLACEMENT:
                    if (classLoader == null) {
                        log.error("REPLACEMENT on JDK class [{}] is not allowed", typeDescription.getName());
                        return;
                    }
                    builder = builder.method(pointCutDescriptor.getMethodMatcher())
                                     .intercept(Advice.withCustomMapping()
                                                      .bind(AdviceAnnotation.Interceptor.class,
                                                            new AdviceAnnotation.InterceptorResolver(typeDescription, fieldName))
                                                      .to(ReplacementAdvice.class).wrap(StubMethod.INSTANCE));
                    break;

                default:
                    log.warn("Interceptor[{}] ignored due to unknown method type {}",
                             pointCutDescriptor.getInterceptorClassName(),
                             pointCutDescriptor.getInterceptorType().name());
                    break;
            }
        }
    }
}
