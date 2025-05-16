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
import org.bithon.agent.observability.tracing.context.propagation.w3c.W3CTraceContextExtractor;
import org.bithon.agent.observability.tracing.context.propagation.w3c.W3CTraceContextHeader;
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
 * @date 2024/8/29 22:03
 */
public class W3CTraceContextExtractorTest {
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
    public void testExtract_ValidTraceParent() {
        W3CTraceContextExtractor extractor = new W3CTraceContextExtractor();
        Map<String, String> headers = ImmutableMap.of(
            W3CTraceContextHeader.TRACE_HEADER_PARENT, "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01",
            W3CTraceContextHeader.TRACE_HEADER_STATE, "key1=v1, key2=v2"
        );

        ITraceContext context = extractor.extract(headers, new TestPropagationGetter());
        Assertions.assertNotNull(context);
        Assertions.assertEquals("4bf92f3577b34da6a3ce929d0e0e4736", context.traceId());
        Assertions.assertEquals("00f067aa0ba902b7", context.currentSpan().parentSpanId());
        Assertions.assertEquals(TraceMode.TRACING, context.traceMode());
        Assertions.assertEquals("v1", context.traceState().get("key1"));
        Assertions.assertEquals("v2", context.traceState().get("key2"));
    }

    @Test
    public void testExtract_MissingTraceParent() {
        W3CTraceContextExtractor extractor = new W3CTraceContextExtractor();

        Assertions.assertNull(extractor.extract(Collections.emptyMap(),
                                            TestPropagationGetter.INSTANCE));
    }

    @Test
    public void testExtract_InvalidTraceParentFormat() {
        W3CTraceContextExtractor extractor = new W3CTraceContextExtractor();

        Assertions.assertNull(extractor.extract(ImmutableMap.of(W3CTraceContextHeader.TRACE_HEADER_PARENT, "invalid-format"),
                                            TestPropagationGetter.INSTANCE));
    }

    @Test
    public void testExtract_InvalidTraceId() {
        W3CTraceContextExtractor extractor = new W3CTraceContextExtractor();

        Assertions.assertNull(extractor.extract(ImmutableMap.of(W3CTraceContextHeader.TRACE_HEADER_PARENT, "00-invalidtraceid-00f067aa0ba902b7-01"),
                                            TestPropagationGetter.INSTANCE));

        // Length of trace id is not 32
        Assertions.assertNull(extractor.extract(ImmutableMap.of(W3CTraceContextHeader.TRACE_HEADER_PARENT, "00-4bf92f3577b34d-00f067aa0ba902b7-01"),
                                            TestPropagationGetter.INSTANCE));
        Assertions.assertNull(extractor.extract(ImmutableMap.of(W3CTraceContextHeader.TRACE_HEADER_PARENT, "00-4bf92f3577b34da6a3ce929d0e0e4736A-00f067aa0ba902b7-01"),
                                            TestPropagationGetter.INSTANCE));
    }

    @Test
    public void testExtract_InvalidParentSpanId() {
        W3CTraceContextExtractor extractor = new W3CTraceContextExtractor();

        Assertions.assertNull(extractor.extract(ImmutableMap.of(W3CTraceContextHeader.TRACE_HEADER_PARENT, "00-4bf92f3577b34da6a3ce929d0e0e4736-invalidspanid-01"),
                                            TestPropagationGetter.INSTANCE));

        Assertions.assertNull(extractor.extract(ImmutableMap.of(W3CTraceContextHeader.TRACE_HEADER_PARENT, "00-4bf92f3577b34da6a3ce929d0e0e4736--01"),
                                            TestPropagationGetter.INSTANCE));

        // The length of parent id is not 16
        Assertions.assertNull(extractor.extract(ImmutableMap.of(W3CTraceContextHeader.TRACE_HEADER_PARENT, "00-4bf92f3577b34da6a3ce929d0e0e4736-00f0-01"),
                                            TestPropagationGetter.INSTANCE));
        Assertions.assertNull(extractor.extract(ImmutableMap.of(W3CTraceContextHeader.TRACE_HEADER_PARENT, "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7A-01"),
                                            TestPropagationGetter.INSTANCE));
    }

    @Test
    public void testExtract_InvalidTraceFlags() {
        W3CTraceContextExtractor extractor = new W3CTraceContextExtractor();

        Assertions.assertNull(extractor.extract(ImmutableMap.of(W3CTraceContextHeader.TRACE_HEADER_PARENT, "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-invalidflags"),
                                            TestPropagationGetter.INSTANCE));

        Assertions.assertNull(extractor.extract(ImmutableMap.of(W3CTraceContextHeader.TRACE_HEADER_PARENT, "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-001"),
                                            TestPropagationGetter.INSTANCE));

        Assertions.assertNull(extractor.extract(ImmutableMap.of(W3CTraceContextHeader.TRACE_HEADER_PARENT, "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-"),
                                            TestPropagationGetter.INSTANCE));
    }
}
