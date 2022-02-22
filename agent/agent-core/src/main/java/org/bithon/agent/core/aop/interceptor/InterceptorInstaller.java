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

package org.bithon.agent.core.aop.interceptor;

import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.IBithonObject;
import org.bithon.agent.bootstrap.aop.InterceptorManager;
import org.bithon.agent.bootstrap.aop.advice.ConstructorInterceptorAdvice;
import org.bithon.agent.bootstrap.aop.advice.Interceptor;
import org.bithon.agent.bootstrap.aop.advice.MethodInterceptorAdvice;
import org.bithon.agent.bootstrap.aop.advice.StaticFieldDescription;
import org.bithon.agent.core.aop.AopDebugger;
import org.bithon.agent.core.aop.descriptor.Descriptors;
import org.bithon.agent.core.aop.descriptor.MethodPointCutDescriptor;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import shaded.net.bytebuddy.agent.builder.AgentBuilder;
import shaded.net.bytebuddy.asm.Advice;
import shaded.net.bytebuddy.description.method.MethodDescription;
import shaded.net.bytebuddy.description.type.TypeDescription;
import shaded.net.bytebuddy.dynamic.DynamicType;
import shaded.net.bytebuddy.implementation.FieldAccessor;
import shaded.net.bytebuddy.implementation.Implementation;
import shaded.net.bytebuddy.implementation.MethodCall;
import shaded.net.bytebuddy.matcher.ElementMatchers;
import shaded.net.bytebuddy.matcher.NameMatcher;
import shaded.net.bytebuddy.matcher.StringSetMatcher;
import shaded.net.bytebuddy.utility.JavaModule;
import shaded.net.bytebuddy.utility.RandomString;

import java.lang.instrument.Instrumentation;
import java.util.HashSet;
import java.util.Set;

import static shaded.net.bytebuddy.jar.asm.Opcodes.ACC_PRIVATE;
import static shaded.net.bytebuddy.jar.asm.Opcodes.ACC_STATIC;
import static shaded.net.bytebuddy.jar.asm.Opcodes.ACC_VOLATILE;
import static shaded.net.bytebuddy.matcher.ElementMatchers.isSynthetic;
import static shaded.net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static shaded.net.bytebuddy.matcher.ElementMatchers.named;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/24 9:24 下午
 */
public class InterceptorInstaller {
    // No need to define it as static since this class has very short lifecycle
    private final ILogAdaptor log = LoggerFactory.getLogger(InterceptorInstaller.class);

    private final TypeDescription INTERCEPTOR_TYPE = new TypeDescription.ForLoadedType(AbstractInterceptor.class);

    private final Descriptors descriptors;

    public InterceptorInstaller(Descriptors descriptors) {
        this.descriptors = descriptors;
    }

    private static String getSimpleClassName(String className) {
        int dot = className.lastIndexOf('.');
        return dot == -1 ? className : className.substring(dot + 1);
    }

    public void installOn(AgentBuilder agentBuilder, Instrumentation inst) {
        Set<String> types = new HashSet<>(descriptors.getTypes());

        agentBuilder
            .ignore(new AgentBuilder.RawMatcher.ForElementMatchers(nameStartsWith("shaded.").or(isSynthetic())))
            .type(new NameMatcher<>(new StringSetMatcher(types)))
            .transform((DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader, JavaModule javaModule) -> {
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

                //
                // Transform target class to type of IBithonObject
                //
                if (!typeDescription.isAssignableTo(IBithonObject.class)) {
                    // define an object field on this class to hold objects across interceptors for state sharing
                    builder = builder.defineField(IBithonObject.INJECTED_FIELD_NAME, Object.class, ACC_PRIVATE | ACC_VOLATILE)
                                     .implement(IBithonObject.class)
                                     .intercept(FieldAccessor.ofField(IBithonObject.INJECTED_FIELD_NAME));
                }

                //
                // install interceptors for current matched type
                //
                for (Descriptors.MethodPointCuts mp : descriptor.getMethodPointCuts()) {
                    builder = new Installer(builder, mp, typeDescription, classLoader).install();
                }

                return builder;
            })
            .with(new AopDebugger(types)).installOn(inst);
    }

    public class Installer {
        private final MethodDescription getInterceptorMethod;
        private final Descriptors.MethodPointCuts mp;
        private final TypeDescription typeDescription;
        private final ClassLoader classLoader;
        private DynamicType.Builder<?> builder;
        private Implementation.Composable interceptorInitializers;

