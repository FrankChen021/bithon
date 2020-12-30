package com.sbss.bithon.agent.core.tracing.propagation.extractor;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/5 9:41 下午
 */
public interface PropagationGetter<REQUEST_TYPE> {

    String get(REQUEST_TYPE request, String key);
}
