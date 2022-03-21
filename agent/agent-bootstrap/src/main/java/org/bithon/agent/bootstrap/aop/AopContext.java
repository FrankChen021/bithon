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

package org.bithon.agent.bootstrap.aop;

import java.lang.reflect.Executable;

/**
 * NOTE: This class is injected into boostrap class loader,
 * it's better not use any classes out of standard JDK or there will be NoClassDefFoundError exception thrown when installing interceptors
 *
 * @author frankchen
 */
public class AopContext {

    private final Object target;
    private final Executable method;
    private final Object[] args;
    private Object userContext;
    private Object returning;
    private Throwable exception;

    private long startNanoTime;
    private long endNanoTime;
    private long startTimestamp;
    private long endTimestamp;

    public AopContext(Executable method,
                      Object target,
                      Object[] args) {
        this.target = target;
        this.method = method;
        this.args = args;
        this.userContext = null;
        this.returning = null;
        this.exception = null;
        this.startTimestamp = System.currentTimeMillis();
    }

    public Class<?> getTargetClass() {
        return this.method.getDeclaringClass();
    }

    /**
     * The target object which intercepted method is invoked on
     * <p>
     * If the intercepted method is a static method, returns null, otherwise returns non-null
     */
    public Object getTarget() {
        return target;
    }

    @SuppressWarnings("unchecked")
    public <T> T castTargetAs() {
        return (T) target;
    }

    /**
     * get the intercepted method
     * if the intercepted method is a constructor, instance of {@link java.lang.reflect.Constructor} is returned
     * or instance of {@link java.lang.reflect.Method} is returned
     */
    public Executable getMethod() {
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
    public <T> T castUserContextAs() {
        return (T) userContext;
    }

    /**
     * the returning object of intercepted method
     * Note: only available in {@link AbstractInterceptor#onMethodLeave(AopContext)}
     */
    public Object getReturning() {
        return returning;
    }

    public void setReturning(Object returning) {
        this.returning = returning;
    }

    /**
     * cast the returning result returned by intercepted method
     */
    @SuppressWarnings("unchecked")
    public <T> T castReturningAs() {
        return (T) returning;
    }

    /**
     * exception thrown by intercepted method
     * Note: only available in {@link AbstractInterceptor#onMethodLeave(AopContext)}
     */
    public Throwable getException() {
        return exception;
    }

    public void setException(Throwable exception) {
        this.exception = exception;
    }

    public boolean hasException() {
        return exception != null;
    }

    /**
     * How long the execution of intercepted method takes in nanoseconds
     * Note: Only available in {@link AbstractInterceptor#onMethodLeave}
     */
    public long getCostTime() {
        return endNanoTime - startNanoTime;
    }

    /**
     * the timestamp that after {@link AbstractInterceptor#onMethodEnter(AopContext)} and before the intercepted method
     */
    public long getStartTimestamp() {
        return startTimestamp;
    }

    /**
     * the timestamp that after the intercepted method and before the {@link AbstractInterceptor#onMethodLeave(AopContext)}
     */
    public long getEndTimestamp() {
        return endTimestamp;
    }

    public void setEndTimestamp(long timestamp) {
        this.endTimestamp = timestamp;
    }

    public <T> T castInjectedOnTargetAs() {
        //noinspection unchecked
        return (T) ((IBithonObject) this.target).getInjectedObject();
    }

    public long getStartNanoTime() {
        return startNanoTime;
    }

    public long getEndNanoTime() {
        return endNanoTime;
    }

    /**
     * An internal interface. SHOULD NOT be used by users' code
     */
    public void onBeforeTargetMethodInvocation() {
        startNanoTime = System.nanoTime();
        startTimestamp = System.currentTimeMillis();
    }

    /**
     * An internal interface. SHOULD NOT be used by users' code
     */
    public void onAfterTargetMethodInvocation() {
        endNanoTime = System.nanoTime();
        endTimestamp = System.currentTimeMillis();
    }
}
