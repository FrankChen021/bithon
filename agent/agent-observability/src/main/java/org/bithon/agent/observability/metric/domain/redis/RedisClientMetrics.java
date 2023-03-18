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

package org.bithon.agent.observability.metric.domain.redis;

import org.bithon.agent.observability.metric.model.IMetricSet;
import org.bithon.agent.observability.metric.model.IMetricValueProvider;
import org.bithon.agent.observability.metric.model.Max;
import org.bithon.agent.observability.metric.model.Min;
import org.bithon.agent.observability.metric.model.Sum;

/**
 * @author frankchen
 */
public class RedisClientMetrics implements IMetricSet {

    private final Min minResponseTime = new Min();
    private final Sum responseTime = new Sum();
    private final Max maxResponseTime = new Max();
    private final Sum totalCount = new Sum();
    private final Sum exceptionCount = new Sum();
    private final Sum responseBytes = new Sum();
    private final Sum requestBytes = new Sum();

    private final IMetricValueProvider[] metrics = new IMetricValueProvider[]{
        minResponseTime,
        responseTime,
        maxResponseTime,
        totalCount,
        exceptionCount,
        responseBytes,
        requestBytes
    };

    public void addResponseBytes(int responseBytes) {
        this.responseBytes.update(responseBytes);
    }

    public void addRequestBytes(int requestBytes) {
        this.requestBytes.update(requestBytes);
    }

    public void addRequest(long responseTime, int exceptionCount) {
        this.responseTime.update(responseTime);
        this.minResponseTime.update(responseTime);
        this.maxResponseTime.update(responseTime);
        this.exceptionCount.update(exceptionCount);
        this.totalCount.incr();
    }

    @Override
    public IMetricValueProvider[] getMetrics() {
        return metrics;
    }
}
