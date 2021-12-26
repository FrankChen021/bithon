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

package org.bithon.agent.core.aop;

import org.bithon.agent.core.context.AgentContext;
import org.bithon.agent.core.utils.filter.IMatcher;
import org.bithon.agent.core.utils.filter.InCollectionMatcher;
import org.bithon.agent.core.utils.filter.StringEqualMatcher;
import shaded.net.bytebuddy.agent.builder.AgentBuilder;
import shaded.net.bytebuddy.asm.Advice;
import shaded.net.bytebuddy.description.field.FieldDescription;
import shaded.net.bytebuddy.description.method.MethodDescription;
import shaded.net.bytebuddy.description.type.TypeDescription;
import shaded.net.bytebuddy.dynamic.DynamicType;
import shaded.net.bytebuddy.matcher.ElementMatcher;
import shaded.net.bytebuddy.matcher.ElementMatchers;
import shaded.net.bytebuddy.matcher.NameMatcher;
import shaded.net.bytebuddy.matcher.StringSetMatcher;
import shaded.net.bytebuddy.utility.JavaModule;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/7/10 13:05
 */
public class BeanMethodAopInstaller {
    private static final Logger log = LoggerFactory.getLogger(BeanMethodAopInstaller.class);

    private static final Set<String> PROCESSED = new ConcurrentSkipListSet<>();
    private static Map<String, BeanAopDescriptor> PENDING_DESCRIPTORS = new ConcurrentHashMap<>();

    static {
        AgentContext.getInstance().getAppInstance().addListener(port -> {
            install(PENDING_DESCRIPTORS);

            // clear the reference
            BeanMethodAopInstaller.PENDING_DESCRIPTORS = null;
        });
    }

    /**
     * @param toAdviceClass must be in bootstrap class loader
     */
    public static void install(Class<?> targetClass, Class<?> toAdviceClass, BeanTransformationConfig transformationConfig) {
        if (targetClass.isSynthetic()) {
            /*
             * eg: org.springframework.boot.actuate.autoconfigure.metrics.KafkaMetricsAutoConfiguration$$Lambda$709/829537923
             */
            return;
        }

        if (!PROCESSED.add(targetClass.getName())) {
            return;
        }

        final MatcherList excludedMethods = new MatcherList();
        //
        // derive from the global configuration
        //
        excludedMethods.addAll(transformationConfig.excludedMethods);

        //
        // check if current class is in includedClasses list
        //
        IncludedClassConfig includedClassConfig = null;
        if (!transformationConfig.includedClasses.isEmpty()) {
            for (IncludedClassConfig includedClass : transformationConfig.includedClasses) {
                if (includedClass.matcher.matches(targetClass.getName())) {
                    includedClassConfig = includedClass;
                    if (includedClassConfig.excludedMethods != null) {
                        excludedMethods.addAll(includedClassConfig.excludedMethods);
                    }
                    break;
                }
            }
            if (includedClassConfig == null) {
                // if includedClasses is configured, current class must be in this list
                return;
            }
        }

        //
        // check if current class is in the excludedClasses(black list) only when it does NOT exist in the includedClasses list
        //
        if (includedClassConfig == null || !(includedClassConfig.matcher instanceof StringEqualMatcher)) {
            //execute excluding rules for non-exact matcher
            if (transformationConfig.excludedClasses.matches(targetClass.getName())) {
                return;
            }
        }

        BeanAopDescriptor descriptor = new BeanAopDescriptor(targetClass.getName(), toAdviceClass, excludedMethods);

        if (AgentContext.getInstance().getAppInstance().getPort() == 0) {
            //
            // For any target class's public method, if any parameter's type is being instrumented,
            // the instrumentation will not take effect. This might be a bug of bytebuddy or wrong use of bytebuddy's API
            //
            // Since I don't have enough time to take a look at this problem,
            // I use a workaround that by deferring the installation until the application's web service is working
            // This may not solve the problem from the root or completely, but I think such problems have been eased a lot.
            //
            PENDING_DESCRIPTORS.put(descriptor.targetClass, descriptor);
        } else {
            installOne(descriptor);
        }
    }

    private static void installOne(BeanAopDescriptor descriptor) {

        new AgentBuilder.Default().ignore(ElementMatchers.nameStartsWith("shaded."))
                                  .disableClassFormatChanges()
                                  .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                                  .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                                  .type(ElementMatchers.named(descriptor.targetClass))
                                  .transform((builder, typeDescription, classLoader, javaModule) -> transformBeanClass(descriptor, builder, typeDescription))
                                  .with(new AopTransformationListener())
                                  .installOn(InstrumentationHelper.getInstance());
    }

    private static void install(Map<String, BeanAopDescriptor> descriptors) {
        ElementMatcher<? super TypeDescription> typeMatcher = new NameMatcher<>(new StringSetMatcher(new HashSet<>(descriptors.keySet())));

        new AgentBuilder.Default().ignore(ElementMatchers.nameStartsWith("shaded."))
                                  .disableClassFormatChanges()
                                  .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                                  .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                                  .type(typeMatcher)
                                  .transform((DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader, JavaModule javaModule) -> {
                                      BeanAopDescriptor descriptor = descriptors.get(typeDescription.getTypeName());
                                      if (descriptor == null) {
                                          // this must be an error
                                          log.error("Can't find BeanAopDescriptor for [{}]", typeDescription.getTypeName());
                                          return builder;
                                      }

                                      return transformBeanClass(descriptor, builder, typeDescription);

                                  })
                                  .with(new AopTransformationListener())
                                  .installOn(InstrumentationHelper.getInstance());
    }

    private static DynamicType.Builder<?> transformBeanClass(BeanAopDescriptor descriptor, DynamicType.Builder<?> builder, TypeDescription typeDescription) {
        log.info("Setup AOP for Bean class [{}]", descriptor.targetClass);

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
        return builder.visit(Advice.to(descriptor.toAdviceClass)
                                   .on(new BeanMethodModifierMatcher().and((method -> !propertyMethods.contains(method.getName())
                                                                                      && !descriptor.excludedMethods.matches(method.getName())))));
    }

    private static class BeanAopDescriptor {
        private final String targetClass;
        private final Class<?> toAdviceClass;
        private final MatcherList excludedMethods;

        public BeanAopDescriptor(String targetClass, Class<?> toAdviceClass, MatcherList excludedMethods) {
            this.targetClass = targetClass;
            this.toAdviceClass = toAdviceClass;
            this.excludedMethods = excludedMethods;
        }
    }

    public static class BeanTransformationConfig {
        private final MatcherList excludedMethods;
        private boolean debug = false;
        private MatcherList excludedClasses = new MatcherList();
        private List<IncludedClassConfig> includedClasses = new ArrayList<>();

        public BeanTransformationConfig() {
            //exclude all methods declared in Object.class
            excludedMethods = new MatcherList();
            excludedMethods.add(new InCollectionMatcher(Stream.of(Object.class.getMethods()).map(Method::getName).collect(Collectors.toList())));
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

    static class BeanMethodModifierMatcher extends ElementMatcher.Junction.AbstractBase<MethodDescription> {
        @Override
        public boolean matches(MethodDescription target) {
            boolean matched = target.isPublic()
                              && !target.isConstructor()
                              && !target.isStatic()
                              && !target.isAbstract()
                              && !target.isNative();
            if (!matched) {
                return false;
            }
            return !target.getName().startsWith("is")
                   && !target.getName().startsWith("set")
                   && !target.getName().startsWith("can");
        }
    }

}
