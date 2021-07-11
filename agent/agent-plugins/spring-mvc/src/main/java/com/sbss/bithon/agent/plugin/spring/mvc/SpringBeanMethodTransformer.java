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
import com.sbss.bithon.agent.core.plugin.debug.AopDebugger;
import com.sbss.bithon.agent.core.utils.bytecode.ByteCodeUtils;
import com.sbss.bithon.agent.core.utils.filter.IMatcher;
import com.sbss.bithon.agent.core.utils.filter.StringContainsMatcher;
import com.sbss.bithon.agent.core.utils.filter.StringPrefixMatcher;
import com.sbss.bithon.agent.core.utils.filter.StringSuffixMatcher;
import shaded.net.bytebuddy.ByteBuddy;
import shaded.net.bytebuddy.agent.builder.AgentBuilder;
import shaded.net.bytebuddy.asm.Advice;
import shaded.net.bytebuddy.description.method.MethodDescription;
import shaded.net.bytebuddy.dynamic.ClassFileLocator;
import shaded.net.bytebuddy.dynamic.DynamicType;
import shaded.net.bytebuddy.dynamic.loading.ClassInjector;
import shaded.net.bytebuddy.matcher.ElementMatcher;
import shaded.net.bytebuddy.matcher.ElementMatchers;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
    private static final Logger log = LoggerFactory.getLogger(SpringBeanMethodTransformer.class);

    private static final Set<String> INSTRUMENTED = new ConcurrentSkipListSet<>();

    private static final List<IMatcher> EXCLUDE_MATCHERS = Arrays.asList(
        new StringSuffixMatcher("Properties"),
        new StringSuffixMatcher("Configuration"),
        new StringSuffixMatcher("BeanPostProcessor"),
        new StringPrefixMatcher("org.springframework.boot.context.properties."),
        new StringPrefixMatcher("org.springframework.boot.autoconfigure."),
        new StringPrefixMatcher("org.springframework.context.annotation."),
        new StringPrefixMatcher("org.springframework.cloud.bootstrap"),
        new StringContainsMatcher(".autoconfigure.")
    );

    static class SpringBeanMethodAopClassLocator implements ClassFileLocator {
        private final String className;
        private final byte[] byteCode;
        private Class<?> aopClass;

        public String getClassName() {
            return className;
        }

        public byte[] getAopByteCode() {
            return byteCode;
        }

        public Class<?> getAopClass() {
            return aopClass;
        }

        public void setAopClass(Class<?> aopClass) {
            this.aopClass = aopClass;
        }

        public SpringBeanMethodAopClassLocator() {
            className = SpringBeanMethodAop.class.getName() + "InBootstrap";

            final DynamicType.Unloaded<?> aopClassType = new ByteBuddy().redefine(SpringBeanMethodAop.class)
                                                                        .name(className)
                                                                        .field(ElementMatchers.named(
                                                                            "interceptorClassName"))
                                                                        .value(SpringBeanMethodInterceptorImpl.class.getName())
                                                                        .make();

            AopDebugger.INSTANCE.saveClassToFile(aopClassType);

            byteCode = aopClassType.getBytes();
        }

        @Override
        public Resolution locate(String name) {
            if (name.equals(className)) {
                return new Resolution.Explicit(byteCode);
            } else {
                return new Resolution.Illegal(name);
            }
        }

        @Override
        public void close() {
        }
    }

    static SpringBeanMethodAopClassLocator aopClassLocator;

    public static void initialize() {

        aopClassLocator = new SpringBeanMethodAopClassLocator();

        //
        // inject AOP class into bootstrap class loader to ensure this Aop class could be found by Adviced code which would be loaded by application class loader
        // because for any class loader, it would back to bootstrap class loader to find class first
        //
        ClassInjector.UsingUnsafe.Factory.resolve(InstrumentationHelper.getInstance())
                                         .make(null, null).injectRaw(new HashMap() {
            {
                put(aopClassLocator.getClassName(), aopClassLocator.getAopByteCode());
                put(SpringBeanMethodInterceptorIntf.class.getName(),
                    ByteCodeUtils.getClassByteCode(SpringBeanMethodInterceptorIntf.class.getName(),
                                                   SpringBeanMethodInterceptorIntf.class.getClassLoader()));
            }
        });

        // check if class injected successfully
        try {
            aopClassLocator.setAopClass(Class.forName(aopClassLocator.getClassName()));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Injected class could not found. This is unexpected.", e);
        }
    }

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

        boolean excluded = EXCLUDE_MATCHERS.stream().anyMatch(matcher -> matcher.matches(clazz.getName()));
        if (excluded) {
            return;
        }

        log.info("Setup AOP for Spring Bean class [{}]", clazz.getName());

        //
        // For Advice class, it's not neccessary to put this class in bootstrap class loader.
        // Byte Buddy reads the byte code of this class(from a class loader) and weaves its OnEnter and OnExit into target method
        //
        // But because the implementation of this Advice class accesses a static member of this class, we have to make sure this
        // class in bootstrap class loader so that it could be found by any class loaders
        //
        // We could also extract that static member into another class and inject this new class into bootstrap class loader
        //
        AgentBuilder agentBuilder = InstrumentationHelper.getBuilder();
        agentBuilder.ignore(none())
                    .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                    .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                    .type(ElementMatchers.is(clazz), ElementMatchers.is(clazz.getClassLoader()))
                    .transform((builder, typeDescription, classLoader, javaModule) ->
                                   builder.visit(
                                       Advice.to(aopClassLocator.getAopClass(), aopClassLocator)
                                             .on(BeanMethodMatcher.INSTANCE)))
                    .with(AopDebugger.INSTANCE)
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
