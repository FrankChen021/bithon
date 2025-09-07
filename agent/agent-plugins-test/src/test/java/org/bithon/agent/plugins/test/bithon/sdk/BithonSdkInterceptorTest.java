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

package org.bithon.agent.plugins.test.bithon.sdk;


import com.google.common.collect.ImmutableMap;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;
import org.bithon.agent.observability.exporter.IMessageConverter;
import org.bithon.agent.observability.exporter.IMessageExporter;
import org.bithon.agent.observability.exporter.IMessageExporterFactory;
import org.bithon.agent.observability.exporter.config.ExporterConfig;
import org.bithon.agent.observability.tracing.Tracer;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.reporter.ITraceReporter;
import org.bithon.agent.observability.tracing.reporter.ReporterConfig;
import org.bithon.agent.plugin.bithon.sdk.BithonSdkPlugin;
import org.bithon.agent.plugins.test.AbstractPluginInterceptorTest;
import org.bithon.agent.plugins.test.MavenArtifact;
import org.bithon.agent.plugins.test.MavenArtifactClassLoader;
import org.bithon.agent.sdk.expt.SdkException;
import org.bithon.agent.sdk.tracing.ITraceScope;
import org.bithon.agent.sdk.tracing.TraceContext;
import org.bithon.component.commons.tracing.SpanKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 7/9/25 8:55 pm
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BithonSdkInterceptorTest extends AbstractPluginInterceptorTest {

    public static class TestFactory implements IMessageExporterFactory {
        @Override
        public IMessageExporter createMetricExporter(ExporterConfig exporterConfig) {
            return null;
        }

        @Override
        public IMessageExporter createTracingExporter(ExporterConfig exporterConfig) {
            return new IMessageExporter() {
                @Override
                public void export(Object message) {
                }

                @Override
                public void close() {
                }
            };
        }

        @Override
        public IMessageExporter createEventExporter(ExporterConfig exporterConfig) {
            return null;
        }

        @Override
        public IMessageConverter createMessageConverter() {
            return null;
        }
    }

    @Override
    protected ClassLoader getCustomClassLoader() {
        return MavenArtifactClassLoader.create(
            // Bithon SDK
            MavenArtifact.of("org.bithon.agent", "agent-sdk", "1.2.2")
        );
    }

    @Override
    protected Map<String, String> getEnvironmentVariables() {
        // Add SDK-specific environment variables for testing
        return ImmutableMap.of(
            "bithon_exporters_tracing_client_factory", TestFactory.class.getName()
        );
    }

    @Override
    protected IPlugin[] getPlugins() {
        return new IPlugin[]{
            new BithonSdkPlugin()
        };
    }

    /**
     * Override the parent test to ensure it runs first
     */
    @Test
    @Order(0)
    @Override
    public void testInterceptorInstallation() {
        super.testInterceptorInstallation();
    }

    /**
     * Test case that runs after the parent's interceptor installation test
     */
    @Test
    public void testNewTraceApi() {
        // Replace the default reporter
        final List<ITraceSpan> reportedSpans = new ArrayList<>();
        Tracer.get()
              .reporter(new ITraceReporter() {
                  @Override
                  public ReporterConfig getReporterConfig() {
                      return new ReporterConfig();
                  }

                  @Override
                  public void report(List<ITraceSpan> spans) {
                      reportedSpans.addAll(spans);
                  }
              });

        try (ITraceScope traceScope = TraceContext.newTrace("root").attach()) {
            traceScope.currentSpan().tag("key1", "value1");
        }

        Assertions.assertEquals(1, reportedSpans.size());
        Assertions.assertEquals("root", reportedSpans.get(0).name());
        Assertions.assertEquals(SpanKind.INTERNAL, reportedSpans.get(0).kind());
        Assertions.assertEquals("value1", reportedSpans.get(0).tags().get("key1"));
    }

    @Test
    public void testAttachOnExistingTracing() {
        // Replace the default reporter
        final List<ITraceSpan> reportedSpans = new ArrayList<>();
        Tracer.get()
              .reporter(new ITraceReporter() {
                  @Override
                  public ReporterConfig getReporterConfig() {
                      return new ReporterConfig();
                  }

                  @Override
                  public void report(List<ITraceSpan> spans) {
                      reportedSpans.addAll(spans);
                  }
              });

        try (ITraceScope traceScope = TraceContext.newTrace("root").attach()) {
            try {
                TraceContext.newTrace("child").attach();
                Assertions.fail("Should not reach here");
            } catch (SdkException ignored) {
            }
        }

        Assertions.assertEquals(1, reportedSpans.size());
        Assertions.assertEquals("root", reportedSpans.get(0).name());
        Assertions.assertEquals(SpanKind.INTERNAL, reportedSpans.get(0).kind());
    }
}
