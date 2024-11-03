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

package org.bithon.agent.observability.aop;

import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.interceptor.installer.DynamicInterceptorInstaller;
import org.bithon.agent.observability.context.AppInstance;
import org.bithon.agent.observability.utils.filter.IMatcher;
import org.bithon.agent.observability.utils.filter.InCollectionMatcher;
import org.bithon.agent.observability.utils.filter.StringEqualMatcher;
import org.bithon.shaded.net.bytebuddy.description.method.MethodDescription;
import org.bithon.shaded.net.bytebuddy.matcher.ElementMatcher;
import org.bithon.shaded.net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.Method;
import java.util.ArrayList;
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
        AppInstance.getInstance().addListener(port -> {
            if (PENDING_DESCRIPTORS != null) {
                DynamicInterceptorInstaller.getInstance().install(PENDING_DESCRIPTORS);

                // clear the reference
                BeanMethodAopInstaller.PENDING_DESCRIPTORS = null;
            }
        });
    }

    public static void install(Class<?> targetClass,
                               String interceptor,
                               BeanTransformationConfig transformationConfig) {
        if (targetClass.isPrimitive()) {
            // Some user beans are defined to be primitive type
            return;
        }
        if (targetClass.getClassLoader() == null) {
            // class in bootstrap loader
            return;
        }
        if (targetClass.isSynthetic() || targetClass.isAnonymousClass()) {
            /*
             * eg: org.springframework.boot.actuate.autoconfigure.metrics.KafkaMetricsAutoConfiguration$$Lambda$709/829537923
             */
            return;
        }

        if (!PROCESSED.add(targetClass.getName())) {
            return;
        }

        // Exclude Jackson serializer/deserializer because they might be called many times if there's a long array
        Class<?> parentClass = targetClass.getSuperclass();
        while (parentClass != null && !parentClass.getName().equals("java.lang.Object")) {
            if (parentClass.getName().endsWith("com.fasterxml.jackson.databind.JsonDeserializer")) {
                return;
            }
            if (parentClass.getName().endsWith("com.fasterxml.jackson.databind.JsonSerializer")) {
                return;
            }
            parentClass = parentClass.getSuperclass();
        }

        final MatcherList excludedMethods = new MatcherList();
        //
        // derive from the global configuration
        //
        excludedMethods.addAll(transformationConfig.getExcludedMethods());

        //
        // check if the current class is in includedClasses list
        //
        IncludedClassConfig includedClassConfig = null;
        if (!transformationConfig.getIncludedClasses().isEmpty()) {
            for (IncludedClassConfig includedClass : transformationConfig.getIncludedClasses()) {
                if (includedClass.matcher.matches(targetClass.getName())) {
                    includedClassConfig = includedClass;
                    if (includedClassConfig.excludedMethods != null) {
                        excludedMethods.addAll(includedClassConfig.excludedMethods);
                    }
                    break;
                }
            }
            if (includedClassConfig == null) {
                // if includedClasses is configured, the current class must be in this list
                return;
            }
        }

        //
        // check if the current class is in the excludedClasses(blocklist) only when it does NOT exist in the includedClasses list
        //
        if (includedClassConfig == null || !(includedClassConfig.matcher instanceof StringEqualMatcher)) {
            //execute excluding rules for non-exact matcher
            if (transformationConfig.getExcludedClasses().matches(targetClass.getName())) {
                return;
            }
        }

        DynamicInterceptorInstaller.AopDescriptor descriptor = new DynamicInterceptorInstaller.AopDescriptor(targetClass.getName(),
                                                                                                             new BeanMethodMatcher(excludedMethods),
                                                                                                             interceptor);

        if (AppInstance.getInstance().getPort() == 0) {
            //
            // For any target class's public method, if any parameter's type is being instrumented,
            // the instrumentation will not take effect. This might be a bug of bytebuddy or wrong use of byte-buddy's API
            //
            // Since I don't have enough time to take a look at this problem,
            // I use a workaround that by deferring the installation until the application's web service is working,
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
        private static final ElementMatcher<MethodDescription> IS_SETTER = ElementMatchers.isSetter();
        private static final ElementMatcher<MethodDescription> IS_GETTER = ElementMatchers.isGetter();

        private final MatcherList excludedMethods;

        BeanMethodMatcher(MatcherList excludedMethods) {
            this.excludedMethods = excludedMethods;
        }

        @Override
        public boolean matches(MethodDescription target) {
            boolean matched = target.isPublic()
                              && !target.isConstructor()
                              && !target.isStatic()
                              && !target.isAbstract()
                              && !target.isNative()
                              && !IS_GETTER.matches(target)
                              && !IS_SETTER.matches(target);

            // The exclude methods already include methods defined in Object.class and IBithonObject.class
            return matched && !excludedMethods.matches(target.getName());
        }
    }
}
