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

    @ConfigurationProperties(path = "agent.observability.metric.sql")
    public static class SQLMetricsConfig {
        private HumanReadableDuration slowSqlThreshold = HumanReadableDuration.of(5, TimeUnit.SECONDS);

        public HumanReadableDuration getSlowSqlThreshold() {
            return slowSqlThreshold;
        }

        public void setSlowSqlThreshold(HumanReadableDuration slowSqlThreshold) {
            this.slowSqlThreshold = slowSqlThreshold;
        }
    }

    private static volatile SQLMetricStorage INSTANCE;

    public SQLMetricStorage(SQLMetricsConfig config) {
        super("sql-metrics",
              Arrays.asList("connectionString", "sqlType", "traceId", "statement"),
              SQLMetrics.class,
              (dimensions, metrics) -> {
                  Preconditions.checkIfTrue(dimensions.length() == 4, "dimensions.length() == 3");

                  if (metrics.responseTime < config.getSlowSqlThreshold().getDuration().toNanos()) {
                      // Aggregate metrics if response time is less than the threshold
                      // Before that, we need to clear 'traceId' and 'statement' dimensions
                      dimensions.setValue(2, "");
                      dimensions.setValue(3, "");
                      return true;
                  }

                  return false;
              }
        );
    }

    public static SQLMetricStorage get() {
        if (INSTANCE == null) {
            synchronized (SQLMetricStorage.class) {
                if (INSTANCE == null) {
                    SQLMetricsConfig config = ConfigurationManager.getInstance().getConfig(SQLMetricsConfig.class);
                    INSTANCE = new SQLMetricStorage(config);

                    MetricCollectorManager.getInstance().register("sql-metrics", INSTANCE);
                }
            }
        }
        return INSTANCE;
    }
}
