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
import org.bithon.agent.bootstrap.aop.BootstrapConstructorAop;
import org.bithon.agent.bootstrap.aop.BootstrapMethodAop;
import org.bithon.agent.bootstrap.aop.ConstructorAop;
import org.bithon.agent.bootstrap.aop.IBithonObject;
import org.bithon.agent.bootstrap.aop.IReplacementInterceptor;
import org.bithon.agent.bootstrap.aop.ISuperMethod;
import org.bithon.agent.bootstrap.aop.MethodAop;
import org.bithon.agent.bootstrap.aop.ReplaceMethodAop;
import org.bithon.agent.bootstrap.aop.bytebuddy.Interceptor;
import org.bithon.agent.bootstrap.expt.AgentException;
import org.bithon.agent.core.aop.AopDebugger;
import org.bithon.agent.core.aop.descriptor.Descriptors;
import org.bithon.agent.core.aop.descriptor.MethodPointCutDescriptor;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import shaded.net.bytebuddy.agent.builder.AgentBuilder;
import shaded.net.bytebuddy.asm.Advice;
import shaded.net.bytebuddy.description.annotation.AnnotationList;
import shaded.net.bytebuddy.description.field.FieldDescription;
import shaded.net.bytebuddy.description.modifier.Ownership;
import shaded.net.bytebuddy.description.modifier.Visibility;
import shaded.net.bytebuddy.description.type.TypeDescription;
import shaded.net.bytebuddy.dynamic.DynamicType;
import shaded.net.bytebuddy.implementation.FieldAccessor;
import shaded.net.bytebuddy.implementation.LoadedTypeInitializer;
import shaded.net.bytebuddy.implementation.MethodDelegation;
import shaded.net.bytebuddy.implementation.SuperMethodCall;
import shaded.net.bytebuddy.implementation.bind.annotation.Morph;
import shaded.net.bytebuddy.matcher.NameMatcher;
import shaded.net.bytebuddy.matcher.StringSetMatcher;
import shaded.net.bytebuddy.utility.JavaModule;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static shaded.net.bytebuddy.jar.asm.Opcodes.ACC_PRIVATE;
import static shaded.net.bytebuddy.jar.asm.Opcodes.ACC_STATIC;
import static shaded.net.bytebuddy.jar.asm.Opcodes.ACC_VOLATILE;
import static shaded.net.bytebuddy.matcher.ElementMatchers.isSynthetic;
import static shaded.net.bytebuddy.matcher.ElementMatchers.nameStartsWith;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/24 9:24 下午
 */
public class InterceptorInstaller {
    private static final ILogAdaptor log = LoggerFactory.getLogger(InterceptorInstaller.class);

    private static final TypeDescription INTERCEPTOR_TYPE = new TypeDescription.ForLoadedType(AbstractInterceptor.class);

    private final Descriptors descriptors;

    public InterceptorInstaller(Descriptors descriptors) {
        this.descriptors = descriptors;
    }

    /**
     * Since methods in {@link BootstrapMethodAop} and {@link BootstrapConstructorAop} are defined as static,
     * the interceptors must be installed as classes
     */
    private DynamicType.Builder<?> installBootstrapInterceptor(TypeDescription typeDescription,
                                                               DynamicType.Builder<?> builder,
                                                               String interceptorClassName,
                                                               MethodPointCutDescriptor pointCutDescriptor) {

        String fieldName = "__" + getSimpleClassName(interceptorClassName);
        switch (pointCutDescriptor.getTargetMethodType()) {
            case NON_CONSTRUCTOR:
                return builder.defineField(fieldName, INTERCEPTOR_TYPE, Ownership.STATIC, Visibility.PRIVATE)
                              .initializer(new InterceptorFieldInitializer(fieldName, interceptorClassName))
                              .visit(Advice.withCustomMapping()
                                           .bind(Interceptor.class, new DynamicFieldDescription(typeDescription, fieldName))
                                           .to(BootstrapMethodAop.class)
                                           .on(pointCutDescriptor.getMethodMatcher()));

            case CONSTRUCTOR:
                return builder.defineField(fieldName, INTERCEPTOR_TYPE, Ownership.STATIC, Visibility.PRIVATE)
                              .initializer(new InterceptorFieldInitializer(fieldName, interceptorClassName))
                              .visit(Advice.withCustomMapping()
                                           .bind(Interceptor.class, new DynamicFieldDescription(typeDescription, fieldName))
                                           .to(BootstrapConstructorAop.class)
                                           .on(pointCutDescriptor.getMethodMatcher()));
            case REPLACEMENT:
                log.error("REPLACEMENT on JDK class [{}] is not allowed", typeDescription.getName());
                break;

            default:
                log.warn("Interceptor[{}] ignored due to unknown method type {}", interceptorClassName, pointCutDescriptor.getTargetMethodType().name());
                break;
        }

        return builder;
    }

