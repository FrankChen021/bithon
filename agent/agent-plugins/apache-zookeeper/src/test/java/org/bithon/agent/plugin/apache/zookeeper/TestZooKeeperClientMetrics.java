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

package org.bithon.agent.plugin.apache.zookeeper;

import com.google.common.collect.ImmutableMap;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.TestingServer;
import org.bithon.agent.configuration.ConfigurationManager;
import org.bithon.agent.configuration.source.Helper;
import org.bithon.agent.configuration.source.PropertySource;
import org.bithon.agent.configuration.source.PropertySourceType;
import org.bithon.agent.instrumentation.aop.interceptor.descriptor.Descriptors;
import org.bithon.agent.instrumentation.aop.interceptor.installer.InterceptorInstaller;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.PluginResolver;
import org.bithon.agent.instrumentation.loader.PluginClassLoader;
import org.bithon.agent.observability.event.EventMessage;
import org.bithon.agent.observability.exporter.IMessageConverter;
import org.bithon.agent.observability.exporter.IMessageExporter;
import org.bithon.agent.observability.exporter.IMessageExporterFactory;
import org.bithon.agent.observability.exporter.config.ExporterConfig;
import org.bithon.agent.observability.metric.domain.jvm.JvmMetrics;
import org.bithon.agent.observability.metric.model.IMeasurement;
import org.bithon.agent.observability.metric.model.schema.Schema;
import org.bithon.agent.observability.metric.model.schema.Schema2;
import org.bithon.agent.observability.metric.model.schema.Schema3;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.shaded.net.bytebuddy.agent.ByteBuddyAgent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @date 2025/2/20 21:38
 */
public class TestZooKeeperClientMetrics {
    private static final List<IMeasurement> METRIC_MESSAGE_LIST = Collections.synchronizedList(new ArrayList<>());

    public static class TestExporterFactory implements IMessageExporterFactory {

        @Override
        public IMessageExporter createMetricExporter(ExporterConfig exporterConfig) {
            return new IMessageExporter() {
                @Override
                public void export(Object message) {
                    if (message instanceof Collection) {
                        //noinspection unchecked
                        METRIC_MESSAGE_LIST.addAll((Collection<IMeasurement>) message);
                        return;
                    }
                    METRIC_MESSAGE_LIST.add((IMeasurement) message);
                }

                @Override
                public void close() {
                }
            };
        }

        @Override
        public IMessageExporter createTracingExporter(ExporterConfig exporterConfig) {
            return null;
        }

        @Override
        public IMessageExporter createEventExporter(ExporterConfig exporterConfig) {
            return null;
        }

        @Override
        public IMessageConverter createMessageConverter() {
            return new IMessageConverter() {
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
                public Object from(Schema schema,
                                   Collection<IMeasurement> measurementList,
                                   long timestamp,
                                   int interval) {
                    return measurementList;
                }

                @Override
                public Object from(Schema2 schema,
                                   Collection<IMeasurement> measurementList,
                                   long timestamp,
                                   int interval) {
                    return measurementList;
                }

                @Override
                public Object from(Schema3 schema, List<Object[]> measurementList, long timestamp, int interval) {
                    return measurementList;
                }
            };
        }
    }

    @BeforeAll
    public static void beforeClass() {
        // Initialize configuration for testing
        try (MockedStatic<Helper> configurationMock = Mockito.mockStatic(Helper.class)) {
            configurationMock.when(Helper::getCommandLineInputArgs)
                             .thenReturn(Arrays.asList("-Dbithon.application.name=test",
                                                       "-Dbithon.application.env=local",
                                                       "-Dbithon.application.port=9897",
                                                       "-Dbithon.exporters.metric.client.factory="
                                                       + TestExporterFactory.class.getName(),
                                                       "-Dbithon.exporters.tracing.client.factory="
                                                       + TestExporterFactory.class.getName(),
                                                       "-Dbithon.exporters.event.client.factory="
                                                       + TestExporterFactory.class.getName()
                             ));

            configurationMock.when(Helper::getEnvironmentVariables)
                             .thenReturn(ImmutableMap.of("bithon_t", "t1",
                                                         //Overwrite the prop2
                                                         "bithon_test_prop2", "from_env"));

            ConfigurationManager.createForTesting(new File("not-exists"));
        }

        PluginClassLoader.setClassLoader(TestZooKeeperClientMetrics.class.getClassLoader());

        // Resolve interceptors
        Descriptors descriptors = new Descriptors();
        ZooKeeperPlugin plugin = new ZooKeeperPlugin();
        descriptors.merge(plugin.getBithonClassDescriptor());
        descriptors.merge(plugin.getClass().getSimpleName(),
                          plugin.getPreconditions(),
                          plugin.getInterceptors());
        PluginResolver.resolveInterceptorType(descriptors.getAllDescriptor());

        // Install interceptors
        ByteBuddyAgent.install();
        new InterceptorInstaller(descriptors)
            .installOn(ByteBuddyAgent.getInstrumentation());
    }

    @BeforeEach
    public void beforeEachTest() {
        METRIC_MESSAGE_LIST.clear();
    }

