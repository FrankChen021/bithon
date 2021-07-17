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

package com.sbss.bithon.agent.core.aop.interceptor;

import com.sbss.bithon.agent.bootstrap.aop.AbstractInterceptor;
import com.sbss.bithon.agent.bootstrap.aop.BootstrapConstructorAop;
import com.sbss.bithon.agent.bootstrap.aop.BootstrapMethodAop;
import com.sbss.bithon.agent.bootstrap.aop.ConstructorAop;
import com.sbss.bithon.agent.bootstrap.aop.IBithonObject;
import com.sbss.bithon.agent.bootstrap.aop.ISuperMethod;
import com.sbss.bithon.agent.bootstrap.aop.MethodAop;
import com.sbss.bithon.agent.bootstrap.expt.AgentException;
import com.sbss.bithon.agent.core.aop.AopClassGenerator;
import com.sbss.bithon.agent.core.aop.AopDebugger;
import com.sbss.bithon.agent.core.aop.descriptor.BithonClassDescriptor;
import com.sbss.bithon.agent.core.aop.descriptor.InterceptorDescriptor;
import com.sbss.bithon.agent.core.aop.descriptor.MethodPointCutDescriptor;
import com.sbss.bithon.agent.core.aop.precondition.IInterceptorPrecondition;
import com.sbss.bithon.agent.core.utils.CollectionUtils;
import shaded.net.bytebuddy.agent.builder.AgentBuilder;
import shaded.net.bytebuddy.description.type.TypeDescription;
import shaded.net.bytebuddy.dynamic.DynamicType;
import shaded.net.bytebuddy.implementation.FieldAccessor;
import shaded.net.bytebuddy.implementation.MethodDelegation;
import shaded.net.bytebuddy.implementation.SuperMethodCall;
import shaded.net.bytebuddy.implementation.bind.annotation.Morph;
import shaded.net.bytebuddy.matcher.ElementMatcher;
import shaded.net.bytebuddy.matcher.ElementMatchers;
import shaded.net.bytebuddy.utility.JavaModule;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.List;

import static shaded.net.bytebuddy.jar.asm.Opcodes.ACC_PRIVATE;
import static shaded.net.bytebuddy.jar.asm.Opcodes.ACC_VOLATILE;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/24 9:24 下午
 */
public class InterceptorInstaller {
    private static final Logger log = LoggerFactory.getLogger(InterceptorInstaller.class);

    AgentBuilder agentBuilder;
    Instrumentation inst;

    public InterceptorInstaller(AgentBuilder agentBuilder,
                                Instrumentation inst) {
        this.agentBuilder = agentBuilder;
        this.inst = inst;
    }

    public void transformToBithonClass(BithonClassDescriptor descriptor) {
        if (descriptor == null) {
            return;
        }

        agentBuilder = agentBuilder.type(descriptor.getClassMatcher())
                                   .transform((DynamicType.Builder<?> builder,
                                               TypeDescription typeDescription,
                                               ClassLoader classLoader,
                                               JavaModule javaModule) -> {
                                       if (typeDescription.isAssignableTo(IBithonObject.class)) {
                                           return builder;
                                       }

                                       builder = builder.defineField(IBithonObject.INJECTED_FIELD_NAME,
                                                                     Object.class,
                                                                     ACC_PRIVATE | ACC_VOLATILE)
                                                        .implement(IBithonObject.class)
                                                        .intercept(FieldAccessor.ofField(IBithonObject.INJECTED_FIELD_NAME));

                                       return builder;
                                   });

        if (descriptor.isDebug()) {
            agentBuilder = agentBuilder.with(AopDebugger.INSTANCE);
        }
        agentBuilder.installOn(inst);
    }

    public void installInterceptor(String providerName,
                                   InterceptorDescriptor interceptor,
                                   List<IInterceptorPrecondition> preconditions) {

        agentBuilder = agentBuilder
            // make sure the target class is not ignored by Bytebuddy's default ignore rule
            .ignore(new IgnoreExclusionMatcher(interceptor.getClassMatcher()))
            .type(interceptor.getClassMatcher())
            .transform((DynamicType.Builder<?> builder,
                        TypeDescription typeDescription,
                        ClassLoader classLoader,
                        JavaModule javaModule) -> {

                //
                // Run checkers first to see if a plugin can be installed
                //
                if (CollectionUtils.isNotEmpty(preconditions)) {
                    for (IInterceptorPrecondition condition : preconditions) {
                        if (!condition.canInstall(providerName, classLoader, typeDescription)) {
                            return null;
                        }
                    }
                }

                //
                // Transform target class to type of IBithonObject
                //
                if (!typeDescription.isAssignableTo(IBithonObject.class)) {
                    builder = builder.defineField(IBithonObject.INJECTED_FIELD_NAME,
                                                  Object.class,
                                                  ACC_PRIVATE | ACC_VOLATILE)
                                     .implement(IBithonObject.class)
                                     .intercept(FieldAccessor.ofField(IBithonObject.INJECTED_FIELD_NAME));
                }

                //
                // Install interceptor
                //
                for (MethodPointCutDescriptor pointCut : interceptor.getMethodPointCutDescriptors()) {
                    if (interceptor.isBootstrapClass()) {
                        builder = installBootstrapInterceptor(builder,
                                                              pointCut.getInterceptor(),
                                                              pointCut);
                    } else {
                        builder = installInterceptor(builder,
                                                     providerName,
                                                     pointCut.getInterceptor(),
                                                     classLoader,
                                                     pointCut);
                    }
                }
                return builder;
            });
        if (interceptor.isDebug()) {
            agentBuilder = agentBuilder.with(AopDebugger.INSTANCE);
        }

        agentBuilder.installOn(inst);
    }

