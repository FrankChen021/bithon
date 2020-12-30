package com.sbss.bithon.agent.core.plugin.aop.bootstrap;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/17 11:34 下午
 */
public enum InterceptionDecision {
    CONTINUE,

    /**
     * Whether or not SKIP call of {@link AbstractInterceptor#onMethodLeave}
     * It's very useful when implement interceptors for tracing
     */
    SKIP_LEAVE
}
