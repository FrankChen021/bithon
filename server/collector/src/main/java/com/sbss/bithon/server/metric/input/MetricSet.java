package com.sbss.bithon.server.metric.input;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/4/8 22:47
 */
@AllArgsConstructor
public class MetricSet {

    @Getter
    private final long timestamp;
    private final Map<String, String> dimensions;
    private final Map<String, Number> metrics;

    public String getDimension(String dimensionName) {
        return dimensions.get(dimensionName);
    }

    public Number getMetric(String metricName) {
        return metrics.get(metricName);
    }
}
