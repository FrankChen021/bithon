package com.sbss.bithon.server.metric.handler;

import com.sbss.bithon.component.db.dao.EndPointType;
import com.sbss.bithon.server.metric.input.MetricSet;

import java.util.HashMap;
import java.util.Map;

/**
 * ¬
 *
 * @author frank.chen021@outlook.com
 * @date 2021/4/9 20:43
 */
public class EndPointMetricSetBuilder {

    private long timestamp;
    private final Map<String, Number> metrics = new HashMap<>(8);
    private final Map<String, String> dimensions = new HashMap<>(8);

    public static EndPointMetricSetBuilder builder() {
        return new EndPointMetricSetBuilder();
    }

    public EndPointMetricSetBuilder timestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public EndPointMetricSetBuilder srcEndpointType(EndPointType srcEndpointType) {
        dimensions.put("srcEndpointType", srcEndpointType.name());
        return this;
    }

    public EndPointMetricSetBuilder srcEndpoint(String srcEndpoint) {
        dimensions.put("srcEndpoint", srcEndpoint);
        return this;
    }

    public EndPointMetricSetBuilder dstEndpointType(EndPointType dstEndpointType) {
        dimensions.put("dstEndpointType", dstEndpointType.name());
        return this;
    }

    public EndPointMetricSetBuilder dstEndpointType(String dstEndpointType) {
        dimensions.put("dstEndpointType", dstEndpointType);
        return this;
    }

    public EndPointMetricSetBuilder dstEndpoint(String dstEndpoint) {
        dimensions.put("dstEndpoint", dstEndpoint);
        return this;
    }

    public EndPointMetricSetBuilder interval(long interval) {
        metrics.put("interval", interval);
        return this;
    }

    public EndPointMetricSetBuilder callCount(long callCount) {
        metrics.put("callCount", callCount);
        return this;
    }

    public EndPointMetricSetBuilder errorCount(long errorCount) {
        metrics.put("errorCount", errorCount);
        return this;
    }

    public EndPointMetricSetBuilder responseTime(long responseTime) {
        metrics.put("responseTime", responseTime);
        return this;
    }

    public EndPointMetricSetBuilder minResponseTime(long minResponseTime) {
        metrics.put("minResponseTime", minResponseTime);
        return this;
    }

    public EndPointMetricSetBuilder maxResponseTime(long maxResponseTime) {
        metrics.put("maxResponseTime", maxResponseTime);
        return this;
    }

    public MetricSet build() {
        return new MetricSet(this.timestamp, this.dimensions, this.metrics);
    }
}
