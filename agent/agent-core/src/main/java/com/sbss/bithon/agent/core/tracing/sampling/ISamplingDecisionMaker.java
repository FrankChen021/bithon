package com.sbss.bithon.agent.core.tracing.sampling;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/9 10:33 下午
 */
public interface ISamplingDecisionMaker {
    SamplingMode decideSamplingMode(Object request);
}
