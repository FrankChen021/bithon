package com.sbss.bithon.agent.core.interceptor;

import java.lang.reflect.Method;

public class AfterJoinPoint extends BeforeJoinPoint {

    private Object context;
    private Object result;
    private Exception exception;

    public AfterJoinPoint(Object target, Method method, Object[] args, Object context, Object result,
                          Exception exception) {
        super(target, method, args);
        this.context = context;
        this.result = result;
        this.exception = exception;
    }

    public Object getContext() {
        return context;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }
}
