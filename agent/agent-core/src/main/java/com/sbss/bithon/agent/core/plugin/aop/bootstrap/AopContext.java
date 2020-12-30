package com.sbss.bithon.agent.core.plugin.aop.bootstrap;

import java.lang.reflect.Method;

/**
 * NOTE: This class is injected into boostrap class loader,
 * it's better not use any classes out of standard JDK or there will be NoClassDefFoundError exception thrown when installing interceptors
 *
 * @author frankchen
 */
public class AopContext {

    private final Class<?> targetClass;
    private final Object target;
    private final Method method;
    private final Object[] args;
    private final long startTimestamp;
    private Object userContext;
    private Object returning;
    private Exception exception;
    private Long costTime;
    private long endTimestamp;

    public AopContext(Class<?> targetClass,
                      Object target,
                      Method method,
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
        return targetClass;
    }

    public void setUserContext(Object userContext) {
        this.userContext = userContext;
    }

    public void setCostTime(long costTime) {
        this.costTime = costTime;
    }

    public void setEndTimestamp(long timestamp) {
        this.endTimestamp = timestamp;
    }

    public void setReturning(Object returning) {
        this.returning = returning;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    /**
     * @return null for static method; non-null for instance method
     */
    public Object getTarget() {
        return target;
    }

    public <T> T castTargetAs() {
        return (T) target;
    }

    public Method getMethod() {
        return method;
    }

    public Object[] getArgs() {
        return args;
    }

    public Object getUserContext() {
        return userContext;
    }

    public <T> T castUserContextAs() {
        return (T) userContext;
    }

    public Object getReturning() {
        return returning;
    }

    /**
     * cast the returning result returned by intercepted method
     */
    public <T> T castReturningAs() {
        return (T)returning;
    }

    /**
     * exception thrown by intercepted method
     */
    public Exception getException() {
        return exception;
    }

    public boolean hasException() {
        return exception != null;
    }

    /**
     * How long the execution of intercepted method takes in nano-second
     * Only available in {@link AbstractInterceptor#onMethodLeave}
     */
    public Long getCostTime() {
        return costTime;
    }

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public long getEndTimestamp() {
        return endTimestamp;
    }
}
