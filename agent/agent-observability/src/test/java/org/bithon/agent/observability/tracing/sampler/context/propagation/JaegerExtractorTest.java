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

package org.bithon.agent.observability.tracing.sampler.context.propagation;

import com.google.common.collect.ImmutableMap;
import org.bithon.agent.configuration.ConfigurationManager;
import org.bithon.agent.configuration.source.Helper;
import org.bithon.agent.observability.tracing.Tracer;
import org.bithon.agent.observability.tracing.context.ITraceContext;
import org.bithon.agent.observability.tracing.context.TraceMode;
import org.bithon.agent.observability.tracing.context.propagation.PropagationGetter;
import org.bithon.agent.observability.tracing.context.propagation.jaeger.JaegerExtractor;
import org.bithon.agent.observability.tracing.id.impl.DefaultSpanIdGenerator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.util.Collections;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2025/05/26
 */
public class JaegerExtractorTest {
    private static class TestPropagationGetter implements PropagationGetter<Map<String, String>> {
        public static final TestPropagationGetter INSTANCE = new TestPropagationGetter();

        @Override
        public String get(Map<String, String> carrier, String key) {
            return carrier.get(key);
        }
    }

    private static MockedStatic<Helper> configurationMock;
    private static MockedStatic<Tracer> mockTracer;

    @BeforeAll
    public static void setUp() {
        // mock to create ConfigurationManager
        configurationMock = Mockito.mockStatic(Helper.class);
        configurationMock.when(Helper::getCommandLineInputArgs).thenReturn(Collections.emptyList());
        configurationMock.when(Helper::getEnvironmentVariables).thenReturn(Collections.emptyMap());
        ConfigurationManager.createForTesting(new File("/"));

        // mock Tracer.get()
        mockTracer = Mockito.mockStatic(Tracer.class);
        mockTracer.when(Tracer::get).thenReturn(new Tracer("test", "test").spanIdGenerator(new DefaultSpanIdGenerator()));
    }

    @AfterAll
    public static void tearDown() {
        configurationMock.close();
        mockTracer.close();
    }

    @Test
    public void testExtract_ValidUberTraceId() {
        JaegerExtractor extractor = new JaegerExtractor();
        Map<String, String> headers = ImmutableMap.of(
            JaegerExtractor.UBER_TRACE_ID, "4bf92f3577b34da6a3ce929d0e0e4736:00f067aa0ba902b7:b7ad6b7169203331:01"
        );

        ITraceContext context = extractor.extract(headers, TestPropagationGetter.INSTANCE);
        Assertions.assertNotNull(context);
        Assertions.assertEquals("4bf92f3577b34da6a3ce929d0e0e4736", context.traceId());
        Assertions.assertEquals("00f067aa0ba902b7", context.currentSpan().parentSpanId());
        Assertions.assertEquals(TraceMode.TRACING, context.traceMode());
    }

    @Test
    public void testExtract_ValidUberTraceId_NotSampled() {
        JaegerExtractor extractor = new JaegerExtractor();
        Map<String, String> headers = ImmutableMap.of(
            JaegerExtractor.UBER_TRACE_ID, "4bf92f3577b34da6a3ce929d0e0e4736:00f067aa0ba902b7:b7ad6b7169203331:00"
        );

        ITraceContext context = extractor.extract(headers, TestPropagationGetter.INSTANCE);
        Assertions.assertNotNull(context);
        Assertions.assertEquals("4bf92f3577b34da6a3ce929d0e0e4736", context.traceId());
        Assertions.assertEquals("00f067aa0ba902b7", context.currentSpan().parentSpanId());
        Assertions.assertEquals(TraceMode.LOGGING, context.traceMode());
    }

    @Test
    public void testExtract_ValidUberTraceId_RootSpan() {
        JaegerExtractor extractor = new JaegerExtractor();
        Map<String, String> headers = ImmutableMap.of(
            JaegerExtractor.UBER_TRACE_ID, "4bf92f3577b34da6a3ce929d0e0e4736:00f067aa0ba902b7:0:01"
        );

        ITraceContext context = extractor.extract(headers, TestPropagationGetter.INSTANCE);
        Assertions.assertNotNull(context);
        Assertions.assertEquals("4bf92f3577b34da6a3ce929d0e0e4736", context.traceId());
        Assertions.assertEquals("00f067aa0ba902b7", context.currentSpan().parentSpanId());
        Assertions.assertEquals(TraceMode.TRACING, context.traceMode());
    }

