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

package org.bithon.agent.observability.metric.domain.sql;

import org.bithon.agent.configuration.ConfigurationManager;
import org.bithon.agent.configuration.ConfigurationProperties;
import org.bithon.agent.observability.metric.collector.MetricCollectorManager;
import org.bithon.agent.observability.metric.model.AbstractMetricStorage;
import org.bithon.component.commons.utils.HumanReadableDuration;
import org.bithon.component.commons.utils.Preconditions;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author frank.chen021@outlook.com
 * @date 2025/2/5 20:22
 */
public class SQLMetricStorage extends AbstractMetricStorage<SQLMetrics> {
    private static final String NAME = "sql-metrics";

    /**
     * Note:
     * 1. The property path must be under 'agent.observability.metrics'
     * 2. The property name must be the same as the metric name
     */
    @ConfigurationProperties(path = "agent.observability.metrics." + NAME)
    public static class SQLMetricsConfig {
        private HumanReadableDuration responseTime = HumanReadableDuration.of(5, TimeUnit.SECONDS);

        public HumanReadableDuration getResponseTime() {
            return responseTime;
        }

        public void setResponseTime(HumanReadableDuration responseTime) {
            this.responseTime = responseTime;
        }
    }

    private static volatile SQLMetricStorage INSTANCE;

    public SQLMetricStorage(SQLMetricsConfig config) {
        super(NAME,
              Arrays.asList("connectionString", "sqlType", "traceId", "statement"),
              SQLMetrics.class,
              (dimensions, metrics) -> {
                  Preconditions.checkIfTrue(dimensions.length() == 4, "dimensions.length() must be 4");

                  if (metrics.responseTime < config.getResponseTime().getDuration().toNanos()) {
                      // Aggregate metrics if response time is less than the threshold
                      // Before that, we need to clear 'traceId' and 'statement' dimensions
                      dimensions.setValue(2, "");
                      dimensions.setValue(3, "");
                      return true;
                  }

                  if (dimensions.getValue(2).isEmpty() && dimensions.getValue(3).isEmpty()) {
                      // If both traceId and statement are not provided, the metrics can be aggregated
                      return true;
                  }

                  return false;
              }
        );
    }

    public static SQLMetricStorage getInstance() {
        if (INSTANCE == null) {
            synchronized (SQLMetricStorage.class) {
                if (INSTANCE == null) {
                    SQLMetricsConfig config = ConfigurationManager.getInstance().getConfig(SQLMetricsConfig.class);
                    INSTANCE = new SQLMetricStorage(config);

                    MetricCollectorManager.getInstance().register(NAME, INSTANCE);
                }
            }
        }

        return INSTANCE;
    }
}
