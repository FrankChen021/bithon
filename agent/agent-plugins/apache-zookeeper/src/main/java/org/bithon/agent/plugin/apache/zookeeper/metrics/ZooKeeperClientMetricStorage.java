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

import org.bithon.agent.configuration.ConfigurationManager;
import org.bithon.agent.configuration.ConfigurationProperties;
import org.bithon.agent.observability.metric.collector.MetricCollectorManager;
import org.bithon.agent.observability.metric.model.AbstractMetricStorage;
import org.bithon.component.commons.utils.HumanReadableDuration;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.component.commons.utils.StringUtils;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author frank.chen021@outlook.com
 * @date 15/1/25 4:32 pm
 */
public class ZooKeeperClientMetricStorage extends AbstractMetricStorage<ZooKeeperClientMetrics> {
    public static final String NAME = "zookeeper-client-metrics";

    @ConfigurationProperties(path = "agent.observability.metrics.zookeeper-client-metrics")
    public static class ZKClientMetricsConfig {
        private HumanReadableDuration responseTime = HumanReadableDuration.of(3, TimeUnit.SECONDS);

        public HumanReadableDuration getResponseTime() {
            return responseTime;
        }

        public void setResponseTime(HumanReadableDuration responseTime) {
            this.responseTime = responseTime;
        }
    }

    /**
     * Visible for testing
     */
    ZooKeeperClientMetricStorage(ZKClientMetricsConfig config) {
        super(NAME,
              Arrays.asList("operation", "status", "server", "path", "traceId"),
              ZooKeeperClientMetrics.class,
              ((dimensions, metrics) -> {
                  Preconditions.checkIfTrue(dimensions.length() == 5, "Required 5 dimensions, but got %d", 5, dimensions.length());

                  if (metrics.responseTime < config.getResponseTime().getDuration().toNanos()) {
                      // Aggregate metrics if response time is less than the threshold
                      // Before that, we need to clear 'traceId' and 'path' dimensions
                      dimensions.setValue(3, "");
                      dimensions.setValue(4, "");
                      return true;
                  }

                  if (StringUtils.isEmpty(dimensions.getValue(3)) && StringUtils.isEmpty(dimensions.getValue(4))) {
                      // If both path and traceId is not provided, the metrics can be aggregated
                      return true;
                  }

                  return false;
              }));
    }

    private static volatile ZooKeeperClientMetricStorage INSTANCE;

    public static ZooKeeperClientMetricStorage getInstance() {
        if (INSTANCE == null) {
            synchronized (ZooKeeperClientMetricStorage.class) {
                if (INSTANCE == null) {
                    ZKClientMetricsConfig config = ConfigurationManager.getInstance().getConfig(ZKClientMetricsConfig.class);
                    INSTANCE = new ZooKeeperClientMetricStorage(config);

                    MetricCollectorManager.getInstance().register(NAME, INSTANCE);
                }
            }
        }
        return INSTANCE;
    }
}
