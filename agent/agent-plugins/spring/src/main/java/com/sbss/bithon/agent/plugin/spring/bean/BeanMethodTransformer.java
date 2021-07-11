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
import com.sbss.bithon.agent.core.plugin.config.StaticConfig;
import com.sbss.bithon.agent.core.plugin.debug.AopDebugger;
import com.sbss.bithon.agent.core.utils.bytecode.ByteCodeUtils;
import com.sbss.bithon.agent.core.utils.filter.IMatcher;
import com.sbss.bithon.agent.core.utils.filter.InCollectionMatcher;
import com.sbss.bithon.agent.core.utils.filter.StringPrefixMatcher;
import com.sbss.bithon.agent.core.utils.filter.StringSuffixMatcher;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
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

    private static MatcherList excludingClasses;
    private static MatcherList excludingMethods;
    private static MatcherList includingClasses;
    private static MatcherList includingMethods;

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
        StaticConfig config = StaticConfig.load(SpringPlugin.class);
        excludingClasses = config.getConfig("spring.bean.excluding.classes", MatcherList.class)
                                 .orElse(new MatcherList());
        excludingMethods = config.getConfig("spring.bean.excluding.methods", MatcherList.class)
                                 .orElse(new MatcherList());
        includingClasses = config.getConfig("spring.bean.including.classes", MatcherList.class)
                                 .orElse(new MatcherList());
        includingMethods = config.getConfig("spring.bean.including.methods", MatcherList.class)
                                 .orElse(new MatcherList());

        //exclude all methods declared in Object.class
        excludingMethods.add(0, new InCollectionMatcher(Stream.of(Object.class.getMethods())
                                                              .map(Method::getName)
                                                              .collect(Collectors.toList())));

        //
        // process user specified matchers, eg: -Dspring.bean.including.classes=startwith:XXX,endwith:YYYY
        //
        Collection<IMatcher> userMatchers = Stream.of(System.getProperty("bithon.spring.bean.including.classes", "")
                                                            .split(","))
                                                  .map(String::trim)
                                                  .filter((v) -> !v.isEmpty())
                                                  .map(v -> {
                                                      String[] parts = v.split(":");
                                                      if (parts.length != 2) {
                                                          return null;
                                                      }

                                                      String matcher = parts[0].trim();
                                                      String pattern = parts[1].trim();
                                                      switch (matcher) {
                                                          case "startwith":
                                                              return new StringPrefixMatcher(pattern);
                                                          case "endwith":
                                                              return new StringSuffixMatcher(pattern);
                                                          default:
                                                              return null;
                                                      }
                                                  })
                                                  .filter(Objects::nonNull)
                                                  .collect(Collectors.toList());
        includingClasses.addAll(userMatchers);

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

        if (includingClasses.isEmpty()) {
            if (excludingClasses.matches(clazz.getName())) {
                return;
            }
        } else {
            if (!includingClasses.matches(clazz.getName())) {
                return;
            }
        }

        log.info("Setup AOP for Spring Bean class [{}]", clazz.getName());

        AgentBuilder agentBuilder = InstrumentationHelper.getBuilder();
        agentBuilder.ignore(none())
                    .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                    .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                    .type(ElementMatchers.is(clazz), ElementMatchers.is(clazz.getClassLoader()))
                    .transform((builder, typeDescription, classLoader, javaModule) -> {

                        Set<String> propertyMethods = new HashSet<>();
                        for (FieldDescription field : typeDescription.getDeclaredFields()) {
                            String name = field.getName();
                            char[] chr = name.toCharArray();
                            chr[0] = Character.toUpperCase(chr[0]);
                            name = new String(chr);
                            propertyMethods.add("get" + name);
                            propertyMethods.add("set" + name);
                        }

                        return builder.visit(
                            Advice.to(BeanMethodAdvice.class)
                                  .on(new BeanMethodModifierMatcher().and((method -> {
                                      if (includingMethods.isEmpty()) {
                                          return !excludingMethods.matches(method.getName())
                                                 && !propertyMethods.contains(method.getName());
                                      } else {
                                          return includingMethods.matches(clazz.getName());
                                      }
                                  }
                                                                          ))));
                    })
                    .with(AopDebugger.INSTANCE)
                    .installOn(InstrumentationHelper.getInstance());
    }

    static class BeanMethodModifierMatcher extends ElementMatcher.Junction.AbstractBase<MethodDescription> {
        @Override
        public boolean matches(MethodDescription target) {
            if (!target.isPublic()
                || target.isConstructor()
                || target.isStatic()
                || target.isAbstract()
                || target.isNative()) {
                return false;
            }
            return true;
        }
    }
}
