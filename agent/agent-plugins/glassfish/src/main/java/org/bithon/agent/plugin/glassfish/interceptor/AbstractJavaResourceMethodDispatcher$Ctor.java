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

package org.bithon.agent.plugin.glassfish.interceptor;

import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.AfterInterceptor;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceSpanFactory;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.utils.ReflectionUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Enhance all REST APIs that comply with JAX-RS standard implemented by glassfish under Eclipse.
 * <p>
 * Hook to ctor of {@link org.glassfish.jersey.server.model.internal.AbstractJavaResourceMethodDispatcher} to enhance the {@link java.lang.reflect.InvocationHandler}
 * The reason that we don't intercept the {@link InvocationHandler#invoke(Object, Method, Object[])} is that {@link java.lang.reflect.InvocationHandler} is an interface,
 * we need to hook on all implementations to do that. It's a little complex and less efficient.
 *
 * @author frankchen
 */
public class AbstractJavaResourceMethodDispatcher$Ctor extends AfterInterceptor {

    // TODO: check the method does not return the CompletionStage/
    @Override
    public void after(AopContext aopContext) {
        InvocationHandler rawInvoker = aopContext.getArgAs(1);

        InvocationHandler enhancedInvoker = (proxy, method, args) -> {
            ITraceSpan span = null;
            try {
                span = TraceSpanFactory.newSpan("endpoint");
            } catch (Exception ignored) {
            }
            try {
                if (span != null) {
                    span.method(method).start();
                }
                return rawInvoker.invoke(proxy, method, args);
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
            ReflectionUtils.setFieldValue(aopContext.getTarget(), "methodHandler", enhancedInvoker);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            LoggerFactory.getLogger(AbstractJavaResourceMethodDispatcher$Ctor.class).error("Unable to enhance invoker", e);
        }
    }
}
