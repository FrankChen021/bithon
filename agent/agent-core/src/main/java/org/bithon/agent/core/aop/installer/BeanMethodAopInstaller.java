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

package org.bithon.agent.core.aop.installer;

import org.bithon.agent.bootstrap.aop.IBithonObject;
import org.bithon.agent.core.context.AgentContext;
import org.bithon.agent.core.utils.filter.IMatcher;
import org.bithon.agent.core.utils.filter.InCollectionMatcher;
import org.bithon.agent.core.utils.filter.StringEqualMatcher;
import org.bithon.shaded.net.bytebuddy.asm.Advice;
import org.bithon.shaded.net.bytebuddy.description.method.MethodDescription;
import org.bithon.shaded.net.bytebuddy.matcher.ElementMatcher;

import java.lang.reflect.Field;
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

    private static final Set<String> PROCESSED = new ConcurrentSkipListSet<>();
    private static Map<String, DynamicInterceptorInstaller.AopDescriptor> PENDING_DESCRIPTORS = new ConcurrentHashMap<>();

    static {
        AgentContext.getInstance().getAppInstance().addListener(port -> {
            DynamicInterceptorInstaller.getInstance().install(PENDING_DESCRIPTORS);

            // clear the reference
            BeanMethodAopInstaller.PENDING_DESCRIPTORS = null;
        });
    }

    public static void install(Class<?> targetClass, String interceptor, Advice advice, BeanTransformationConfig transformationConfig) {
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

        //
        // infer property methods
        //
        Set<String> propertyMethods = new HashSet<>();
        for (Field field : targetClass.getDeclaredFields()) {
            String name = field.getName();
            char[] chr = name.toCharArray();
            chr[0] = Character.toUpperCase(chr[0]);
            name = new String(chr);
            propertyMethods.add("get" + name);
            propertyMethods.add("set" + name);
            propertyMethods.add("is" + name);
        }

        DynamicInterceptorInstaller.AopDescriptor descriptor = new DynamicInterceptorInstaller.AopDescriptor(targetClass.getName(),
                                                                                                             interceptor,
                                                                                                             advice,
                                                                                                             new BeanMethodMatcher(propertyMethods,
                                                                                                                                   excludedMethods));

        if (AgentContext.getInstance().getAppInstance().getPort() == 0) {
            //
            // For any target class's public method, if any parameter's type is being instrumented,
            // the instrumentation will not take effect. This might be a bug of bytebuddy or wrong use of bytebuddy's API
            //
            // Since I don't have enough time to take a look at this problem,
            // I use a workaround that by deferring the installation until the application's web service is working
            // This may not solve the problem from the root or completely, but I think such problems have been eased a lot.
            //
            PENDING_DESCRIPTORS.put(descriptor.getTargetClass(), descriptor);
        } else {
            DynamicInterceptorInstaller.getInstance().installOne(descriptor);
        }
    }

    public static class BeanTransformationConfig {
        private final MatcherList excludedMethods;
        private boolean debug = false;
        private MatcherList excludedClasses = new MatcherList();
        private List<IncludedClassConfig> includedClasses = new ArrayList<>();

        public BeanTransformationConfig() {
            //exclude all methods declared in Object.class
            Set<String> methods = Stream.of(Object.class.getMethods()).map(Method::getName).collect(Collectors.toSet());

            //exclude all methods declared in IBithonObject.class
            methods.addAll(Stream.of(IBithonObject.class.getMethods()).map(Method::getName).collect(Collectors.toSet()));

            excludedMethods = new MatcherList();
            excludedMethods.add(new InCollectionMatcher(methods));
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

    static class BeanMethodMatcher extends ElementMatcher.Junction.AbstractBase<MethodDescription> {
        private final Set<String> propertyMethods;
        private final MatcherList excludedMethods;

        BeanMethodMatcher(Set<String> propertyMethods, MatcherList excludedMethods) {
            this.propertyMethods = propertyMethods;
            this.excludedMethods = excludedMethods;
        }

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

            String name = target.getName();

            matched = !name.startsWith("is") && !name.startsWith("set") && !name.startsWith("can");
            if (!matched) {
                return false;
            }

            if (propertyMethods.contains(name)) {
                return false;
            }

            if (excludedMethods.matches(name)) {
                return false;
            }

            return true;
        }
    }

}
