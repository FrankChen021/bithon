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
import shaded.net.bytebuddy.matcher.ElementMatcher;
import shaded.net.bytebuddy.matcher.ElementMatchers;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
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
    private static List<InstallerTask> PENDING_TASKS = Collections.synchronizedList(new ArrayList<>());

    static {
        AgentContext.getInstance().getAppInstance().addListener(port -> {
            InstallerTask[] tasks = BeanMethodAopInstaller.PENDING_TASKS.toArray(new InstallerTask[0]);
            for (InstallerTask task : tasks) {
                task.run();
            }

            // clear the list
            BeanMethodAopInstaller.PENDING_TASKS = null;
        });
    }

    /**
     * @param toAdviceClass must be in bootstrap class loader
     */
    public static void install(Class<?> targetClass,
                               Class<?> toAdviceClass,
                               BeanTransformationConfig transformationConfig) {
        if (targetClass.isSynthetic()) {
            /*
             * eg: org.springframework.boot.actuate.autoconfigure.metrics.KafkaMetricsAutoConfiguration$$Lambda$709/829537923
             */
            return;
        }

        if (!INSTRUMENTED.add(targetClass.getName())) {
            return;
        }

        InstallerTask task = new InstallerTask(targetClass, toAdviceClass, transformationConfig);

        if (AgentContext.getInstance().getAppInstance().getPort() == 0) {
            //
            // If the target class's public method's parameters have the same class which are being instrumented
            // the instrumentation will not take effect. This might be a bug of bytebuddy or wrong use of bytebuddy's API
            //
            // Since I don't have enough time to take a look at this problem,
            // I use a workaround that by deferring the installation until the application's web service is working
            // This may not solve the problem from the root or completely, but I think such problems have been eased a lot.
            //
            PENDING_TASKS.add(task);
        } else {
            task.run();
        }
    }

    private static class InstallerTask {
        private final Class<?> targetClass;
        private final Class<?> toAdviceClass;
        private final BeanTransformationConfig transformationConfig;

        public InstallerTask(Class<?> targetClass, Class<?> toAdviceClass, BeanTransformationConfig config) {
            this.targetClass = targetClass;
            this.toAdviceClass = toAdviceClass;
            this.transformationConfig = config;
        }

        public void run() {

            final MatcherList excludedMethods = new MatcherList();
            excludedMethods.addAll(transformationConfig.excludedMethods);

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
                    return;
                }
            }
            if (includedClassConfig == null || !(includedClassConfig.matcher instanceof StringEqualMatcher)) {
                //execute excluding rules for non-exact matcher
                if (transformationConfig.excludedClasses.matches(targetClass.getName())) {
                    return;
                }
            }

            log.info("Setup AOP for class [{}]", targetClass.getName());

            new AgentBuilder.Default().ignore(ElementMatchers.none())
                                      .disableClassFormatChanges()
                                      .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                                      .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                                      .type(ElementMatchers.is(targetClass))
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
                                          return builder.visit(Advice.to(toAdviceClass)
                                                                     .on(new BeanMethodModifierMatcher().and((method -> !propertyMethods.contains(method.getName())
                                                                                                                        && !excludedMethods.matches(method.getName())))));
                                      })
                                      .with(new AopTransformationListener())
                                      .installOn(InstrumentationHelper.getInstance());
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
                return matched;
            }
            return !target.getName().startsWith("is") && !target.getName().startsWith("set") && !target.getName().startsWith("can");
        }
    }
}
