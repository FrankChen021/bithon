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

package org.bithon.agent.instrumentation.aop.interceptor;

import org.bithon.agent.instrumentation.aop.interceptor.declaration.AbstractInterceptor;
import org.bithon.agent.instrumentation.loader.InterceptorClassLoaderManager;
import org.bithon.agent.instrumentation.logging.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/4/21 22:07
 */
public class InterceptorSupplier implements Supplier<AbstractInterceptor> {
    private String interceptorClassName;
    private ClassLoader classLoader;

    private volatile AbstractInterceptor interceptor;

    /**
     * The exception that the interceptor fails to load
     */
    private String exception;

    /**
     * The index of this supplier in the {@link InterceptorManager}
     */
    private final int index;

    public InterceptorSupplier(int index,
                               String interceptorClassName,
                               ClassLoader classLoader) {
        this.index = index;
        this.interceptorClassName = interceptorClassName;
        this.classLoader = classLoader;
    }

    public int getIndex() {
        return index;
    }

    /**
     * The return can be NULL
     */
    @Override
    public AbstractInterceptor get() {
        if (interceptor != null || exception != null) {
            return interceptor;
        }

        synchronized (this) {
            // Double check
            if (interceptor != null) {
                return interceptor;
            }

            interceptor = createInterceptor(interceptorClassName, classLoader);
            if (interceptor != null) {
                // Release reference so that GC works
                this.classLoader = null;
                this.interceptorClassName = null;
            }
            return interceptor;
        }
    }

    private AbstractInterceptor createInterceptor(String interceptorClassName, ClassLoader userClassLoader) {
        try {
            // Load class out of lock in case of deadlock
            ClassLoader interceptorClassLoader = InterceptorClassLoaderManager.getClassLoader(userClassLoader);
            Class<?> interceptorClass = Class.forName(interceptorClassName, true, interceptorClassLoader);

            return (AbstractInterceptor) interceptorClass.getConstructor().newInstance();
        } catch (Throwable e) {
            this.exception = getStackTrace(e);

            LoggerFactory.getLogger(InterceptorManager.class).error(String.format(Locale.ENGLISH,
                    "Failed to load interceptor[%s] due to %s",
                    interceptorClassName,
                    e.getMessage()), e);
            return null;
        }
    }

    private static String getStackTrace(Throwable throwable) {
        StringWriter stackTrace = new StringWriter(256);
        while (throwable != null) {
            try (PrintWriter pw = new PrintWriter(stackTrace)) {
                pw.format(Locale.ENGLISH, "%s:%s%n", throwable.getClass().getName(), throwable.getMessage());
                throwable.printStackTrace(pw);
            }
            throwable = throwable.getCause();
        }
        return stackTrace.toString();
    }

    public String getException() {
        return exception;
    }
}
