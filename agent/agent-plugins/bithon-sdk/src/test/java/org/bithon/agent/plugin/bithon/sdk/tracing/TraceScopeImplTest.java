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

package org.bithon.agent.plugin.bithon.sdk.tracing;


import org.bithon.agent.configuration.ConfigurationManager;
import org.bithon.agent.observability.tracing.Tracer;
import org.bithon.agent.observability.tracing.config.TraceConfig;
import org.bithon.agent.observability.tracing.context.ITraceContext;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceContextHolder;
import org.bithon.agent.observability.tracing.context.TraceMode;
import org.bithon.agent.observability.tracing.context.propagation.PropagationSetter;
import org.bithon.agent.observability.tracing.id.ISpanIdGenerator;
import org.bithon.agent.observability.tracing.id.impl.DefaultSpanIdGenerator;
import org.bithon.agent.observability.tracing.reporter.ITraceReporter;
import org.bithon.agent.observability.tracing.reporter.ReporterConfig;
import org.bithon.agent.plugin.bithon.sdk.tracing.interceptor.TraceScopeBuilder$Attach;
import org.bithon.agent.plugin.bithon.sdk.tracing.interceptor.TraceScopeBuilder$AttachOrReplaceCurrent;
import org.bithon.agent.sdk.tracing.ITraceScope;
import org.bithon.agent.sdk.tracing.TraceContext;
import org.bithon.agent.sdk.tracing.impl.NoopTraceScope;
import org.bithon.component.commons.time.Clock;
import org.bithon.component.commons.tracing.SpanKind;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2026/05/22
 */
public class TraceScopeImplTest {
    private Tracer originalTracer;

    @BeforeEach
    public void setUp() throws Exception {
        ConfigurationManager.createForTesting();
        originalTracer = setTracer(new Tracer("test", "test")
                                       .traceConfig(new TraceConfig())
                                       .spanIdGenerator(new DefaultSpanIdGenerator())
                                       .reporter(new ITraceReporter() {
                                           @Override
                                           public ReporterConfig getReporterConfig() {
                                               return new ReporterConfig();
                                           }

                                           @Override
                                           public void report(List<ITraceSpan> spans) {
                                           }
                                       }));
    }

    @AfterEach
    public void tearDown() throws Exception {
        TraceContextHolder.detach();
        setTracer(originalTracer);
    }

    @Test
    public void testCloseClearsCurrentContextWhenNoPreviousContextExists() {
        TestTraceContext current = new TestTraceContext("current");

        new TraceScopeImpl(current, current.currentSpan()).close();

        Assertions.assertNull(TraceContextHolder.current());
        Assertions.assertTrue(current.finished);
        Assertions.assertTrue(current.span.finished);
    }

    @Test
    public void testCloseRestoresPreviousContext() {
        TestTraceContext previous = new TestTraceContext("previous");
        TraceContextHolder.attach(previous);

        TestTraceContext current = new TestTraceContext("current");
        TraceScopeImpl scope = new TraceScopeImpl(current, current.currentSpan(), previous);
        Assertions.assertSame(current, TraceContextHolder.current());

        scope.close();

        Assertions.assertSame(previous, TraceContextHolder.current());
        Assertions.assertFalse(previous.finished);
        Assertions.assertFalse(previous.span.finished);
        Assertions.assertTrue(current.finished);
        Assertions.assertTrue(current.span.finished);
    }

    @Test
    public void testCloseRestoresPreviousContextWhenCurrentContextFinishThrows() {
        TestTraceContext previous = new TestTraceContext("previous");
        TraceContextHolder.attach(previous);

        RuntimeException finishException = new RuntimeException("finish failed");
        TestTraceContext current = new TestTraceContext("current");
        current.finishException = finishException;
        TraceScopeImpl scope = new TraceScopeImpl(current, current.currentSpan(), previous);

        Assertions.assertSame(finishException, Assertions.assertThrows(RuntimeException.class, scope::close));
        Assertions.assertSame(previous, TraceContextHolder.current());
        Assertions.assertFalse(previous.finished);
        Assertions.assertFalse(previous.span.finished);
        Assertions.assertTrue(current.finished);
        Assertions.assertTrue(current.span.finished);
    }

    @Test
    public void testAttachOrReplaceCurrentReturnsNoopWhenParentContextIsIncomplete() {
        TestTraceContext previous = new TestTraceContext("previous");
        TraceContextHolder.attach(previous);

        TraceScopeBuilder$AttachOrReplaceCurrent interceptor = new TraceScopeBuilder$AttachOrReplaceCurrent();

        Assertions.assertSame(NoopTraceScope.INSTANCE,
                              interceptor.execute(TraceContext.newTrace("missing-parent"), null, null));
        Assertions.assertSame(NoopTraceScope.INSTANCE,
                              interceptor.execute(TraceContext.newTrace("missing-trace").parent("", "0000000000000001"),
                                                  null,
                                                  null));
        Assertions.assertSame(NoopTraceScope.INSTANCE,
                              interceptor.execute(TraceContext.newTrace("missing-parent").parent("00000000000000000000000000000001", ""),
                                                  null,
                                                  null));
        Assertions.assertSame(previous, TraceContextHolder.current());
        Assertions.assertFalse(previous.finished);
        Assertions.assertFalse(previous.span.finished);
    }

