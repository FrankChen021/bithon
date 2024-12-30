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

package org.bithon.agent.plugin.jdbc.alibaba.druid.metric;

import com.alibaba.druid.pool.DruidDataSource;
import org.bithon.agent.observability.metric.domain.jdbc.JdbcPoolMetrics;

import java.util.Arrays;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/27 11:32 上午
 */
public class MonitoredSource {
    private final DruidDataSource dataSource;
    private final List<String> dimensions;
    // metrics
    private final JdbcPoolMetrics jdbcPoolMetrics;

    MonitoredSource(String driverClass, String connectionString, DruidDataSource dataSource) {
        this.dataSource = dataSource;
        this.dimensions = Arrays.asList(connectionString, driverClass,
                                        // support multiple data source for a same endpoint, typically read-write data sources
                                        String.valueOf(System.identityHashCode(dataSource)));
        this.jdbcPoolMetrics = new JdbcPoolMetrics(dataSource::getActiveCount,
                                                   dataSource::getActivePeak,
                                                   dataSource::getPoolingPeak,
                                                   dataSource::getPoolingCount);
    }

    public List<String> getDimensions() {
        return dimensions;
    }

    public DruidDataSource getDataSource() {
        return dataSource;
    }

    public JdbcPoolMetrics getJdbcMetric() {
        return jdbcPoolMetrics;
    }
}
