/*
 *    Copyright 2020 bithon.org
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.bithon.server.sink.metrics;

import org.bithon.server.metric.input.Measurement;
import org.bithon.server.storage.meta.EndPointType;

import java.util.HashMap;
import java.util.Map;

/**
 * ¬
 *
 * @author frank.chen021@outlook.com
 * @date 2021/4/9 20:43
 */
public class EndPointMeasurementBuilder {

    private final Map<String, Number> metrics = new HashMap<>(8);
    private final Map<String, String> dimensions = new HashMap<>(8);
    private long timestamp;

    public static EndPointMeasurementBuilder builder() {
        return new EndPointMeasurementBuilder();
    }

    public EndPointMeasurementBuilder timestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public EndPointMeasurementBuilder srcEndpointType(EndPointType srcEndpointType) {
        dimensions.put("srcEndpointType", srcEndpointType.name());
        return this;
    }

    public EndPointMeasurementBuilder srcEndpoint(String srcEndpoint) {
        dimensions.put("srcEndpoint", srcEndpoint);
        return this;
    }

    public EndPointMeasurementBuilder dstEndpointType(EndPointType dstEndpointType) {
        dimensions.put("dstEndpointType", dstEndpointType.name());
        return this;
    }

    public EndPointMeasurementBuilder dstEndpointType(String dstEndpointType) {
        dimensions.put("dstEndpointType", dstEndpointType);
        return this;
    }

    public EndPointMeasurementBuilder dstEndpoint(String dstEndpoint) {
        dimensions.put("dstEndpoint", dstEndpoint);
        return this;
    }

    public EndPointMeasurementBuilder interval(long interval) {
        metrics.put("interval", interval);
        return this;
    }

    public EndPointMeasurementBuilder callCount(long callCount) {
        metrics.put("callCount", callCount);
        return this;
    }

    public EndPointMeasurementBuilder errorCount(long errorCount) {
        metrics.put("errorCount", errorCount);
        return this;
    }

    public EndPointMeasurementBuilder responseTime(long responseTime) {
        metrics.put("responseTime", responseTime);
        return this;
    }

    public EndPointMeasurementBuilder minResponseTime(long minResponseTime) {
        metrics.put("minResponseTime", minResponseTime);
        return this;
    }

    public EndPointMeasurementBuilder maxResponseTime(long maxResponseTime) {
        metrics.put("maxResponseTime", maxResponseTime);
        return this;
    }

    public Measurement build() {
        return new Measurement(this.timestamp, this.dimensions, this.metrics);
    }
}
