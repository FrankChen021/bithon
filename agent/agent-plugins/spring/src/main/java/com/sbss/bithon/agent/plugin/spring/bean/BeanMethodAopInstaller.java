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

import com.sbss.bithon.agent.core.aop.AopDebugger;
import com.sbss.bithon.agent.core.aop.InstrumentationHelper;
import com.sbss.bithon.agent.core.config.Configuration;
import com.sbss.bithon.agent.core.plugin.PluginConfigurationManager;
import com.sbss.bithon.agent.core.utils.bytecode.ByteCodeUtils;
import com.sbss.bithon.agent.core.utils.filter.IMatcher;
import com.sbss.bithon.agent.core.utils.filter.InCollectionMatcher;
import com.sbss.bithon.agent.core.utils.filter.StringEqualMatcher;
import com.sbss.bithon.agent.plugin.spring.SpringPlugin;
import shaded.net.bytebuddy.agent.builder.AgentBuilder;
import shaded.net.bytebuddy.asm.Advice;
import shaded.net.bytebuddy.description.field.FieldDescription;
import shaded.net.bytebuddy.description.method.MethodDescription;
import shaded.net.bytebuddy.dynamic.loading.ClassInjector;
import shaded.net.bytebuddy.matcher.ElementMatcher;
import shaded.net.bytebuddy.matcher.ElementMatchers;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/7/10 13:05
 */
public class BeanMethodAopInstaller {
    private static final Logger log = LoggerFactory.getLogger(BeanMethodAopInstaller.class);

    private static final Set<String> INSTRUMENTED = new ConcurrentSkipListSet<>();

    static BeanTransformationConfig transformationConfig;

    @Configuration(prefix = "agent.plugin.spring.bean")
    public static class BeanTransformationConfig {
        private boolean debug = false;
        private MatcherList excludedClasses = new MatcherList();
        private final MatcherList excludedMethods;
        private List<IncludedClassConfig> includedClasses = new ArrayList<>();

        public BeanTransformationConfig() {
            //exclude all methods declared in Object.class
            excludedMethods = new MatcherList();
            excludedMethods.add(new InCollectionMatcher(Stream.of(Object.class.getMethods())
                                                              .map(Method::getName)
                                                              .collect(Collectors.toList())));
        }

        public boolean isDebug() {
            return debug;
        }

        public void setDebug(boolean debug) {
            this.debug = debug;
        }

        public MatcherList getExcludedClasses() {
            return excludedClasses;
        }

        public void setExcludedClasses(MatcherList excludedClasses) {
            this.excludedClasses = excludedClasses;
        }

        public MatcherList getExcludedMethods() {
            return excludedMethods;
        }

        public List<IncludedClassConfig> getIncludedClasses() {
            return includedClasses;
        }

        public void setIncludedClasses(List<IncludedClassConfig> includedClasses) {
            this.includedClasses = includedClasses;
        }
    }

    static class IncludedClassConfig {
        private IMatcher matcher;
        private List<IMatcher> excludedMethods;

        public IncludedClassConfig() {
        }

        public IncludedClassConfig(IMatcher matcher) {
            this.matcher = matcher;
        }

        public IMatcher getMatcher() {
            return matcher;
        }

        public void setMatcher(IMatcher matcher) {
            this.matcher = matcher;
        }

        public List<IMatcher> getExcludedMethods() {
            return excludedMethods;
        }

        public void setExcludedMethods(List<IMatcher> excludedMethods) {
            this.excludedMethods = excludedMethods;
        }
    }

    public static class MatcherList extends ArrayList<IMatcher> {
        public boolean matches(String name) {
            for (IMatcher matcher : this) {
                if (matcher.matches(name)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static void initialize() {
        transformationConfig = PluginConfigurationManager.load(SpringPlugin.class)
                                                         .getConfig(BeanTransformationConfig.class);

        //
        // inject interceptor classes into bootstrap class loader to ensure this interceptor classes could be found by Adviced code which would be loaded by application class loader
        // because for any class loader, it would back to bootstrap class loader to find class first
        //
        ClassInjector.UsingUnsafe.Factory.resolve(InstrumentationHelper.getInstance())
                                         .make(null, null).injectRaw(new HashMap<String, byte[]>() {
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

    public static void install(String beanName, Object bean) {
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

        final MatcherList excludedMethods = new MatcherList();
        excludedMethods.addAll(transformationConfig.excludedMethods);

        IncludedClassConfig includedClassConfig = null;
        if (!transformationConfig.includedClasses.isEmpty()) {
            for (IncludedClassConfig includedClass : transformationConfig.includedClasses) {
                if (includedClass.matcher.matches(clazz.getName())) {
                    includedClassConfig = includedClass;
                    if (includedClassConfig.excludedMethods != null) {
                        excludedMethods.addAll(includedClassConfig.excludedMethods);
                    }
                    break;
                }
            }
            if (includedClassConfig == null) {
                return;
            }
        }
        if (includedClassConfig == null || !(includedClassConfig.matcher instanceof StringEqualMatcher)) {
            //execute excluding rules for non-exact matcher
            if (transformationConfig.excludedClasses.matches(clazz.getName())) {
                return;
            }
        }

        log.info("Setup AOP for Spring Bean class [{}]", clazz.getName());

        new AgentBuilder.Default()
            .ignore(ElementMatchers.none())
            .disableClassFormatChanges()
            .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
            .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
            .type(ElementMatchers.is(clazz))
            .transform((builder, typeDescription, classLoader, javaModule) -> {

                //
                // infer property methods
                //
                Set<String> propertyMethods = new HashSet<>();
                for (FieldDescription field : typeDescription.getDeclaredFields()) {
                    String name = field.getName();
                    char[] chr = name.toCharArray();
                    chr[0] = Character.toUpperCase(chr[0]);
                    name = new String(chr);
                    propertyMethods.add("get" + name);
                    propertyMethods.add("set" + name);
                    propertyMethods.add("is" + name);
                }

                //
                // inject on corresponding methods
                //
                return builder.visit(
                    Advice.to(BeanMethodAop.class)
                          .on(new BeanMethodModifierMatcher()
                                  .and((method -> !propertyMethods.contains(method.getName())
                                                  && !excludedMethods.matches(method.getName())))));
            })
            .with(AopDebugger.INSTANCE)
            .installOn(InstrumentationHelper.getInstance());
    }

    static class BeanMethodModifierMatcher extends ElementMatcher.Junction.AbstractBase<MethodDescription> {
        @Override
        public boolean matches(MethodDescription target) {
            return target.isPublic()
                   && !target.isConstructor()
                   && !target.isStatic()
                   && !target.isAbstract()
                   && !target.isNative();
        }
    }
}
