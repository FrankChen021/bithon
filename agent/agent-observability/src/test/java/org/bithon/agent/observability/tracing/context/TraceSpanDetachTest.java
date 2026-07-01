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

package org.bithon.agent.observability.tracing.context;

import org.bithon.agent.configuration.ConfigurationManager;
import org.bithon.agent.observability.tracing.Tracer;
import org.bithon.agent.observability.tracing.config.TraceConfig;
import org.bithon.agent.observability.tracing.id.impl.DefaultSpanIdGenerator;
import org.bithon.agent.observability.tracing.reporter.ITraceReporter;
import org.bithon.agent.observability.tracing.reporter.ReporterConfig;
import org.bithon.agent.observability.tracing.sampler.SamplingMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

public class TraceSpanDetachTest {

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
    public void testDetachReturnsCopiedContextEvenWhenSpanIsNotStackTop() {
        ITraceContext context = TraceContextFactory.newContext(SamplingMode.FULL);
        ITraceSpan rootSpan = context.currentSpan().name("root");
        ITraceSpan childSpan = rootSpan.newChildSpan("child");
        ITraceSpan grandchildSpan = childSpan.newChildSpan("grandchild");

        ITraceSpan detachedSpan = childSpan.detach();

        Assertions.assertNotSame(childSpan, detachedSpan);
        Assertions.assertNotSame(childSpan.context(), detachedSpan.context());
        Assertions.assertEquals(childSpan.traceId(), detachedSpan.traceId());
        Assertions.assertEquals(childSpan.spanId(), detachedSpan.spanId());
        Assertions.assertEquals(childSpan.parentSpanId(), detachedSpan.parentSpanId());
        Assertions.assertSame(detachedSpan, detachedSpan.context().currentSpan());

        detachedSpan.finish();
        detachedSpan.context().finish();

        grandchildSpan.finish();
        childSpan.finish();
        rootSpan.finish();
        context.finish();
    }

    private static Tracer setTracer(Tracer tracer) throws Exception {
        Field field = Tracer.class.getDeclaredField("INSTANCE");
        field.setAccessible(true);
        Tracer previous = (Tracer) field.get(null);
        field.set(null, tracer);
        return previous;
    }
}
