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

package org.bithon.agent.core.metric.domain.mongo;

import org.bithon.agent.core.metric.model.IMetricSet;
import org.bithon.agent.core.metric.model.IMetricValueProvider;
import org.bithon.agent.core.metric.model.Max;
import org.bithon.agent.core.metric.model.Min;
import org.bithon.agent.core.metric.model.Sum;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/4 11:35 下午
 */
public class MongoDbMetrics implements IMetricSet {
    Min minResponseTime = new Min();
    Sum responseTime = new Sum();
    Max maxResponseTime = new Max();
    Sum totalCount = new Sum();
    Sum exceptionCount = new Sum();
    Sum responseBytes = new Sum();
    Sum requestBytes = new Sum();

    IMetricValueProvider[] metrics = new IMetricValueProvider[]{
        minResponseTime,
        responseTime,
        maxResponseTime,
        totalCount,
        exceptionCount,
        responseBytes,
        requestBytes
    };

    public void add(long responseTime, int exceptionCount) {
        this.totalCount.incr();
        this.responseTime.update(responseTime);
        this.maxResponseTime.update(responseTime);
        this.minResponseTime.update(responseTime);
        this.exceptionCount.update(exceptionCount);
    }

    public void addBytesIn(int bytesIn) {
        this.responseBytes.update(bytesIn);
    }

    public void addBytesOut(int bytesOut) {
        this.requestBytes.update(bytesOut);
    }

    @Override
    public IMetricValueProvider[] getMetrics() {
        return metrics;
    }
}
