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

    @Getter
    private final Map<String, String> dimensions;

    @Getter
    private final Map<String, ? extends Number> metrics;

    public String getDimension(String dimensionName) {
        return dimensions.get(dimensionName);
    }

    public Number getMetric(String metricName) {
        return metrics.get(metricName);
    }

    public Object getDimension(String name, String defaultValue) {
        return dimensions.getOrDefault(name, defaultValue);
    }

    public Number getMetric(String name, int defaultValue) {
        Number number = metrics.get(name);
        return number == null ? defaultValue : number;
    }
}
