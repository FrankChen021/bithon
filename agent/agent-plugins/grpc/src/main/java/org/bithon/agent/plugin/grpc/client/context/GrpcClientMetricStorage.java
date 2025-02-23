package org.bithon.agent.plugin.grpc.client.context;

import org.bithon.agent.observability.metric.collector.MetricCollectorManager;
import org.bithon.agent.observability.metric.model.AbstractMetricStorage;
import org.bithon.agent.observability.metric.model.schema.Dimensions;
import org.bithon.agent.plugin.grpc.metrics.GrpcMetrics;
import org.bithon.component.commons.utils.Preconditions;

import java.util.Arrays;
import java.util.function.Consumer;

/**
 * @author frank.chen021@outlook.com
 */
public class GrpcClientMetricStorage extends AbstractMetricStorage<GrpcMetrics> {
    public static final String NAME = "grpc-client-metrics";

    private static volatile GrpcClientMetricStorage INSTANCE;

    public GrpcClientMetricStorage() {
        super(NAME,
              Arrays.asList("service", "method", "status", "server"),
              GrpcMetrics.class,
              (dimensions, metric) -> {
                  Preconditions.checkIfTrue(dimensions.length() == 3, "dimensions.length() == 2");
                  return true;
              });
    }

    public void add(String service, String method, String server, Consumer<GrpcMetrics> metricProvider) {
        super.add(Dimensions.of(service, method, server), metricProvider);
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
