package com.sbss.bithon.agent.core.tracing.context;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/17 11:18 下午
 */
public class TraceContextHolder {
    private static final ThreadLocal<TraceContext> HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> ASYNC_HOLDER = new ThreadLocal<>();

    public static void set(TraceContext tracer) {
        HOLDER.set(tracer);
    }

    public static void remove() {
        HOLDER.set(null);
    }

    public static TraceContext get() {
        return HOLDER.get();
    }

    public static Boolean getAsy() {
        return ASYNC_HOLDER.get();
    }

    public static void setAsy(Boolean asy) {
        ASYNC_HOLDER.set(asy);
    }
}