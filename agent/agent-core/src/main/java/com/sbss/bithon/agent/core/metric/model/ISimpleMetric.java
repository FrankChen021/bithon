package com.sbss.bithon.agent.core.metric.model;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/25 7:28 下午
 */
public interface ISimpleMetric extends IMetric {
    long update(long value);
}