    private DynamicType.Builder<?> installInterceptor(DynamicType.Builder<?> builder,
                                                      String interceptorProvider,
                                                      String interceptorName,
                                                      ClassLoader classLoader,
                                                      MethodPointCutDescriptor pointCutDescriptor) {

        Object interceptor;
        try {
            interceptor = InterceptorManager.loadInterceptor(interceptorProvider, interceptorName, classLoader);

            if (interceptor == null) {
                log.info("Interceptor[{}] initial failed, interceptor ignored", interceptorName);
                return builder;
            }
        } catch (Exception e) {
            log.error(String.format(Locale.ENGLISH, "Failed to load interceptor[%s] due to %s", interceptorName, e.getMessage()), e);
            return builder;
        }

        try {
            switch (pointCutDescriptor.getTargetMethodType()) {
                case NON_CONSTRUCTOR:
                    builder = builder.method(pointCutDescriptor.getMethodMatcher())
                                     .intercept(MethodDelegation.withDefaultConfiguration()
                                                                .withBinders(Morph.Binder.install(ISuperMethod.class))
                                                                .to(new MethodAop(interceptor)));
                    break;

                case REPLACEMENT:
                    if (!(interceptor instanceof IReplacementInterceptor)) {
                        throw new AgentException("interceptor [%s] does not implement [IReplacementInterceptor]");
                    }

                    builder = builder.method(pointCutDescriptor.getMethodMatcher())
                                     .intercept(MethodDelegation.withDefaultConfiguration()
                                                                .withBinders(Morph.Binder.install(ISuperMethod.class))
                                                                .to(new ReplaceMethodAop((IReplacementInterceptor) interceptor)));
                    break;

                case CONSTRUCTOR:
                    builder = builder.constructor(pointCutDescriptor.getMethodMatcher())
                                     .intercept(SuperMethodCall.INSTANCE.andThen(MethodDelegation.to(new ConstructorAop((AbstractInterceptor) interceptor))));
                    break;

                default:
                    log.warn("Interceptor[{}] ignored due to unknown method type {}", interceptorName, pointCutDescriptor.getTargetMethodType().name());
                    break;
            }
        } catch (Exception e) {
            log.error("Failed to load interceptor[{}] due to {}", interceptorName, e.getMessage());
            return builder;
        }

        if (log.isDebugEnabled()) {
            log.debug("Interceptor[{}] loaded for target method[{}]", interceptorName, pointCutDescriptor.toString());
        }

        return builder;
    }

    private String getSimpleClassName(String className) {
        int dot = className.lastIndexOf('.');
        return dot == -1 ? className : className.substring(dot + 1);
    }

