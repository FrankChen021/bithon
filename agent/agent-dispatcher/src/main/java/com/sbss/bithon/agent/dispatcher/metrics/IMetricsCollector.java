package com.sbss.bithon.agent.dispatcher.metrics;

import com.sbss.bithon.agent.dispatcher.metrics.jvm.JavaInstanceMetric;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/10/27 2:17 下午
 */
public interface IMetricsCollector {

    void sendJavaInstanceMetric(JavaInstanceMetric metrics);

    void sendWebServerMetrics();

}
