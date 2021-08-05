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

import com.sbss.bithon.agent.core.tracing.context.ITraceContext;
import com.sbss.bithon.agent.core.tracing.context.ITraceSpan;
import com.sbss.bithon.agent.core.tracing.context.SpanKind;
import com.sbss.bithon.agent.core.tracing.context.TraceContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
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
public class BeanMethodInterceptorImpl implements BeanMethodInterceptorIntf {

    private final Map<Class<?>, String> componentNames = new ConcurrentHashMap<>();

    @Override
    public Object onMethodEnter(
        final Method method,
        final Object target,
        final Object[] args
    ) {
        ITraceContext traceContext = TraceContextHolder.current();
        if (traceContext == null) {
            return null;
        }
        ITraceSpan span = traceContext.currentSpan();
        if (span == null) {
            return null;
        }

        String component = componentNames.computeIfAbsent(method.getDeclaringClass(), beanClass -> {
            if (beanClass.isAnnotationPresent(RestController.class)) {
                return "restController";
            } else if (beanClass.isAnnotationPresent(Controller.class)) {
                return "controller";
            } else if (beanClass.isAnnotationPresent(Service.class)) {
                return "springService";
            } else if (beanClass.isAnnotationPresent(Repository.class)) {
                return "springRepository";
            } else if (beanClass.isAnnotationPresent(Component.class)) {
                return "springComponent";
            } else {
                return "springBean";
            }
        });

        return span.newChildSpan(component)
                   .kind(SpanKind.CLIENT)
                   .method(method)
                   .start();
    }

    @Override
    public void onMethodExit(final Method method,
                             final Object target,
                             final Object[] args,
                             final Throwable exception,
                             final Object context) {
        ((ITraceSpan) context).tag(exception).finish();
    }
}
