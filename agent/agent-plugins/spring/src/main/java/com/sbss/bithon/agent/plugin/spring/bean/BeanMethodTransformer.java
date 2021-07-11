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

package com.sbss.bithon.agent.plugin.spring.bean;

import com.sbss.bithon.agent.core.plugin.InstrumentationHelper;
import com.sbss.bithon.agent.core.plugin.debug.AopDebugger;
import com.sbss.bithon.agent.core.utils.bytecode.ByteCodeUtils;
import com.sbss.bithon.agent.core.utils.filter.IMatcher;
import com.sbss.bithon.agent.core.utils.filter.StringContainsMatcher;
import com.sbss.bithon.agent.core.utils.filter.StringPrefixMatcher;
import com.sbss.bithon.agent.core.utils.filter.StringSuffixMatcher;
import shaded.net.bytebuddy.agent.builder.AgentBuilder;
import shaded.net.bytebuddy.asm.Advice;
import shaded.net.bytebuddy.description.method.MethodDescription;
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
public class BeanMethodTransformer {
    private static final Logger log = LoggerFactory.getLogger(BeanMethodTransformer.class);

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

    public static void initialize() {
        //
        // inject interceptor classes into bootstrap class loader to ensure this interceptor classes could be found by Adviced code which would be loaded by application class loader
        // because for any class loader, it would back to bootstrap class loader to find class first
        //
        ClassInjector.UsingUnsafe.Factory.resolve(InstrumentationHelper.getInstance())
                                         .make(null, null).injectRaw(new HashMap() {
            {
                put(BeanMethodInterceptorIntf.class.getName(),
                    ByteCodeUtils.getClassByteCode(BeanMethodInterceptorIntf.class.getName(),
                                                   BeanMethodInterceptorIntf.class.getClassLoader()));
                put(BeanMethodInterceptorFactory.class.getName(),
                    ByteCodeUtils.getClassByteCode(BeanMethodInterceptorFactory.class.getName(),
                                                   BeanMethodInterceptorFactory.class.getClassLoader()));
            }
        });
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

        AgentBuilder agentBuilder = InstrumentationHelper.getBuilder();
        agentBuilder.ignore(none())
                    .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                    .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                    .type(ElementMatchers.is(clazz), ElementMatchers.is(clazz.getClassLoader()))
                    .transform((builder, typeDescription, classLoader, javaModule) ->
                                   builder.visit(
                                       Advice.to(BeanMethodAdvice.class)
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
