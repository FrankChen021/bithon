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

package org.bithon.agent.plugin.apache.kafka.network.interceptor.metrics;

import org.bithon.agent.observability.metric.model.IMetricSet;
import org.bithon.agent.observability.metric.model.IMetricValueProvider;
import org.bithon.agent.observability.metric.model.Max;
import org.bithon.agent.observability.metric.model.Min;
import org.bithon.agent.observability.metric.model.Sum;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/12/4 19:58
 */
public class NetworkMetrics implements IMetricSet {
    public Sum requestCount = new Sum();
    public Min minResponseTime = new Min();
    public Sum responseTime = new Sum();
    public Max maxResponseTime = new Max();

    @Override
    public IMetricValueProvider[] getMetrics() {
        return new IMetricValueProvider[]{
            requestCount,
            minResponseTime,
            responseTime,
            maxResponseTime
        };
    }
}
