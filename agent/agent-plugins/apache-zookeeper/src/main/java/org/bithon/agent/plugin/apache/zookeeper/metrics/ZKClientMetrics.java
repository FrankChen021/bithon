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

package org.bithon.agent.plugin.apache.zookeeper.metrics;

import org.bithon.agent.observability.metric.model.IMetricSet;
import org.bithon.agent.observability.metric.model.IMetricValueProvider;
import org.bithon.agent.observability.metric.model.Max;
import org.bithon.agent.observability.metric.model.Min;
import org.bithon.agent.observability.metric.model.Sum;

/**
 * @author frank.chen021@outlook.com
 * @date 15/1/25 4:33 pm
 */
public class ZKClientMetrics implements IMetricSet {
    private final Min minResponseTime = new Min();
    private final Sum responseTime = new Sum();
    private final Max maxResponseTime = new Max();

    private final Sum totalCount = new Sum();
    private final Sum bytesReceived = new Sum();
    private final Sum bytesSent = new Sum();

    public void add(long responseTime, int bytesReceived, int bytesSent) {
        minResponseTime.update(responseTime);
        this.responseTime.update(responseTime);
        maxResponseTime.update(responseTime);
        this.bytesReceived.update(bytesReceived);
        this.bytesSent.update(bytesSent);
        this.totalCount.update(1);
    }

    @Override
    public IMetricValueProvider[] getMetrics() {
        return new IMetricValueProvider[]{
            minResponseTime,
            responseTime,
            maxResponseTime,
            totalCount,
            bytesReceived,
            bytesSent
        };
    }
}
