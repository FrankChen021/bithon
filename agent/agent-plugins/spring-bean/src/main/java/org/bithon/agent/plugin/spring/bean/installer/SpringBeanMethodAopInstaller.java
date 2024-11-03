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

package org.bithon.agent.plugin.spring.bean.installer;

import org.bithon.agent.configuration.ConfigurationManager;
import org.bithon.agent.observability.aop.BeanMethodAopInstaller;
import org.bithon.agent.plugin.spring.bean.interceptor.BeanMethod$Invoke;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/7/10 13:05
 */
public class SpringBeanMethodAopInstaller {

    public static class BeanClassAnnotations {
        private static final Map<String, String> COMPONENT_NAMES = new ConcurrentHashMap<>();

        public static String getTracingComponentName(Class<?> beanClass) {
            String name = COMPONENT_NAMES.get(beanClass.getName());
            if (name != null) {
                return name;
            }
            name = computeTracingComponentName(beanClass);
            if (name == null) {
                return null;
            }
            COMPONENT_NAMES.put(beanClass.getName(), name);
            return name;
        }

        private static String computeTracingComponentName(Class<?> beanClass) {
            Annotation[] annotations = beanClass.getAnnotations();
            for (Annotation annotation : annotations) {
                String annotationType = annotation.annotationType().getName();
                switch (annotationType) {
                    case "org.springframework.stereotype.Component":
                        return "spring-component";

                    case "org.springframework.stereotype.Repository":
                        return "spring-repository";

                    case "org.springframework.stereotype.Service":
                        return "spring-service";

                    case "org.springframework.context.annotation.Configuration":
                        // No need to instrument Configuration class
                        return null;

                    case "org.springframework.stereotype.Controller":
                        // Spring Controller is instrumented in the spring-webmvc plugin by the InvocableHandlerMethod$DoInvoke interceptor
                        return null;

                    case "org.springframework.web.bind.annotation.RestController":
                        // Rest Controller is instrumented in the spring-webmvc plugin by the InvocableHandlerMethod$DoInvoke interceptor
                        return null;
                }
            }

            return "spring-bean";
        }
    }

    public static void installFor(Class<?> beanClass) {
        if (BeanClassAnnotations.getTracingComponentName(beanClass) == null) {
            return;
        }

        BeanMethodAopInstaller.BeanTransformationConfig transformationConfig = ConfigurationManager.getInstance()
                                                                                                   .getConfig("agent.plugin.spring.bean",
                                                                                                              BeanMethodAopInstaller.BeanTransformationConfig.class,
                                                                                                              true);

        BeanMethodAopInstaller.install(beanClass,
                                       BeanMethod$Invoke.class.getName(),
                                       transformationConfig);
    }
}
