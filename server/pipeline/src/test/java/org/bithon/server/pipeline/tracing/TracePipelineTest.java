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

package org.bithon.server.pipeline.tracing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.google.common.collect.ImmutableMap;
import org.bithon.server.pipeline.metrics.input.IMetricInputSourceManager;
import org.bithon.server.pipeline.tracing.exporter.ITraceExporter;
import org.bithon.server.pipeline.tracing.receiver.ITraceReceiver;
import org.bithon.server.storage.tracing.TraceSpan;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Frank Chen
 * @date 25/1/24 10:28 pm
 */
public class TracePipelineTest {

    static ITraceProcessor registeredProcessor;
    static List<TraceSpan> receivedTraceSpanList;

    static class FakeReceiver implements ITraceReceiver {
        @Override
        public void start() {
        }

        @Override
        public void stop() {
        }

        @Override
        public void registerProcessor(ITraceProcessor processor) {
            registeredProcessor = processor;
        }
    }

    static class FakeExporter implements ITraceExporter {
        @Override
        public void process(String messageType, List<TraceSpan> spans) {
            receivedTraceSpanList = spans;
        }

        @Override
        public void close() {
        }
    }

    ObjectMapper objectMapper;

    @Before
    public void beforeTest() {
        objectMapper = new ObjectMapper();
        objectMapper.registerSubtypes(new NamedType(FakeReceiver.class, "fake-receiver"));
        objectMapper.registerSubtypes(new NamedType(FakeExporter.class, "fake-exporter"));

        registeredProcessor = null;
        receivedTraceSpanList = null;
    }

    @Test
    public void test() {

        TracePipelineConfig config = new TracePipelineConfig();
        config.setEnabled(true);
        config.setReceivers(Collections.singletonList(ImmutableMap.of("type", "fake-receiver")));
        config.setExporters(Collections.singletonList(ImmutableMap.of("type", "fake-exporter")));
        TracePipeline pipeline = new TracePipeline(config, Mockito.mock(IMetricInputSourceManager.class), objectMapper);
        pipeline.start();
        Assert.assertNotNull(registeredProcessor);
        Assert.assertNull(receivedTraceSpanList);

        List<TraceSpan> spans = new ArrayList<>();
        TraceSpan span = new TraceSpan();
        spans.add(span);
        registeredProcessor.process("trace", spans);

        Assert.assertEquals(receivedTraceSpanList, spans);
        pipeline.stop();
    }
}
