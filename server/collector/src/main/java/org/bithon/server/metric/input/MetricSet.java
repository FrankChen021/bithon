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

package org.bithon.server.metric.input;

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
