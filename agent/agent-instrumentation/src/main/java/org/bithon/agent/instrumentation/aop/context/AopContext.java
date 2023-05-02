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

package org.bithon.agent.instrumentation.aop.context;

import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.BeforeInterceptor;

/**
 * NOTE: This class is injected into boostrap class loader,
 * it's better not use any classes out of standard JDK,
 * or there will be NoClassDefFoundError exception thrown when installing interceptors
 *
 * @author frankchen
 */
public abstract class AopContext {

    protected final Class<?> targetClass;
    protected final String method;
    protected Object target;
    private final Object[] args;
    private Object userContext;
    private Object returning;
    protected Throwable exception;

    protected long startNanoTime;
    protected long endNanoTime;
    protected long startTimestamp;
    protected long endTimestamp;

    public AopContext(Class<?> targetClass,
                      String method,
                      Object target,
                      Object[] args) {
        this.targetClass = targetClass;
        this.target = target;
        this.method = method;
        this.args = args;
        this.userContext = null;
        this.returning = null;
        this.exception = null;
        this.startTimestamp = System.currentTimeMillis();
    }

    public Class<?> getTargetClass() {
        return this.targetClass;
    }

    /**
     * The target object which intercepted method is invoked on.
     * If the intercepted method is a static method, this method returns null.
     */
    public Object getTarget() {
        return target;
    }

    @SuppressWarnings("unchecked")
    public <T> T getTargetAs() {
        return (T) target;
    }

    /**
     * Get the intercepted method
     * if the intercepted method is a constructor, instance of {@link java.lang.reflect.Constructor} is returned
     * or instance of {@link java.lang.reflect.Method} is returned
     */
    public String getMethod() {
        return method;
    }

    public Object[] getArgs() {
        return args;
    }

    @SuppressWarnings("unchecked")
    public <T> T getArgAs(int i) {
        return (T) args[i];
    }

    public Object getUserContext() {
        return userContext;
    }

    public void setUserContext(Object userContext) {
        this.userContext = userContext;
    }

    @SuppressWarnings("unchecked")
    public <T> T getUserContextAs() {
        return (T) userContext;
    }

    /**
     * the returning object of intercepted method
     * Note: only available in {@link AfterInterceptor#after(AopContext)}
     */
    public Object getReturning() {
        return returning;
    }

    /**
     * WARNING: this will change the returning object for the intercepted method
     */
    public void setReturning(Object returning) {
        this.returning = returning;
    }

    /**
     * cast the returning result returned by intercepted method
     */
    @SuppressWarnings("unchecked")
    public <T> T getReturningAs() {
        return (T) returning;
    }

    /**
     * Exception thrown by intercepted method
     * Note: only available in {@link AfterInterceptor#after(AopContext)}
     */
    public Throwable getException() {
        return exception;
    }

    public boolean hasException() {
        return exception != null;
    }

    /**
     * How long the execution of intercepted method takes in nanoseconds
     * Note: Only available in {@link AfterInterceptor#after}
     */
    public long getExecutionTime() {
        return endNanoTime - startNanoTime;
    }

    /**
     * The timestamp that after {@link BeforeInterceptor#before(AopContext)} and before the intercepted method
     */
    public long getStartTimestamp() {
        return startTimestamp;
    }

    /**
     * The timestamp that after the intercepted method and before the {@link AfterInterceptor#after(AopContext)}
     */
    public long getEndTimestamp() {
        return endTimestamp;
    }

    public <T> T getInjectedOnTargetAs() {
        //noinspection unchecked
        return (T) ((IBithonObject) this.target).getInjectedObject();
    }

    public long getStartNanoTime() {
        return startNanoTime;
    }

    public long getEndNanoTime() {
        return endNanoTime;
    }
}
