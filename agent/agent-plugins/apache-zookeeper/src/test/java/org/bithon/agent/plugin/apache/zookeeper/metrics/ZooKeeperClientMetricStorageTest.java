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

import org.bithon.agent.observability.event.EventMessage;
import org.bithon.agent.observability.exporter.IMessageConverter;
import org.bithon.agent.observability.metric.domain.jvm.JvmMetrics;
import org.bithon.agent.observability.metric.model.IMeasurement;
import org.bithon.agent.observability.metric.model.schema.Dimensions;
import org.bithon.agent.observability.metric.model.schema.Schema;
import org.bithon.agent.observability.metric.model.schema.Schema2;
import org.bithon.agent.observability.metric.model.schema.Schema3;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.component.commons.utils.HumanReadableDuration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ZooKeeperClientMetricStorageTest {

    private final IMessageConverter converter = new IMessageConverter() {
        @Override
        public Object from(long timestamp, int interval, JvmMetrics metrics) {
            return null;
        }

        @Override
        public Object from(ITraceSpan span) {
            return null;
        }

        @Override
        public Object from(EventMessage event) {
            return null;
        }

        @Override
        public Object from(Map<String, String> log) {
            return null;
        }

        @Override
        public Object from(Schema schema, Collection<IMeasurement> measurementList, long timestamp, int interval) {
            return measurementList;
        }

        @Override
        public Object from(Schema2 schema, Collection<IMeasurement> measurementList, long timestamp, int interval) {
            return measurementList;
        }

        @Override
        public Object from(Schema3 schema, List<Object[]> measurementList, long timestamp, int interval) {
            return measurementList;
        }
    };

    @Test
    public void test_MetricAggregation() {
        ZooKeeperClientMetricStorage.ZKClientMetricsConfig config = new ZooKeeperClientMetricStorage.ZKClientMetricsConfig();
        ZooKeeperClientMetricStorage storage = new ZooKeeperClientMetricStorage(config);
        storage.add(Dimensions.of("operation", "status", "server", "path", "traceId"),
                    (metric) -> {
                        metric.responseTime = 1000;
                        metric.minResponseTime = 1000;
                        metric.maxResponseTime = 1000;
                        metric.totalCount = 1;
                        metric.bytesReceived = 200;
                        metric.bytesSent = 300;
                    });

        storage.add(Dimensions.of("operation", "status", "server", "path", "traceId"),
                    (metric) -> {
                        metric.responseTime = 3000;
                        metric.minResponseTime = 3000;
                        metric.maxResponseTime = metric.responseTime;
                        metric.totalCount = 1;
                        metric.bytesReceived = 300;
                        metric.bytesSent = 400;
                    });

        // Dimension is different, should be in different bucket
        storage.add(Dimensions.of("op2", "status", "server", "path", "traceId"),
                    (metric) -> {
                        metric.responseTime = 3000;
                        metric.minResponseTime = metric.responseTime;
                        metric.maxResponseTime = metric.responseTime;
                        metric.totalCount = 1;
                        metric.bytesReceived = 300;
                        metric.bytesSent = 400;
                    });

        // Although path2 and traceId are different, they will be ignored during aggregation
        storage.add(Dimensions.of("op2", "status", "server", "path2", "traceId2"),
                    (metric) -> {
                        metric.responseTime = 5000;
                        metric.minResponseTime = metric.responseTime;
                        metric.maxResponseTime = metric.responseTime;
                        metric.totalCount = 1;
                        metric.bytesReceived = 3;
                        metric.bytesSent = 4;
                    });

        //noinspection unchecked
        List<IMeasurement> measurementList = (List<IMeasurement>) storage.collect(converter, 1, System.currentTimeMillis());
        Assertions.assertEquals(2, measurementList.size());

        // First aggregate entry
        Assertions.assertEquals(2, measurementList.get(0).getMetricValue("totalCount"));
        Assertions.assertEquals(4000, measurementList.get(0).getMetricValue("responseTime"));
        Assertions.assertEquals(1000, measurementList.get(0).getMetricValue("minResponseTime"));
        Assertions.assertEquals(3000, measurementList.get(0).getMetricValue("maxResponseTime"));
        Assertions.assertEquals(500, measurementList.get(0).getMetricValue("bytesReceived"));
        Assertions.assertEquals(700, measurementList.get(0).getMetricValue("bytesSent"));

        Assertions.assertEquals(2, measurementList.get(1).getMetricValue("totalCount"));
        Assertions.assertEquals(8000, measurementList.get(1).getMetricValue("responseTime"));
        Assertions.assertEquals(3000, measurementList.get(1).getMetricValue("minResponseTime"));
        Assertions.assertEquals(5000, measurementList.get(1).getMetricValue("maxResponseTime"));
        Assertions.assertEquals(303, measurementList.get(1).getMetricValue("bytesReceived"));
        Assertions.assertEquals(404, measurementList.get(1).getMetricValue("bytesSent"));
    }

    @Test
    public void test_RawMetrics() {
        ZooKeeperClientMetricStorage.ZKClientMetricsConfig config = new ZooKeeperClientMetricStorage.ZKClientMetricsConfig();
        config.setResponseTime(HumanReadableDuration.of(3, TimeUnit.SECONDS));

        ZooKeeperClientMetricStorage storage = new ZooKeeperClientMetricStorage(config);

        // Should be aggregated
        storage.add(Dimensions.of("operation", "status", "server", "path", "traceId"),
                    (metric) -> {
                        metric.responseTime = Duration.ofSeconds(1).toNanos();
                        metric.minResponseTime = metric.responseTime;
                        metric.maxResponseTime = metric.responseTime;
                        metric.totalCount = 1;
                        metric.bytesReceived = 200;
                        metric.bytesSent = 300;
                    });

        // Should be aggregated
        storage.add(Dimensions.of("operation", "status", "server", "path", "traceId"),
                    (metric) -> {
                        metric.responseTime = Duration.ofSeconds(2).toNanos();
                        metric.minResponseTime = metric.responseTime;
                        metric.maxResponseTime = metric.responseTime;
                        metric.totalCount = 1;
                        metric.bytesReceived = 300;
                        metric.bytesSent = 400;
                    });

        // SHOULD NOT be aggregated, 3 is NOT inclusive for aggregation
        storage.add(Dimensions.of("operation", "status", "server", "path", "traceId"),
                    (metric) -> {
                        metric.responseTime = Duration.ofSeconds(3).toNanos();
                        metric.minResponseTime = metric.responseTime;
                        metric.maxResponseTime = metric.responseTime;
                        metric.totalCount = 1;
                        metric.bytesReceived = 2;
                        metric.bytesSent = 3;
                    });

        // SHOULD NOT be aggregated
        storage.add(Dimensions.of("operation", "status", "server", "path", "traceId"),
                    (metric) -> {
                        metric.responseTime = Duration.ofSeconds(4).toNanos();
                        metric.minResponseTime = metric.responseTime;
                        metric.maxResponseTime = metric.responseTime;
                        metric.totalCount = 1;
                        metric.bytesReceived = 4;
                        metric.bytesSent = 5;
                    });

        //noinspection unchecked
        List<IMeasurement> measurementList = (List<IMeasurement>) storage.collect(converter, 1, System.currentTimeMillis());
        Assertions.assertEquals(3, measurementList.size());

        // First is the aggregated one
        Assertions.assertEquals(2, measurementList.get(0).getMetricValue("totalCount"));
        Assertions.assertEquals(Duration.ofSeconds(3).toNanos(), measurementList.get(0).getMetricValue("responseTime"));
        Assertions.assertEquals(Duration.ofSeconds(1).toNanos(), measurementList.get(0).getMetricValue("minResponseTime"));
        Assertions.assertEquals(Duration.ofSeconds(2).toNanos(), measurementList.get(0).getMetricValue("maxResponseTime"));
        Assertions.assertEquals(500, measurementList.get(0).getMetricValue("bytesReceived"));
        Assertions.assertEquals(700, measurementList.get(0).getMetricValue("bytesSent"));

        // Raw metrics
        Assertions.assertEquals(1, measurementList.get(1).getMetricValue("totalCount"));
        Assertions.assertEquals(Duration.ofSeconds(3).toNanos(), measurementList.get(1).getMetricValue("responseTime"));
        Assertions.assertEquals(Duration.ofSeconds(3).toNanos(), measurementList.get(1).getMetricValue("minResponseTime"));
        Assertions.assertEquals(Duration.ofSeconds(3).toNanos(), measurementList.get(1).getMetricValue("maxResponseTime"));
        Assertions.assertEquals(2, measurementList.get(1).getMetricValue("bytesReceived"));
        Assertions.assertEquals(3, measurementList.get(1).getMetricValue("bytesSent"));

        Assertions.assertEquals(1, measurementList.get(2).getMetricValue("totalCount"));
        Assertions.assertEquals(Duration.ofSeconds(4).toNanos(), measurementList.get(2).getMetricValue("responseTime"));
        Assertions.assertEquals(Duration.ofSeconds(4).toNanos(), measurementList.get(2).getMetricValue("minResponseTime"));
        Assertions.assertEquals(Duration.ofSeconds(4).toNanos(), measurementList.get(2).getMetricValue("maxResponseTime"));
        Assertions.assertEquals(4, measurementList.get(2).getMetricValue("bytesReceived"));
        Assertions.assertEquals(5, measurementList.get(2).getMetricValue("bytesSent"));
    }
}