    @Test
    public void test_AggregatedMetrics() throws Exception {
        // Set the threshold to 1h which is large enough for test case to enable aggregation
        ConfigurationManager.getInstance()
                            .addPropertySource(PropertySource.from(PropertySourceType.DYNAMIC,
                                                                   "d1",
                                                                   "agent.observability.metrics.zookeeper-client-metrics.responseTime=1h"));

        // Start a zookeeper server
        try (TestingServer server = new TestingServer()) {

            // Create client API instance
            try (CuratorFramework client = CuratorFrameworkFactory.newClient(server.getConnectString(),
                                                                             new ExponentialBackoffRetry(1000, 3))) {
                client.start();

                {
                    String path = "/test1";
                    byte[] data = "testData".getBytes(StandardCharsets.UTF_8);
                    client.create().forPath(path, data);
                }
                {
                    String path = "/test2";
                    byte[] data = "testData".getBytes(StandardCharsets.UTF_8);
                    client.create().forPath(path, data);
                }
            }
        }

        // Wait for metrics to be exported
        Thread.sleep(11_000);

        Assertions.assertFalse(METRIC_MESSAGE_LIST.isEmpty());

        List<IMeasurement> createLog = METRIC_MESSAGE_LIST.stream()
                                                          .filter((measurement) -> "Create".equals(measurement.getDimensions()
                                                                                                              .getValue(
                                                                                                                  0)))
                                                          .collect(Collectors.toList());
        Assertions.assertEquals(1, createLog.size());
        Assertions.assertEquals(5, createLog.get(0).getDimensions().length());
        Assertions.assertEquals("OK", createLog.get(0)
                                               .getDimensions()
                                               .getValue(1));

        // path
        Assertions.assertEquals("", createLog.get(0)
                                             .getDimensions()
                                             .getValue(3));

        // traceId
        Assertions.assertEquals("", createLog.get(0)
                                             .getDimensions()
                                             .getValue(4));

        // responseTime
        Assertions.assertNotEquals(0, createLog.get(0).getMetricValue(0));
        Assertions.assertNotEquals(0, createLog.get(0).getMetricValue(1));
        Assertions.assertNotEquals(0, createLog.get(0).getMetricValue(2));

        // totalCount
        Assertions.assertEquals(2, createLog.get(0).getMetricValue("totalCount"));

        METRIC_MESSAGE_LIST.clear();
    }

    @Test
    public void test_DetailLog() throws Exception {
        // Update responseTime threshold to 1ns which is small enough to DISABLE aggregation
        ConfigurationManager.getInstance()
                            .addPropertySource(PropertySource.from(PropertySourceType.DYNAMIC,
                                                                   "d1",
                                                                   "agent.observability.metrics.zookeeper-client-metrics.responseTime=1ns"));

        String zkServer;

        // Start a zookeeper server
        try (TestingServer server = new TestingServer()) {
            zkServer = "localhost:" + server.getPort();

            // Create client API instance
            try (CuratorFramework client = CuratorFrameworkFactory.newClient(server.getConnectString(),
                                                                             new ExponentialBackoffRetry(1000, 3))) {
                client.start();

                {
                    String path = "/test1";
                    byte[] data = "testData".getBytes(StandardCharsets.UTF_8);
                    client.create().forPath(path, data);
                }
                {
                    String path = "/test2";
                    byte[] data = "testData".getBytes(StandardCharsets.UTF_8);
                    client.create().forPath(path, data);
                }
            }
        }

        // Wait for metrics to be exported
        Thread.sleep(11_000);

        List<IMeasurement> createLog = METRIC_MESSAGE_LIST.stream()
                                                          .filter((measurement) -> "Create".equals(measurement.getDimensions()
                                                                                                              .getValue(
                                                                                                                  0)))
                                                          .collect(Collectors.toList());
        Assertions.assertEquals(2, createLog.size());

        // First metric entry
        {
            Assertions.assertEquals(5, createLog.get(0).getDimensions().length());
            Assertions.assertEquals("Create", createLog.get(0)
                                                       .getDimensions()
                                                       .getValue(0));

            Assertions.assertEquals("OK", createLog.get(0)
                                                   .getDimensions()
                                                   .getValue(1));

            Assertions.assertEquals(zkServer, createLog.get(0)
                                                       .getDimensions()
                                                       .getValue(2));

            // path
            Assertions.assertEquals("/test1", createLog.get(0)
                                                       .getDimensions()
                                                       .getValue(3));

            // traceId
            Assertions.assertEquals("", createLog.get(0)
                                                 .getDimensions()
                                                 .getValue(4));

            // responseTime
            Assertions.assertNotEquals(0, createLog.get(0).getMetricValue(0));
            Assertions.assertNotEquals(0, createLog.get(0).getMetricValue(1));
            Assertions.assertNotEquals(0, createLog.get(0).getMetricValue(2));

            // totalCount
            Assertions.assertEquals(1, createLog.get(0).getMetricValue("totalCount"));
        }

        // Second metric entry
        {
            Assertions.assertEquals(5, createLog.get(1).getDimensions().length());
            Assertions.assertEquals("Create", createLog.get(0)
                                                       .getDimensions()
                                                       .getValue(0));

            Assertions.assertEquals("OK", createLog.get(1)
                                                   .getDimensions()
                                                   .getValue(1));

            Assertions.assertEquals(zkServer, createLog.get(0)
                                                       .getDimensions()
                                                       .getValue(2));

            // path
            Assertions.assertEquals("/test2", createLog.get(1)
                                                       .getDimensions()
                                                       .getValue(3));

            // traceId
            Assertions.assertEquals("", createLog.get(1)
                                                 .getDimensions()
                                                 .getValue(4));

            // responseTime
            Assertions.assertNotEquals(0, createLog.get(0).getMetricValue(0));
            Assertions.assertNotEquals(0, createLog.get(0).getMetricValue(1));
            Assertions.assertNotEquals(0, createLog.get(0).getMetricValue(2));

            // totalCount
            Assertions.assertEquals(1, createLog.get(1).getMetricValue("totalCount"));
        }
    }
}