    /**
     * Since methods in
     * {@link BootstrapMethodAop}
     * {@link BootstrapConstructorAop}
     * are defined as static, the interceptors must be installed as classes
     */
    private DynamicType.Builder<?> installBootstrapInterceptor(DynamicType.Builder<?> builder,
                                                               String interceptorClassName,
                                                               MethodPointCutDescriptor pointCutDescriptor) {
        try {
            switch (pointCutDescriptor.getTargetMethodType()) {
                case NON_CONSTRUCTOR:
                    builder = builder.method(pointCutDescriptor.getMethodMatcher())
                                     .intercept(MethodDelegation.withDefaultConfiguration()
                                                                .withBinders(Morph.Binder.install(ISuperMethod.class))
                                                                .to(getBootstrapAopClass(interceptorClassName)));
                    break;

                case CONSTRUCTOR:
                    builder = builder.constructor(pointCutDescriptor.getMethodMatcher())
                                     .intercept(SuperMethodCall.INSTANCE.andThen(MethodDelegation
                                                                                     .withDefaultConfiguration()
                                                                                     .to(getBootstrapAopClass(
                                                                                         interceptorClassName))));
                    break;

                default:
                    log.warn("Interceptor[{}] ignored due to unknown method type {}",
                             interceptorClassName,
                             pointCutDescriptor.getTargetMethodType().name());
                    break;
            }
        } catch (Exception e) {
            log.error(String.format("Failed to load interceptor[%s] due to [%s]",
                                    interceptorClassName,
                                    e.getMessage()),
                      e);
            return builder;
        }

        if (log.isDebugEnabled()) {
            log.debug("Interceptor[{}] loaded for target method[{}]",
                      interceptorClassName,
                      pointCutDescriptor.toString());
        }

        return builder;
    }

    /**
     * get generated AOP class that has been injected into bootstrap class loader during startup
     */
    private Class<?> getBootstrapAopClass(String methodsInterceptor) {
        try {
            return Class.forName(AopClassGenerator.bootstrapAopClass(methodsInterceptor));
        } catch (ClassNotFoundException e) {
            throw new AgentException(e.getMessage(), e);
        }
    }

    private DynamicType.Builder<?> installInterceptor(DynamicType.Builder<?> builder,
                                                      String interceptorProvider,
                                                      String interceptorName,
                                                      ClassLoader classLoader,
                                                      MethodPointCutDescriptor pointCutDescriptor) {

        AbstractInterceptor interceptor;
        try {
            interceptor = InterceptorManager.loadInterceptor(interceptorProvider,
                                                             interceptorName,
                                                             classLoader);

            if (interceptor == null) {
                log.info("Interceptor[{}] initial failed, interceptor ignored", interceptorName);
                return null;
            }
        } catch (Exception e) {
            log.error(String.format("Failed to load interceptor[%s] due to %s", interceptorName, e.getMessage()), e);
            return null;
        }

        try {
            switch (pointCutDescriptor.getTargetMethodType()) {
                case NON_CONSTRUCTOR:
                    builder = builder.method(pointCutDescriptor.getMethodMatcher())
                                     .intercept(MethodDelegation.withDefaultConfiguration()
                                                                .withBinders(Morph.Binder.install(ISuperMethod.class))
                                                                .to(new MethodAop(interceptor)));
                    break;

                case CONSTRUCTOR:
                    builder = builder.constructor(pointCutDescriptor.getMethodMatcher())
                                     .intercept(SuperMethodCall.INSTANCE
                                                    .andThen(MethodDelegation.to(new ConstructorAop(interceptor))));
                    break;

                default:
                    log.warn("Interceptor[{}] ignored due to unknown method type {}",
                             interceptorName,
                             pointCutDescriptor.getTargetMethodType().name());
                    break;
            }
        } catch (Exception e) {
            log.error("Failed to load interceptor[{}] due to {}",
                      interceptorName,
                      e.getMessage());
            return null;
        }

        if (log.isDebugEnabled()) {
            log.debug("Interceptor[{}] loaded for target method[{}]", interceptorName, pointCutDescriptor.toString());
        }

        return builder;
    }

    static class IgnoreExclusionMatcher implements AgentBuilder.RawMatcher {

        ElementMatcher<? super TypeDescription> exclusion;
        ForElementMatchers or1;
        ForElementMatchers or2;

        public IgnoreExclusionMatcher(ElementMatcher<? super TypeDescription> exclusion) {
            this.exclusion = exclusion;
            this.or1 = new AgentBuilder.RawMatcher.ForElementMatchers(ElementMatchers.any(),
                                                                      ElementMatchers.isBootstrapClassLoader());
            this.or2 = new AgentBuilder.RawMatcher.ForElementMatchers(ElementMatchers.nameStartsWith("shaded.") //shaded.net.bytebuddy.
                                                                                     .or(ElementMatchers.nameStartsWith(
                                                                                         "com.sbss.bithon.agent."))
                                                                                     .or(ElementMatchers.nameStartsWith(
                                                                                         "sun.reflect."))
                                                                                     .or(ElementMatchers.isSynthetic()));
        }

        @Override
        public boolean matches(TypeDescription typeDescription,
                               ClassLoader classLoader,
                               JavaModule javaModule,
                               Class<?> aClass,
                               ProtectionDomain protectionDomain) {
            return !exclusion.matches(typeDescription) && (or1.matches(typeDescription,
                                                                       classLoader,
                                                                       javaModule,
                                                                       aClass,
                                                                       protectionDomain) || or2.matches(typeDescription,
                                                                                                        classLoader,
                                                                                                        javaModule,
                                                                                                        aClass,
                                                                                                        protectionDomain));
        }
    }
}
