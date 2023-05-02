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

package org.bithon.agent.plugin.jersey.interceptor;

import com.sun.jersey.spi.container.JavaMethodInvoker;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceSpanFactory;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.utils.ReflectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Enhance all REST APIs that comply with the JAX-RS standard implemented by Sun.
 * <p>
 * Hook to ctor of {@link com.sun.jersey.server.impl.model.method.dispatch.ResourceJavaMethodDispatcher} to enhance the {@link JavaMethodInvoker}
 * The reason that we don't intercept the {@link JavaMethodInvoker#invoke(Method, Object, Object...)} is that {@link JavaMethodInvoker} is an interface,
 * we need to hook on all implementations to do that. It's a little complex and less efficient.
 *
 * @author frankchen
 */
public class ResourceJavaMethodDispatcher$Ctor extends AfterInterceptor {

    @Override
    public void after(AopContext aopContext) {
        JavaMethodInvoker rawInvoker = aopContext.getArgAs(1);

        JavaMethodInvoker enhancedInvoker = (m, o, parameters) -> {
            ITraceSpan span = null;
            try {
                span = TraceSpanFactory.newSpan("jersey");
            } catch (Exception ignored) {
            }
            try {
                if (span != null) {
                    span.method(m.getDeclaringClass(), m.getName())
                        .start();
                }
                return rawInvoker.invoke(m, o, parameters);
            } catch (Exception e) {
                if (span != null) {
                    span.tag(e instanceof InvocationTargetException ? ((InvocationTargetException) e).getTargetException() : e);
                }
                throw e;
            } finally {
                if (span != null) {
                    span.finish();
                }
            }
        };

        try {
            ReflectionUtils.setFieldValue(aopContext.getTarget(), "invoker", enhancedInvoker);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            LoggerFactory.getLogger(ResourceJavaMethodDispatcher$Ctor.class).error("Unable to enhance invoker", e);
        }
    }
}
