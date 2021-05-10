/*
 *    Copyright 2020 bithon.cn
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

package com.sbss.bithon.agent.core.metric.domain.jdbc;

import com.sbss.bithon.agent.core.metric.model.Gauge;
import com.sbss.bithon.agent.core.metric.model.Sum;

/**
 * @author frankchen
 */
public class JdbcPoolMetricSet {
    // dimension
    private final String connectionString;
    private final String driverClass;

    // metrics
    public Gauge activeCount = new Gauge();
    public Gauge activePeak = new Gauge();
    public Gauge poolingPeak = new Gauge();
    public Gauge poolingCount = new Gauge();
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

    public JdbcPoolMetricSet(String connectionString, String driverClass) {
        this.connectionString = connectionString;
        this.driverClass = driverClass;
    }

    public String getConnectionString() {
        return connectionString;
    }

    public String getDriverClass() {
        return driverClass;
    }
}
