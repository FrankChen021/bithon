package com.sbss.bithon.agent.core.tracing.propagation.injector;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/5 9:38 下午
 */
public interface PropagationSetter<REQUEST_TYPE> {
    void put(REQUEST_TYPE request, String key, String value);
}
