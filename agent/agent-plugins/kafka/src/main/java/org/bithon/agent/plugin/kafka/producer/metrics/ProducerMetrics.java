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

package org.bithon.agent.plugin.kafka.producer.metrics;

import org.bithon.agent.core.metric.model.IMetricSet;
import org.bithon.agent.core.metric.model.IMetricValueProvider;
import org.bithon.agent.core.metric.model.Max;
import org.bithon.agent.core.metric.model.Min;
import org.bithon.agent.core.metric.model.Sum;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/12/3 16:24
 */
public class ProducerMetrics implements IMetricSet {

    /**
     * size of a batch
     */
    public Sum batchSize = new Sum();

    /**
     * max record size in a batch
     */
    public Sum maxRecordSize = new Sum();

    /**
     * record count in a batch
     */
    public Sum recordCount = new Sum();

    public Min minQueueTimeMs = new Min();
    public Sum queueTimeMs = new Sum();
    public Max maxQueueTimeMs = new Max();

    public Sum errorCount = new Sum();
    public Sum retryCount = new Sum();

    @Override
    public IMetricValueProvider[] getMetrics() {
        return new IMetricValueProvider[]{
            batchSize,
            maxRecordSize,
            recordCount,
            minQueueTimeMs,
            queueTimeMs,
            maxQueueTimeMs,
            errorCount,
            retryCount
        };
    }
}
