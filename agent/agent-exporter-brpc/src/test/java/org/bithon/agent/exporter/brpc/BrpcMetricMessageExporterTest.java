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

package org.bithon.agent.exporter.brpc;

import org.bithon.agent.config.RpcClientConfig;
import org.bithon.agent.configuration.ConfigurationManager;
import org.bithon.agent.observability.exporter.config.ExporterConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class BrpcMetricMessageExporterTest {

    @TempDir
    private Path tempDir;

    @Test
    public void testExportDoesNotCreateConnectionDuringShutdown() throws Exception {
        Path agentConfig = tempDir.resolve("agent.yml");
        Files.write(agentConfig, "application:\n  name: test\n  env: local\n".getBytes(StandardCharsets.UTF_8));
        ConfigurationManager.createForTesting(agentConfig.toFile());

        BrpcMetricMessageExporter exporter = new BrpcMetricMessageExporter(createExporterConfig());
        try {
            exporter.prepareShutdown();
            exporter.export(new Object());

            Field metricCollector = BrpcMetricMessageExporter.class.getDeclaredField("metricCollector");
            metricCollector.setAccessible(true);
            Assertions.assertNull(metricCollector.get(exporter));
        } finally {
            exporter.close();
        }
    }

    @Test
    public void testPrepareShutdownDoesNotWaitForCollectorCreation() throws Exception {
        Path agentConfig = tempDir.resolve("agent.yml");
        Files.write(agentConfig, "application:\n  name: test\n  env: local\n".getBytes(StandardCharsets.UTF_8));
        ConfigurationManager.createForTesting(agentConfig.toFile());

        BrpcMetricMessageExporter exporter = new BrpcMetricMessageExporter(createExporterConfig());
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        // Hold the lock of the exporter like the collector creation that is connecting to the server
        Thread creating = new Thread(() -> {
            synchronized (exporter) {
                locked.countDown();
                try {
                    release.await();
                } catch (InterruptedException ignored) {
                }
            }
        });
        creating.start();
        try {
            Assertions.assertTrue(locked.await(5, TimeUnit.SECONDS));

            Thread preparing = new Thread(exporter::prepareShutdown);
            preparing.start();
            preparing.join(5000);
            Assertions.assertFalse(preparing.isAlive(), "prepareShutdown() waits for the collector creation");
        } finally {
            release.countDown();
            creating.join(5000);
            exporter.close();
        }
    }

    private static ExporterConfig createExporterConfig() {
        RpcClientConfig clientConfig = new RpcClientConfig();
        clientConfig.setConnectionTimeout(60_000);

        ExporterConfig exporterConfig = new ExporterConfig();
        exporterConfig.setClient(clientConfig);
        exporterConfig.setServers("127.0.0.1:1");
        return exporterConfig;
    }
}
