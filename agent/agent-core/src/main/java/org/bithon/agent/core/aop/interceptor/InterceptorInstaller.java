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
import org.bithon.agent.bootstrap.aop.IBithonObject;
import org.bithon.agent.bootstrap.aop.IReplacementInterceptor;
import org.bithon.agent.bootstrap.aop.ISuperMethod;
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
import shaded.net.bytebuddy.description.type.TypeDescription;
import shaded.net.bytebuddy.dynamic.DynamicType;
import shaded.net.bytebuddy.implementation.FieldAccessor;
import shaded.net.bytebuddy.implementation.LoadedTypeInitializer;
import shaded.net.bytebuddy.implementation.MethodDelegation;
import shaded.net.bytebuddy.implementation.bind.annotation.Morph;
import shaded.net.bytebuddy.matcher.NameMatcher;
import shaded.net.bytebuddy.matcher.StringSetMatcher;
import shaded.net.bytebuddy.utility.JavaModule;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
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

    public static class Installer {
        private final Map<String, InterceptorStruct> interceptors = new HashMap<>();
        private final Descriptors.MethodPointCuts mp;
        private final TypeDescription typeDescription;
        private final ClassLoader classLoader;
        private DynamicType.Builder<?> builder;

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

                injectInterceptor(pointCut);
                installInterceptor(pointCut);
            }

            return builder;
        }

        private void injectInterceptor(MethodPointCutDescriptor pointCutDescriptor) {
            String interceptorClass = pointCutDescriptor.getInterceptorClassName();
            if (this.interceptors.containsKey(interceptorClass)) {
                return;
            }

            Object interceptor;
            try {
                interceptor = InterceptorManager.loadInterceptor("", interceptorClass, classLoader);
                if (interceptor == null) {
                    log.info("Interceptor[{}] initial failed, interceptor ignored", interceptorClass);
                    return;
                }
            } catch (Exception e) {
                log.error(String.format(Locale.ENGLISH,
                                        "Failed to load interceptor[%s] due to %s",
                                        interceptorClass,
                                        e.getMessage()), e);
                return;
            }

            String fieldName = "__" + getSimpleClassName(interceptorClass);
            builder = builder.defineField(fieldName, INTERCEPTOR_TYPE, ACC_PRIVATE | ACC_STATIC)
                             .initializer(new StaticFieldInitializer(fieldName, interceptor));
            this.interceptors.put(interceptorClass, new InterceptorStruct(fieldName, interceptor));
        }

        private void installInterceptor(MethodPointCutDescriptor pointCutDescriptor) {

            InterceptorStruct interceptor = this.interceptors.get(pointCutDescriptor.getInterceptorClassName());
            if (interceptor == null) {
                log.error("Failed to locate Interceptor[{}] for target class", pointCutDescriptor.getInterceptorClassName(), typeDescription.getName());
                return;
            }

            switch (pointCutDescriptor.getTargetMethodType()) {
                case NON_CONSTRUCTOR:
                    builder = builder.visit(Advice.withCustomMapping()
                                                  .bind(Interceptor.class, new StaticFieldDescription(typeDescription, interceptor.getFieldName()))
                                                  .to(BootstrapMethodAop.class)
                                                  .on(pointCutDescriptor.getMethodMatcher()));
                    break;
                case CONSTRUCTOR:
                    builder = builder.visit(Advice.withCustomMapping()
                                                  .bind(Interceptor.class, new StaticFieldDescription(typeDescription, interceptor.getFieldName()))
                                                  .to(BootstrapConstructorAop.class)
                                                  .on(pointCutDescriptor.getMethodMatcher()));
                    break;
                case REPLACEMENT:
                    if (classLoader == null) {
                        log.error("REPLACEMENT on JDK class [{}] is not allowed", typeDescription.getName());
                        return;
                    }

                    if (!(interceptor.getInterceptor() instanceof IReplacementInterceptor)) {
                        throw new AgentException("interceptor [%s] does not implement [IReplacementInterceptor]", pointCutDescriptor.getInterceptorClassName());
                    }
                    builder = builder.method(pointCutDescriptor.getMethodMatcher())
                                     .intercept(MethodDelegation.withDefaultConfiguration()
                                                                .withBinders(Morph.Binder.install(ISuperMethod.class))
                                                                .to(new ReplaceMethodAop((IReplacementInterceptor) interceptor.getInterceptor())));
                    break;

                default:
                    log.warn("Interceptor[{}] ignored due to unknown method type {}",
                             pointCutDescriptor.getInterceptorClassName(),
                             pointCutDescriptor.getTargetMethodType().name());
                    break;
            }
        }

        private String getSimpleClassName(String className) {
            int dot = className.lastIndexOf('.');
            return dot == -1 ? className : className.substring(dot + 1);
        }

        static class InterceptorStruct {
            private final String fieldName;
            private final Object interceptor;

            InterceptorStruct(String fieldName, Object value) {
                this.fieldName = fieldName;
                this.interceptor = value;
            }

            public String getFieldName() {
                return fieldName;
            }

            public Object getInterceptor() {
                return interceptor;
            }
        }
    }

    static class StaticFieldDescription extends FieldDescription.InDefinedShape.AbstractBase {
        private final TypeDescription declaringType;
        private final String fieldName;

        StaticFieldDescription(TypeDescription declaringType, String fieldName) {
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

    private static class StaticFieldInitializer implements LoadedTypeInitializer {
        private final String fieldName;
        private final Object value;

        public StaticFieldInitializer(String fieldName, Object value) {
            this.fieldName = fieldName;
            this.value = value;
        }

        @Override
        public void onLoad(Class<?> type) {
            try {
                Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(null, value);
            } catch (Exception e) {
                log.error(String.format(Locale.ENGLISH,
                                        "Failed to inject interceptor[%s] due to %s",
                                        value.getClass().getName(),
                                        e.getMessage()), e);
            }
        }

        @Override
        public boolean isAlive() {
            return true;
        }
    }
}