    public void installOn(AgentBuilder agentBuilder, Instrumentation inst) {
        Set<String> types = new HashSet<>(descriptors.getTypes());

        agentBuilder
            .ignore(new AgentBuilder.RawMatcher.ForElementMatchers(nameStartsWith("shaded.").or(isSynthetic())))
            .type(new NameMatcher<>(new StringSetMatcher(types)))
            .transform((DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader, JavaModule javaModule) -> {
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
                    builder = builder.defineField(IBithonObject.INJECTED_FIELD_NAME, Object.class, ACC_PRIVATE | ACC_VOLATILE)
                                     .implement(IBithonObject.class)
                                     .intercept(FieldAccessor.ofField(IBithonObject.INJECTED_FIELD_NAME));
                }

                //
                // install interceptors for current matched type
                //
                for (Descriptors.MethodPointCuts mp : descriptor.getMethodPointCuts()) {
                    builder = installInterceptor(mp, builder, typeDescription, classLoader);
                }

                return builder;
            })
            .with(new AopDebugger(types)).installOn(inst);
    }

    private DynamicType.Builder<?> installInterceptor(Descriptors.MethodPointCuts mp,
                                                      DynamicType.Builder<?> builder,
                                                      TypeDescription typeDescription,
                                                      ClassLoader classLoader) {

        //
        // Run checkers first to see if an interceptor can be installed
        //
        if (mp.getPrecondition() != null) {
            if (!mp.getPrecondition().canInstall(mp.getPlugin(), classLoader, typeDescription)) {
                return builder;
            }
        }

        //
        // Install interceptor
        //
        for (MethodPointCutDescriptor pointCut : mp.getMethodInterceptors()) {
            log.info("Install interceptor [{}#{}] to [{}#{}]",
                     mp.getPlugin(),
                     getSimpleClassName(pointCut.getInterceptor()),
                     getSimpleClassName(typeDescription.getName()),
                     pointCut);

            if (classLoader == null) {
                builder = installBootstrapInterceptor(typeDescription, builder, pointCut.getInterceptor(), pointCut);
            } else {
                builder = installInterceptor(builder, mp.getPlugin(), pointCut.getInterceptor(), classLoader, pointCut);
            }
        }
        return builder;
    }

    static class DynamicFieldDescription extends FieldDescription.InDefinedShape.AbstractBase {
        private final TypeDescription declaringType;
        private final String fieldName;

        DynamicFieldDescription(TypeDescription declaringType, String fieldName) {
            this.declaringType = declaringType;
            this.fieldName = fieldName;
        }

        @Override
        public TypeDescription getDeclaringType() {
            return declaringType;
        }

        @Override
        public TypeDescription.Generic getType() {
            return INTERCEPTOR_TYPE.asGenericType();
        }

        @Override
        public int getModifiers() {
            return ACC_PRIVATE | ACC_STATIC;
        }

        @Override
        public String getName() {
            return fieldName;
        }

        @Override
        public AnnotationList getDeclaredAnnotations() {
            return null;
        }
    }

    private static class InterceptorFieldInitializer implements LoadedTypeInitializer {
        private final String simpleClassName;
        private final String interceptorClassName;

        public InterceptorFieldInitializer(String simpleClassName, String interceptorClassName) {
            this.simpleClassName = simpleClassName;
            this.interceptorClassName = interceptorClassName;
        }

        @Override
        public void onLoad(Class<?> type) {
            try {
                Field field = type.getDeclaredField(simpleClassName);
                field.setAccessible(true);

                Object interceptor;
                try {
                    interceptor = InterceptorManager.loadInterceptor("", interceptorClassName, null);
                    if (interceptor == null) {
                        log.info("Interceptor[{}] initial failed, interceptor ignored", interceptorClassName);
                        return;
                    }
                } catch (Exception e) {
                    log.error(String.format(Locale.ENGLISH,
                                            "Failed to load interceptor[%s] due to %s",
                                            interceptorClassName,
                                            e.getMessage()), e);
                    return;
                }
                field.set(null, interceptor);

            } catch (Exception e) {
                log.error(String.format(Locale.ENGLISH,
                                        "Failed to load interceptor[%s] due to %s",
                                        interceptorClassName,
                                        e.getMessage()), e);
            }
        }

        @Override
        public boolean isAlive() {
            return true;
        }
    }
}
