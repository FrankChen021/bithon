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

import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AroundInterceptor;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceSpanFactory;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author frank.chen021@outlook.com
 * @date 2021/7/10 18:46
 */
public class BeanMethod$Invoke extends AroundInterceptor {

    @Override
    public InterceptionDecision before(AopContext aopContext) {
        ITraceSpan span = TraceSpanFactory.newSpan("");
        if (span == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        aopContext.setUserContext(span.component(AnnotationHelper.getComponentName(aopContext.getTargetClass()))
                                      .method(aopContext.getTargetClass(), aopContext.getMethod())
                                      .start());

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void after(AopContext aopContext) {
        ITraceSpan span = aopContext.getUserContextAs();
        span.tag(aopContext.getException()).finish();
    }

    public static class AnnotationHelper {
        private static final Map<String, String> ANNOTATION2_NAME = new HashMap<>();
        private static final Map<String, String> COMPONENT_NAMES = new ConcurrentHashMap<>();

        // Use string format class name instead of using Class to avoid ClassNotFound problem
        // when target application does not ship with spring-web
        static {
            ANNOTATION2_NAME.put("org.springframework.stereotype.Component", "spring-component");
            ANNOTATION2_NAME.put("org.springframework.stereotype.Controller", "spring-controller");
            ANNOTATION2_NAME.put("org.springframework.stereotype.Repository", "spring-repository");
            ANNOTATION2_NAME.put("org.springframework.stereotype.Service", "spring-service");
            ANNOTATION2_NAME.put("org.springframework.web.bind.annotation.RestController", "spring-controller");
        }

        public static String getComponentName(Class<?> clazz) {
            return COMPONENT_NAMES.computeIfAbsent(clazz.getSimpleName(), (key) -> getComponentNameByClass(clazz));
        }

        private static String getComponentNameByClass(Class<?> beanClass) {
            Annotation[] annotations = beanClass.getAnnotations();
            for (Annotation annotation : annotations) {
                String name = ANNOTATION2_NAME.get(annotation.annotationType().getName());
                if (name != null) {
                    return name;
                }
            }

            // created by @Bean or BeanFactory
            return "spring-bean";
        }
    }
}