    @Test
    public void testExtract_ValidUberTraceId_AllZeroParentSpan() {
        JaegerExtractor extractor = new JaegerExtractor();
        Map<String, String> headers = ImmutableMap.of(
            JaegerExtractor.UBER_TRACE_ID, "4bf92f3577b34da6a3ce929d0e0e4736:00f067aa0ba902b7:0000000000000000:01"
        );

        ITraceContext context = extractor.extract(headers, TestPropagationGetter.INSTANCE);
        Assertions.assertNotNull(context);
        Assertions.assertEquals("4bf92f3577b34da6a3ce929d0e0e4736", context.traceId());
        Assertions.assertEquals("00f067aa0ba902b7", context.currentSpan().parentSpanId());
        Assertions.assertEquals(TraceMode.TRACING, context.traceMode());
    }

    @Test
    public void testExtract_MissingUberTraceId() {
        JaegerExtractor extractor = new JaegerExtractor();

        Assertions.assertNull(extractor.extract(Collections.emptyMap(), TestPropagationGetter.INSTANCE));
    }

    @Test
    public void testExtract_InvalidUberTraceIdFormat() {
        JaegerExtractor extractor = new JaegerExtractor();

        // Wrong number of parts
        Assertions.assertNull(extractor.extract(ImmutableMap.of(JaegerExtractor.UBER_TRACE_ID, "invalid-format"),
                                                TestPropagationGetter.INSTANCE));

        // Missing parts
        Assertions.assertNull(extractor.extract(ImmutableMap.of(JaegerExtractor.UBER_TRACE_ID, "trace:span:parent"),
                                                TestPropagationGetter.INSTANCE));

        // Too many parts
        Assertions.assertNull(extractor.extract(ImmutableMap.of(JaegerExtractor.UBER_TRACE_ID, "trace:span:parent:flags:extra"),
                                                TestPropagationGetter.INSTANCE));
    }

    @Test
    public void testExtract_InvalidTraceId() {
        JaegerExtractor extractor = new JaegerExtractor();

        // Empty trace ID
        Assertions.assertNull(extractor.extract(ImmutableMap.of(JaegerExtractor.UBER_TRACE_ID, ":00f067aa0ba902b7:b7ad6b7169203331:01"),
                                                TestPropagationGetter.INSTANCE));

        // All zeros trace ID
        Assertions.assertNull(extractor.extract(ImmutableMap.of(JaegerExtractor.UBER_TRACE_ID, "00000000000000000000000000000000:00f067aa0ba902b7:b7ad6b7169203331:01"),
                                                TestPropagationGetter.INSTANCE));
    }

    @Test
    public void testExtract_InvalidSpanId() {
        JaegerExtractor extractor = new JaegerExtractor();

        // Empty span ID
        Assertions.assertNull(extractor.extract(ImmutableMap.of(JaegerExtractor.UBER_TRACE_ID, "4bf92f3577b34da6a3ce929d0e0e4736::b7ad6b7169203331:01"),
                                                TestPropagationGetter.INSTANCE));

        // All zeros span ID
        Assertions.assertNull(extractor.extract(ImmutableMap.of(JaegerExtractor.UBER_TRACE_ID, "4bf92f3577b34da6a3ce929d0e0e4736:0000000000000000:b7ad6b7169203331:01"),
                                                TestPropagationGetter.INSTANCE));
    }

    @Test
    public void testExtract_InvalidGrandParentSpanId() {
        JaegerExtractor extractor = new JaegerExtractor();

        // We don't care the grandparent span ID
        Assertions.assertNotNull(extractor.extract(ImmutableMap.of(JaegerExtractor.UBER_TRACE_ID, "4bf92f3577b34da6a3ce929d0e0e4736:00f067aa0ba902b7::01"),
                                                   TestPropagationGetter.INSTANCE));
    }

    @Test
    public void testExtract_InvalidFlags() {
        JaegerExtractor extractor = new JaegerExtractor();

        // Non-hex flags should still work, just default to not sampled
        ITraceContext context = extractor.extract(ImmutableMap.of(JaegerExtractor.UBER_TRACE_ID, "4bf92f3577b34da6a3ce929d0e0e4736:00f067aa0ba902b7:b7ad6b7169203331:invalid"),
                                                  TestPropagationGetter.INSTANCE);
        Assertions.assertNotNull(context);
        Assertions.assertEquals(TraceMode.LOGGING, context.traceMode());
    }
} 