        /**
         * @param classLoader can be NULL. If is NULL, it's Bootstrap class loader
         */
        public Installer(DynamicType.Builder<?> builder,
                         Descriptors.MethodPointCuts mp,
                         TypeDescription typeDescription,
                         ClassLoader classLoader) {
            this.builder = builder;
            this.mp = mp;
            this.typeDescription = typeDescription;
            this.classLoader = classLoader;

            getInterceptorMethod = new TypeDescription.ForLoadedType(InterceptorManager.class)
                .getDeclaredMethods()
                .filter(named("getInterceptor"))
                .getOnly();
        }

        public DynamicType.Builder<?> install() {
            //
            // Run checkers first to see if an interceptor can be installed
            //
            if (mp.getPrecondition() != null) {
                if (!mp.getPrecondition().canInstall(mp.getPlugin(), classLoader, typeDescription)) {
                    return builder;
                }
            }

            for (MethodPointCutDescriptor pointCut : mp.getMethodInterceptors()) {
                log.info("Install interceptor [{}#{}] to [{}#{}]",
                         mp.getPlugin(),
                         getSimpleClassName(pointCut.getInterceptorClassName()),
                         getSimpleClassName(typeDescription.getName()),
                         pointCut);
                installInterceptor(pointCut);
            }
            if (interceptorInitializers != null) {
                builder = builder.invokable(ElementMatchers.isTypeInitializer())
                                 .intercept(interceptorInitializers);
            }

            return builder;
        }

        private void installInterceptor(MethodPointCutDescriptor pointCutDescriptor) {

            String fieldName = "intcep" + getSimpleClassName(pointCutDescriptor.getInterceptorClassName()) + "_" + RandomString.make();
            builder = builder.defineField(fieldName, INTERCEPTOR_TYPE, ACC_PRIVATE | ACC_STATIC);

            MethodCall.FieldSetting interceptorFieldInitializer = MethodCall.invoke(getInterceptorMethod)
                                                                            .with(pointCutDescriptor.getInterceptorClassName())
                                                                            .with(typeDescription).setsField(named(fieldName));
            if (interceptorInitializers == null) {
                interceptorInitializers = interceptorFieldInitializer;
            } else {
                interceptorInitializers = interceptorInitializers.andThen(interceptorFieldInitializer);
            }

            switch (pointCutDescriptor.getTargetMethodType()) {
                case NON_CONSTRUCTOR:
                    builder = builder.visit(Advice.withCustomMapping()
                                                  .bind(Interceptor.class,
                                                        new StaticFieldDescription(typeDescription,
                                                                                   fieldName,
                                                                                   INTERCEPTOR_TYPE.asGenericType()))
                                                  .to(MethodInterceptorAdvice.class)
                                                  .on(pointCutDescriptor.getMethodMatcher()));
                    break;
                case CONSTRUCTOR:
                    builder = builder.visit(Advice.withCustomMapping()
                                                  .bind(Interceptor.class,
                                                        new StaticFieldDescription(typeDescription,
                                                                                   fieldName,
                                                                                   INTERCEPTOR_TYPE.asGenericType()))
                                                  .to(ConstructorInterceptorAdvice.class)
                                                  .on(pointCutDescriptor.getMethodMatcher()));
                    break;
                case REPLACEMENT:
                    if (classLoader == null) {
                        log.error("REPLACEMENT on JDK class [{}] is not allowed", typeDescription.getName());
                        return;
                    }
/*
                    if (!(interceptor.getInterceptor() instanceof IReplacementInterceptor)) {
                        throw new AgentException("interceptor [%s] does not implement [IReplacementInterceptor]", pointCutDescriptor.getInterceptorClassName());
                    }
                    builder = builder.method(pointCutDescriptor.getMethodMatcher())
                                     .intercept(MethodDelegation.withDefaultConfiguration()
                                                                .withBinders(Morph.Binder.install(ISuperMethod.class))
                                                                 .to(new ReplaceMethodAop((IReplacementInterceptor) interceptor.getInterceptor())));
 */
                    break;

                default:
                    log.warn("Interceptor[{}] ignored due to unknown method type {}",
                             pointCutDescriptor.getInterceptorClassName(),
                             pointCutDescriptor.getTargetMethodType().name());
                    break;
            }
        }
    }
}