    @Test
    public void testAttachOrReplaceCurrentReplacesAndRestoresPreviousContext() {
        TestTraceContext previous = new TestTraceContext("previous");
        TraceContextHolder.attach(previous);

        TraceScopeBuilder$AttachOrReplaceCurrent interceptor = new TraceScopeBuilder$AttachOrReplaceCurrent();
        ITraceScope scope = (ITraceScope) interceptor.execute(TraceContext.newTrace("current")
                                                                          .parent("00000000000000000000000000000001",
                                                                                  "0000000000000002"),
                                                              null,
                                                              null);

        ITraceContext current = TraceContextHolder.current();
        Assertions.assertNotSame(previous, current);
        Assertions.assertEquals("00000000000000000000000000000001", current.traceId());
        Assertions.assertEquals("0000000000000002", current.currentSpan().parentSpanId());
        Assertions.assertEquals("current", current.currentSpan().name());

        scope.close();

        Assertions.assertSame(previous, TraceContextHolder.current());
        Assertions.assertFalse(previous.finished);
        Assertions.assertFalse(previous.span.finished);
    }

    @Test
    public void testAttachAppliesRootSpanKind() {
        TraceScopeBuilder$Attach interceptor = new TraceScopeBuilder$Attach();
        ITraceScope scope = (ITraceScope) interceptor.execute(TraceContext.newTrace("current")
                                                                          .kind(org.bithon.agent.sdk.tracing.SpanKind.SERVER),
                                                              null,
                                                              null);

        ITraceContext current = TraceContextHolder.current();
        Assertions.assertEquals("current", current.currentSpan().name());
        Assertions.assertEquals(SpanKind.SERVER, current.currentSpan().kind());

        scope.close();

        Assertions.assertNull(TraceContextHolder.current());
    }

    private static Tracer setTracer(Tracer tracer) throws Exception {
        Field field = Tracer.class.getDeclaredField("INSTANCE");
        field.setAccessible(true);
        Tracer previous = (Tracer) field.get(null);
        field.set(null, tracer);
        return previous;
    }

    private static class TestTraceContext implements ITraceContext {
        private final Clock clock = new Clock();
        private final ISpanIdGenerator spanIdGenerator = () -> "0000000000000001";
        private final String traceId;
        private final TestTraceSpan span;
        private boolean finished;
        private RuntimeException finishException;

        private TestTraceContext(String traceId) {
            this.traceId = traceId;
            this.span = new TestTraceSpan(this);
        }

        @Override
        public TraceMode traceMode() {
            return TraceMode.TRACING;
        }

        @Override
        public String traceId() {
            return traceId;
        }

        @Override
        public ITraceSpan currentSpan() {
            return span;
        }

        @Override
        public Clock clock() {
            return clock;
        }

        @Override
        public ISpanIdGenerator spanIdGenerator() {
            return spanIdGenerator;
        }

        @Override
        public ITraceSpan newSpan(String parentSpanId, String spanId) {
            return span;
        }

        @Override
        public void finish() {
            finished = true;
            if (finishException != null) {
                throw finishException;
            }
        }

        @Override
        public <T> void propagate(T injectedTo, PropagationSetter<T> setter) {
        }

        @Override
        public ITraceContext copy() {
            return new TestTraceContext(traceId);
        }

        @Override
        public boolean finished() {
            return finished;
        }
    }

    private static class TestTraceSpan implements ITraceSpan {
        private final ITraceContext context;
        private boolean finished;

        private TestTraceSpan(ITraceContext context) {
            this.context = context;
        }

        @Override
        public ITraceContext context() {
            return context;
        }

        @Override
        public String spanId() {
            return "0000000000000001";
        }

        @Override
        public String parentSpanId() {
            return "";
        }

        @Override
        public SpanKind kind() {
            return SpanKind.INTERNAL;
        }

        @Override
        public ITraceSpan kind(SpanKind kind) {
            return this;
        }

        @Override
        public String name() {
            return "test";
        }

        @Override
        public ITraceSpan name(String name) {
            return this;
        }

        @Override
        public Map<String, String> tags() {
            return Collections.emptyMap();
        }

        @Override
        public ITraceSpan tag(String name, String value) {
            return this;
        }

        @Override
        public ITraceSpan tag(String name, SocketAddress address) {
            return this;
        }

        @Override
        public ITraceSpan tag(String name, InetSocketAddress address) {
            return this;
        }

        @Override
        public ITraceSpan tag(String name, int value) {
            return this;
        }

        @Override
        public ITraceSpan tag(String name, long value) {
            return this;
        }

        @Override
        public ITraceSpan tag(String name, Object value) {
            return this;
        }

        @Override
        public ITraceSpan tag(Throwable exception) {
            return this;
        }

        @Override
        public String parentApplication() {
            return null;
        }

        @Override
        public ITraceSpan parentApplication(String sourceApp) {
            return this;
        }

        @Override
        public String clazz() {
            return "";
        }

        @Override
        public String method() {
            return "";
        }

        @Override
        public ITraceSpan method(Executable method) {
            return this;
        }

        @Override
        public ITraceSpan method(Class<?> clazz, String method) {
            return this;
        }

        @Override
        public ITraceSpan method(String clazz, String method) {
            return this;
        }

        @Override
        public ITraceSpan clazz(String clazz) {
            return this;
        }

        @Override
        public long startTime() {
            return 0;
        }

        @Override
        public long endTime() {
            return 0;
        }

        @Override
        public ITraceSpan newChildSpan(String name) {
            return this;
        }

        @Override
        public ITraceSpan start() {
            return this;
        }

        @Override
        public void finish() {
            finished = true;
        }
    }
}
