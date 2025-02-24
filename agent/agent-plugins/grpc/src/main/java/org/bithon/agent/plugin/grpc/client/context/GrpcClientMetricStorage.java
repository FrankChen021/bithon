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

package org.bithon.agent.plugin.grpc.client.context;

import org.bithon.agent.observability.metric.collector.MetricCollectorManager;
import org.bithon.agent.observability.metric.model.AbstractMetricStorage;
import org.bithon.agent.observability.metric.model.schema.Dimensions;
import org.bithon.agent.plugin.grpc.metrics.GrpcMetrics;
import org.bithon.component.commons.utils.Preconditions;

import java.util.Arrays;

/**
 * @author frank.chen021@outlook.com
 * @date 2025/2/23 8:49
 */
public class GrpcClientMetricStorage extends AbstractMetricStorage<GrpcMetrics> {
    public static final String NAME = "grpc-client-metrics";

    private static volatile GrpcClientMetricStorage INSTANCE;

    public GrpcClientMetricStorage() {
        super(NAME,
              Arrays.asList("service", "method", "status", "server"),
              GrpcMetrics.class,
              (dimensions, metric) -> {
                  Preconditions.checkIfTrue(dimensions.length() == 4, "Number of dimensions must be 4");

                  // Always aggregate metrics
                  return true;
              });
    }

    public void add(String service,
                    String method,
                    String server,
                    String status,
                    GrpcMetrics metrics) {
        super.add(Dimensions.of(service, method, status, server), metrics);
    }

    public static GrpcClientMetricStorage getInstance() {
        if (INSTANCE == null) {
            synchronized (GrpcClientMetricStorage.class) {
                if (INSTANCE == null) {
                    INSTANCE = new GrpcClientMetricStorage();

                    MetricCollectorManager.getInstance().register(NAME, INSTANCE);
                }
            }
        }
        return INSTANCE;
    }
}
