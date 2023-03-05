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

package org.bithon.agent.plugin.spring.bean.interceptor;

import org.bithon.agent.bootstrap.aop.advice.IAdviceInterceptor;
import org.bithon.agent.core.tracing.context.ITraceSpan;
import org.bithon.agent.core.tracing.context.TraceSpanFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NOTE:
 * Any update of class/package name of this class must be manually reflected to {@link BeanMethodInterceptorFactory#INTERCEPTOR_CLASS_NAME},
 * or the Bean interception WON'T WORK
 *
 * @author frank.chen021@outlook.com
 * @date 2021/7/10 18:46
 */
public class BeanMethod$Invoke implements IAdviceInterceptor {

    @Override
    public Object onMethodEnter(
        final Method method,
        final Object target,
        final Object[] args
    ) {
        ITraceSpan span = TraceSpanFactory.newSpan("");
        if (span == null) {
            return null;
        }

        return span.component(AnnotationHelper.getComponentName(method.getDeclaringClass()))
                   .method(method)
                   .start();
    }

    @Override
    public Object onMethodExit(final Method method,
                               final Object target,
                               final Object[] args,
                               final Object returning,
                               final Throwable exception,
                               final Object context) {
        ((ITraceSpan) context).tag(exception).finish();
        return returning;
    }

    public static class AnnotationHelper {
        private static final Map<String, String> ANNOTATION2_NAME = new HashMap<>();
        private static final Map<Class<?>, String> COMPONENT_NAMES = new ConcurrentHashMap<>();

        // Use string format class name instead of using Class to avoid ClassNotFound problem
        // when target application does not ship with spring-web
        static {
            ANNOTATION2_NAME.put("org.springframework.stereotype.Component", "spring-component");
            ANNOTATION2_NAME.put("org.springframework.stereotype.Controller", "spring-controller");
            ANNOTATION2_NAME.put("org.springframework.stereotype.Repository", "spring-repository");
            ANNOTATION2_NAME.put("org.springframework.stereotype.Service", "spring-service");
            ANNOTATION2_NAME.put("org.springframework.web.bind.annotation.RestController", "spring-controller");
        }

        public static String getOrCreateComponentName(Class<?> beanClass) {
            Annotation[] annotations = beanClass.getAnnotations();
            for (Annotation annotation : annotations) {
                String name = ANNOTATION2_NAME.get(annotation.annotationType().getName());
                if (name != null) {
                    COMPONENT_NAMES.put(beanClass, name);
                    return name;
                }
            }
            return null;
        }

        public static String getComponentName(Class<?> clazz) {
            return COMPONENT_NAMES.get(clazz);
        }
    }
}
