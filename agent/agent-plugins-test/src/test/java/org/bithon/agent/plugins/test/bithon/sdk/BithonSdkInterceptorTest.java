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
import org.bithon.agent.sdk.tracing.ISpan;
import org.bithon.agent.sdk.tracing.ISpanScope;
import org.bithon.agent.sdk.tracing.ITraceScope;
import org.bithon.agent.sdk.tracing.TraceContext;
import org.bithon.agent.sdk.tracing.TracingMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 7/9/25 8:55 pm
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BithonSdkInterceptorTest extends AbstractPluginInterceptorTest {

    private final List<ITraceSpan> reportedSpans = new ArrayList<>();

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
            // Bithon SDK - use local version for testing new API
            MavenArtifact.of("org.bithon.agent", "agent-sdk", "1.2.3")
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

    @Override
    protected void initializeBeforeEachTestCase() {
        reportedSpans.clear();

        // Replace default report
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
        try (ITraceScope traceScope = TraceContext.newTrace("root").attach()) {
            traceScope.currentSpan().tag("key1", "value1");

            Assertions.assertEquals(TracingMode.TRACING, traceScope.tracingMode());
        }

        Assertions.assertEquals(1, reportedSpans.size());
        Assertions.assertEquals("root", reportedSpans.get(0).name());
        Assertions.assertEquals(org.bithon.component.commons.tracing.SpanKind.INTERNAL, reportedSpans.get(0).kind());
        Assertions.assertEquals("value1", reportedSpans.get(0).tags().get("key1"));
    }

    @Test
    public void testAttachOnExistingTracing() {
        try (ITraceScope traceScope = TraceContext.newTrace("root").attach()) {

            Assertions.assertEquals(TracingMode.TRACING, traceScope.tracingMode());

            try {
                TraceContext.newTrace("child").attach();
                Assertions.fail("Should not reach here");
            } catch (SdkException ignored) {
            }
        }

        Assertions.assertEquals(1, reportedSpans.size());
        Assertions.assertEquals("root", reportedSpans.get(0).name());
        Assertions.assertEquals(org.bithon.component.commons.tracing.SpanKind.INTERNAL, reportedSpans.get(0).kind());
    }

    @Test
    public void testNewScopedSpan() {
        // Test TraceContext methods outside of trace scope
        Assertions.assertNull(TraceContext.currentTraceId());
        Assertions.assertNull(TraceContext.currentSpanId());
        ISpan scopedSpan = TraceContext.newScopedSpan();
        Assertions.assertNotNull(scopedSpan);

        // Test TraceContext methods inside trace scope
        try (ITraceScope traceScope = TraceContext.newTrace("test-operation")
                                                  .tracingMode(TracingMode.TRACING)
                                                  .attach()) {
            String traceId = TraceContext.currentTraceId();
            String spanId = TraceContext.currentSpanId();

            Assertions.assertNotNull(traceId);
            Assertions.assertNotNull(spanId);
            Assertions.assertEquals(traceId, traceScope.currentTraceId());
            Assertions.assertEquals(spanId, traceScope.currentSpan().spanId());

            // Test trace scope methods
            TracingMode mode = traceScope.tracingMode();
            ISpanScope span = traceScope.currentSpan();

            Assertions.assertEquals(TracingMode.TRACING, mode);
            Assertions.assertNotNull(span);
            Assertions.assertEquals(traceId, span.traceId());

            // Test that currentSpan() returns different instances but same span
            ISpanScope span2 = traceScope.currentSpan();
            Assertions.assertNotSame(span, span2); // Different instances
            Assertions.assertEquals(span.spanId(), span2.spanId()); // Same span ID

            // Test newScopedSpan within existing trace context
            try (ISpan nestedSpan = TraceContext.newScopedSpan()) {
                Assertions.assertEquals(traceId, nestedSpan.traceId());
                Assertions.assertEquals(spanId, nestedSpan.parentId());
                Assertions.assertNotNull(nestedSpan.spanId());
                Assertions.assertNotEquals("", nestedSpan.spanId());
                Assertions.assertNotEquals(spanId, nestedSpan.spanId());
            }
        }
    }

    @Test
    public void testNewScopedSpanWithBuilder() {
        // Test new builder-based newScopedSpan API
        try (ITraceScope traceScope = TraceContext.newTrace("root-operation")
                                                  .tracingMode(TracingMode.TRACING)
                                                  .attach()) {
            String traceId = TraceContext.currentTraceId();
            String rootSpanId = TraceContext.currentSpanId();

            // Test simple scoped span with builder
            try (ISpanScope span = TraceContext.newScopedSpan("child-operation").create()) {
                Assertions.assertEquals(traceId, span.traceId());
                Assertions.assertEquals(rootSpanId, span.parentId());
                Assertions.assertNotNull(span.spanId());
                Assertions.assertNotEquals("", span.spanId());
                Assertions.assertNotEquals(rootSpanId, span.spanId());

                // Test span name is set correctly
                Assertions.assertEquals("child-operation", span.name());
            }

            // Test scoped span with custom kind
            try (ISpanScope span = TraceContext.newScopedSpan("client-operation")
                                               .kind(org.bithon.agent.sdk.tracing.SpanKind.CLIENT)
                                               .create()) {
                Assertions.assertEquals(traceId, span.traceId());
                Assertions.assertEquals(rootSpanId, span.parentId());
                Assertions.assertEquals("client-operation", span.name());
                Assertions.assertEquals(org.bithon.agent.sdk.tracing.SpanKind.CLIENT, span.kind());
            }
        }

        // Verify spans were reported
        Assertions.assertEquals(3, reportedSpans.size()); // root + 2 child spans

        // Verify span names
        Assertions.assertEquals("child-operation", reportedSpans.get(0).name());
        Assertions.assertEquals(org.bithon.component.commons.tracing.SpanKind.INTERNAL, reportedSpans.get(0).kind());

        Assertions.assertEquals("client-operation", reportedSpans.get(1).name());
        Assertions.assertEquals(org.bithon.component.commons.tracing.SpanKind.CLIENT, reportedSpans.get(1).kind());

        Assertions.assertEquals("root-operation", reportedSpans.get(2).name());
        Assertions.assertEquals(org.bithon.component.commons.tracing.SpanKind.INTERNAL, reportedSpans.get(2).kind());
    }

    @Test
    public void testNewScopedSpanBuilderValidation() {
        // Test null operation name validation
        Assertions.assertThrows(IllegalArgumentException.class, () -> TraceContext.newScopedSpan(null));

        // Test that scoped span requires active trace context
        ISpanScope scope = TraceContext.newScopedSpan("operation").create();
        Assertions.assertEquals(ISpanScope.NOOP_INSTANCE, scope);
    }

    @Test
    public void testBasicTracingModes() {
        // Test TRACING mode (default)
        try (ITraceScope tracingScope = TraceContext.newTrace("tracing-mode")
                                                    .tracingMode(TracingMode.TRACING)
                                                    .attach()) {
            Assertions.assertEquals(TracingMode.TRACING, tracingScope.tracingMode());
        }

        // Test LOGGING mode
        try (ITraceScope loggingScope = TraceContext.newTrace("logging-mode")
                                                    .tracingMode(TracingMode.LOGGING)
                                                    .attach()) {
            Assertions.assertEquals(TracingMode.LOGGING, loggingScope.tracingMode());
        }
    }

    @Test
    public void testSpanTagMethod() {
        try (ITraceScope traceScope = TraceContext.newTrace("tagging-test").attach()) {
            ISpanScope span = traceScope.currentSpan();

            // Test different tag types
            span.tag("string-tag", "string-value");
            span.tag("int-tag", 42);
            span.tag("long-tag", 123456789L);
            span.tag("object-tag", new Object() {
                @Override
                public String toString() {
                    return "custom-object";
                }
            });

            // Test exception tagging
            RuntimeException testException = new RuntimeException("test exception");
            span.tag(testException);

            // Test socket address tagging
            InetSocketAddress address = new InetSocketAddress("localhost", 8080);
            span.tag("socket-address", address);

            // Verify span has basic properties
            Assertions.assertNotNull(span.traceId());
            Assertions.assertNotNull(span.spanId());
        }

        // Verify tags were set correctly
        Assertions.assertEquals(1, reportedSpans.size());
        Map<String, String> tags = reportedSpans.get(0).tags();

        Assertions.assertEquals("string-value", tags.get("string-tag"));
        Assertions.assertEquals("42", tags.get("int-tag"));
        Assertions.assertEquals("123456789", tags.get("long-tag"));
        Assertions.assertEquals("custom-object", tags.get("object-tag"));

        // Verify exception tags are present
        Assertions.assertTrue(tags.containsKey("exception.type"));
        Assertions.assertEquals("java.lang.RuntimeException", tags.get("exception.type"));
        Assertions.assertTrue(tags.containsKey("exception.message"));
        Assertions.assertEquals("test exception", tags.get("exception.message"));
        Assertions.assertTrue(tags.containsKey("exception.stacktrace"));
        Assertions.assertTrue(tags.get("exception.stacktrace").contains("RuntimeException"));

        // Verify socket address tag is present (localhost resolves to 127.0.0.1)
        Assertions.assertTrue(tags.containsKey("socket-address"));
        Assertions.assertEquals("127.0.0.1:8080", tags.get("socket-address"));
    }

    @Test
    public void testSpanMetadataApis() {
        try (ITraceScope traceScope = TraceContext.newTrace("metadata-test").attach()) {
            ISpanScope span = traceScope.currentSpan();

            // Test span metadata
            Assertions.assertNotNull(span.traceId());
            Assertions.assertNotNull(span.spanId());

            // Test setting class and method information - verify they don't throw exceptions
            span.clazz("TestClass");

            // Test method with Class and String
            span.method(BithonSdkInterceptorTest.class, "testSpanMetadataApis");

            // Test method with String class and method
            span.method("org.example.TestClass", "exampleMethod");

            try {
                Method testMethod = BithonSdkInterceptorTest.class.getMethod("testSpanMetadataApis");
                span.method(testMethod);
            } catch (NoSuchMethodException e) {
                // Ignore if method not found
            }
        }
    }

    @Test
    public void testNestedScopedSpans() {
        try (ITraceScope rootScope = TraceContext.newTrace("root-span").attach()) {
            String rootTraceId = rootScope.currentTraceId();
            String rootSpanId = rootScope.currentSpan().spanId();

            // Verify root span setup
            Assertions.assertNotNull(rootTraceId);
            Assertions.assertNotNull(rootSpanId);
            Assertions.assertEquals(rootSpanId, rootScope.currentSpan().spanId());

            // Create first nested scoped span
            try (ISpan level1Span = TraceContext.newScopedSpan()) {
                String level1SpanId = level1Span.spanId();

                // Verify level 1 span relationships
                Assertions.assertEquals(rootTraceId, level1Span.traceId());
                Assertions.assertEquals(rootSpanId, level1Span.parentId());
                Assertions.assertNotNull(level1SpanId);
                Assertions.assertNotEquals("", level1SpanId);
                Assertions.assertNotEquals(rootSpanId, level1SpanId);

                // Verify current span in trace scope has changed to the new span
                Assertions.assertEquals(level1SpanId, rootScope.currentSpan().spanId());

                // Create second nested scoped span
                try (ISpan level2Span = TraceContext.newScopedSpan()) {
                    String level2SpanId = level2Span.spanId();

                    // Verify level 2 span relationships
                    Assertions.assertEquals(rootTraceId, level2Span.traceId());
                    Assertions.assertEquals(level1SpanId, level2Span.parentId()); // Should parent to level1 span
                    Assertions.assertNotNull(level2SpanId);
                    Assertions.assertNotEquals("", level2SpanId);
                    Assertions.assertNotEquals(rootSpanId, level2SpanId);
                    Assertions.assertNotEquals(level1SpanId, level2SpanId);

                    // Verify current span in trace scope has changed to the level2 span
                    Assertions.assertEquals(level2SpanId, rootScope.currentSpan().spanId());

                    // Create third nested scoped span to test deeper nesting
                    try (ISpan level3Span = TraceContext.newScopedSpan()) {
                        String level3SpanId = level3Span.spanId();

                        // Verify level 3 span relationships
                        Assertions.assertEquals(rootTraceId, level3Span.traceId());
                        Assertions.assertEquals(level2SpanId, level3Span.parentId()); // Should parent to level2 span
                        Assertions.assertNotNull(level3SpanId);
                        Assertions.assertNotEquals("", level3SpanId);
                        Assertions.assertNotEquals(rootSpanId, level3SpanId);
                        Assertions.assertNotEquals(level1SpanId, level3SpanId);
                        Assertions.assertNotEquals(level2SpanId, level3SpanId);

                        // Verify current span in trace scope has changed to the level3 span
                        Assertions.assertEquals(level3SpanId, rootScope.currentSpan().spanId());
                    }

                    // After level3 span is closed, current span should return to level2
                    Assertions.assertEquals(level2SpanId, rootScope.currentSpan().spanId());
                }

                // After level2 span is closed, current span should return to level1
                Assertions.assertEquals(level1SpanId, rootScope.currentSpan().spanId());
            }

            // After level1 span is closed, current span should return to root
            Assertions.assertEquals(rootSpanId, rootScope.currentSpan().spanId());
        }

        // Verify all spans were reported (root + 3 nested scoped spans)
        Assertions.assertEquals(4, reportedSpans.size());

        // Verify all spans have the same trace ID
        String expectedTraceId = reportedSpans.get(0).traceId();
        for (ITraceSpan span : reportedSpans) {
            Assertions.assertEquals(expectedTraceId, span.traceId());
        }
    }

    @Test
    public void testSpanLifecycle() {
        try (ITraceScope traceScope = TraceContext.newTrace("lifecycle-test").attach()) {
            ISpanScope span = traceScope.currentSpan();

            // Test timing methods
            long startTime = span.startTime();
            Assertions.assertTrue(startTime > 0);

            // Span should not be finished yet
            Assertions.assertEquals(0, span.endTime());

            // Test multiple finish calls (should be safe)
            span.close();
            span.close();
        }
    }


    @Test
    public void testParentChildRelationships() {
        String traceId;
        String spanId;

        // Create parent trace
        try (ITraceScope parentScope = TraceContext.newTrace("parent-operation").attach()) {
            traceId = parentScope.currentTraceId();
            spanId = parentScope.currentSpan().spanId();

            // Verify parent span setup via TraceContext.currentSpan API
            ISpanScope currentSpan = TraceContext.currentSpan();
            Assertions.assertEquals(traceId, currentSpan.traceId());
            Assertions.assertEquals(spanId, currentSpan.spanId());
            Assertions.assertEquals("parent-operation", currentSpan.name());
            // Set tag
            currentSpan.tag("a1", "v1");

            parentScope.currentSpan().tag("parent", "true");
            Assertions.assertEquals("v1", parentScope.currentSpan().tags().get("a1"));
        }

        // Create child trace with explicit parent
        try (ITraceScope childScope = TraceContext.newTrace("child-operation")
                                                  .parent(traceId, spanId)
                                                  .attach()) {

            Assertions.assertEquals(traceId, childScope.currentTraceId());
            // Note: SDK ISpan doesn't expose parentId(), but we can verify the trace ID matches

            childScope.currentSpan().tag("child", "true");
        }
    }
}
