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

package org.bithon.agent.core.metric.domain.jdbc;

import org.bithon.agent.core.metric.model.Gauge;
import org.bithon.agent.core.metric.model.IMetricSet;
import org.bithon.agent.core.metric.model.IMetricValueProvider;
import org.bithon.agent.core.metric.model.Sum;

/**
 * @author frankchen
 */
public class JdbcPoolMetrics implements IMetricSet {
    public final Gauge activeCount;
    public final Gauge activePeak;
    public final Gauge poolingPeak;
    public final Gauge poolingCount;
    private final IMetricValueProvider[] metrics;
    public Sum createCount = new Sum();
    public Sum destroyCount = new Sum();
    public Sum logicConnectionCount = new Sum();
    public Sum logicCloseCount = new Sum();
    public Sum createErrorCount = new Sum();
    public Sum executeCount = new Sum();
    public Sum commitCount = new Sum();
    public Sum rollbackCount = new Sum();
    public Sum startTransactionCount = new Sum();
    public Sum waitThreadCount = new Sum();

    public JdbcPoolMetrics(Gauge activeCount, Gauge activePeak, Gauge poolingPeak, Gauge poolingCount) {
        this.activeCount = activeCount;
        this.activePeak = activePeak;
        this.poolingPeak = poolingPeak;
        this.poolingCount = poolingCount;

        metrics = new IMetricValueProvider[]{
            activeCount,
            activePeak,
            poolingPeak,
            poolingCount,
            createCount,
            destroyCount,
            logicConnectionCount,
            logicCloseCount,
            createErrorCount,
            executeCount,
            commitCount,
            rollbackCount,
            startTransactionCount,
            waitThreadCount
        };
    }

    @Override
    public IMetricValueProvider[] getMetrics() {
        return metrics;
    }
}
