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

package org.bithon.server.collector.jaeger;

import io.jaegertracing.thriftjava.Batch;
import io.jaegertracing.thriftjava.Process;
import io.jaegertracing.thriftjava.Span;
import io.jaegertracing.thriftjava.Tag;
import io.jaegertracing.thriftjava.TagType;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TCompactProtocol;
import org.bithon.server.pipeline.tracing.ITraceProcessor;
import org.bithon.server.storage.tracing.TraceSpan;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test cases for JaegerThriftUDPNettyTraceReceiver
 *
 * @author frank.chen021@outlook.com
 * @date 2025/01/02
 */
public class JaegerThriftUDPTraceReceiverTest {

    private static final int TEST_PORT = 16831; // Use a different port to avoid conflicts
    private static final String TEST_SERVICE_NAME = "test-service";
    private static final String TEST_OPERATION_NAME = "test-operation";

    private JaegerThriftUDPTraceReceiver receiver;
    private TestTraceProcessor testProcessor;
    private TSerializer serializer;

    @BeforeEach
    public void setUp() throws Exception {
        receiver = new JaegerThriftUDPTraceReceiver(TEST_PORT);
        testProcessor = new TestTraceProcessor();
        receiver.registerProcessor(testProcessor);
        serializer = new TSerializer(new TCompactProtocol.Factory());
    }

    @AfterEach
    public void tearDown() {
        if (receiver != null) {
            try {
                receiver.stop();
            } catch (Exception e) {
                // Ignore shutdown exceptions in tests
            }
        }
    }

    @Test
    @Timeout(10)
    public void testReceiveSingleSpan() throws Exception {
        // Start the receiver
        receiver.start();

        // Create test data
        Batch batch = createTestBatch(1);
        byte[] data = serializer.serialize(batch);

        // Send UDP packet
        sendUDPPacket(data);

        // Wait for processing
        assertTrue(testProcessor.waitForSpans(1, 5000), "Should receive 1 span within 5 seconds");

        // Verify received data
        List<TraceSpan> receivedSpans = testProcessor.getReceivedSpans();
        assertEquals(1, receivedSpans.size(), "Should receive exactly 1 span");

        TraceSpan span = receivedSpans.get(0);
        assertEquals(TEST_SERVICE_NAME, span.appName, "App name should match");
        assertEquals(TEST_OPERATION_NAME + "-1", span.name, "Operation name should match");
        assertNotNull(span.traceId, "Trace ID should not be null");
        assertNotNull(span.spanId, "Span ID should not be null");
        assertTrue(span.startTime > 0, "Start time should be positive");
        assertTrue(span.endTime > span.startTime, "End time should be after start time");
        assertTrue(span.costTime > 0, "Cost time should be positive");
    }

    @Test
    @Timeout(10)
    public void testReceiveMultipleSpans() throws Exception {
        // Start the receiver
        receiver.start();

        // Create test data with multiple spans
        Batch batch = createTestBatch(3);
        byte[] data = serializer.serialize(batch);

        // Send UDP packet
        sendUDPPacket(data);

        // Wait for processing
        assertTrue(testProcessor.waitForSpans(3, 5000), "Should receive 3 spans within 5 seconds");

        // Verify received data
        List<TraceSpan> receivedSpans = testProcessor.getReceivedSpans();
        assertEquals(3, receivedSpans.size(), "Should receive exactly 3 spans");

        // Verify all spans have the same service name
        for (TraceSpan span : receivedSpans) {
            assertEquals(TEST_SERVICE_NAME, span.appName, "All spans should have the same app name");
            assertNotNull(span.traceId, "Trace ID should not be null");
            assertNotNull(span.spanId, "Span ID should not be null");
        }
    }

    @Test
    @Timeout(10)
    public void testReceiveMultipleBatches() throws Exception {
        // Start the receiver
        receiver.start();

        // Send multiple batches
        for (int i = 0; i < 3; i++) {
            Batch batch = createTestBatch(2);
            byte[] data = serializer.serialize(batch);
            sendUDPPacket(data);
        }

        // Wait for processing (3 batches * 2 spans each = 6 spans)
        assertTrue(testProcessor.waitForSpans(6, 5000), "Should receive 6 spans within 5 seconds");

        // Verify received data
        List<TraceSpan> receivedSpans = testProcessor.getReceivedSpans();
        assertEquals(6, receivedSpans.size(), "Should receive exactly 6 spans");
    }

    @Test
    @Timeout(10)
    public void testSpanWithTags() throws Exception {
        // Start the receiver
        receiver.start();

        // Create span with tags
        Span span = createTestSpan(1);
        
        // Add various types of tags
        List<Tag> tags = new ArrayList<>();
        tags.add(createTag("string.tag", TagType.STRING, "string-value", null, false, 0L, null));
        tags.add(createTag("bool.tag", TagType.BOOL, null, null, true, 0L, null));
        tags.add(createTag("long.tag", TagType.LONG, null, null, false, 12345L, null));
        tags.add(createTag("double.tag", TagType.DOUBLE, null, 3.14159, false, 0L, null));
        tags.add(createTag("span.kind", TagType.STRING, "server", null, false, 0L, null));
        
        span.setTags(tags);

        Batch batch = new Batch();
        batch.setProcess(createTestProcess());
        batch.setSpans(List.of(span));

        byte[] data = serializer.serialize(batch);

        // Send UDP packet
        sendUDPPacket(data);

        // Wait for processing
        assertTrue(testProcessor.waitForSpans(1, 5000), "Should receive 1 span within 5 seconds");

        // Verify received data
        List<TraceSpan> receivedSpans = testProcessor.getReceivedSpans();
        assertEquals(1, receivedSpans.size(), "Should receive exactly 1 span");

        TraceSpan receivedSpan = receivedSpans.get(0);
        assertNotNull(receivedSpan.tags, "Tags should not be null");
        assertEquals("string-value", receivedSpan.tags.get("string.tag"), "String tag should match");
        assertEquals("true", receivedSpan.tags.get("bool.tag"), "Bool tag should match");
        assertEquals("12345", receivedSpan.tags.get("long.tag"), "Long tag should match");
        assertEquals("3.14159", receivedSpan.tags.get("double.tag"), "Double tag should match");
        assertEquals("SERVER", receivedSpan.kind, "Span kind should be converted to SERVER");
    }

    @Test
    @Timeout(10)
    public void testSpanWithParentChild() throws Exception {
        // Start the receiver
        receiver.start();

        // Create parent and child spans
        Span parentSpan = createTestSpan(1);
        parentSpan.setParentSpanId(0); // Root span

        Span childSpan = createTestSpan(2);
        childSpan.setParentSpanId(parentSpan.getSpanId());

        Batch batch = new Batch();
        batch.setProcess(createTestProcess());
        batch.setSpans(List.of(parentSpan, childSpan));

        byte[] data = serializer.serialize(batch);

        // Send UDP packet
        sendUDPPacket(data);

        // Wait for processing
        assertTrue(testProcessor.waitForSpans(2, 5000), "Should receive 2 spans within 5 seconds");

        // Verify received data
        List<TraceSpan> receivedSpans = testProcessor.getReceivedSpans();
        assertEquals(2, receivedSpans.size(), "Should receive exactly 2 spans");

        // Find parent and child spans
        TraceSpan parent = receivedSpans.stream()
                .filter(s -> s.parentSpanId.isEmpty())
                .findFirst()
                .orElse(null);
        TraceSpan child = receivedSpans.stream()
                .filter(s -> !s.parentSpanId.isEmpty())
                .findFirst()
                .orElse(null);

        assertNotNull(parent, "Should have a parent span");
        assertNotNull(child, "Should have a child span");
        assertEquals(parent.spanId, child.parentSpanId, "Child's parent ID should match parent's span ID");
        assertEquals(parent.traceId, child.traceId, "Both spans should have the same trace ID");
    }

    @Test
    @Timeout(10)
    public void testInvalidData() throws Exception {
        // Start the receiver
        receiver.start();

        // Send invalid data
        byte[] invalidData = "invalid thrift data".getBytes();
        sendUDPPacket(invalidData);

        // Wait a bit to ensure no spans are processed
        Thread.sleep(1000);

        // Verify no spans were received
        assertEquals(0, testProcessor.getReceivedSpans().size(), "Should not receive any spans for invalid data");
    }

    @Test
    @Timeout(10)
    public void testEmptyBatch() throws Exception {
        // Start the receiver
        receiver.start();

        // Create empty batch
        Batch batch = new Batch();
        batch.setProcess(createTestProcess());
        batch.setSpans(new ArrayList<>());

        byte[] data = serializer.serialize(batch);

        // Send UDP packet
        sendUDPPacket(data);

        // Wait a bit to ensure no spans are processed
        Thread.sleep(1000);

        // Verify no spans were received
        assertEquals(0, testProcessor.getReceivedSpans().size(), "Should not receive any spans for empty batch");
    }

    @Test
    public void testStartStop() throws Exception {
        // Test multiple start/stop cycles
        receiver.start();
        assertTrue(true, "Should start without exception");

        receiver.stop();
        assertTrue(true, "Should stop without exception");

        // Test restart
        receiver.start();
        assertTrue(true, "Should restart without exception");

        receiver.stop();
        assertTrue(true, "Should stop again without exception");
    }

    @Test
    @Timeout(10)
    public void testNoProcessorRegistered() throws Exception {
        // Create receiver without processor
        JaegerThriftUDPTraceReceiver receiverWithoutProcessor = new JaegerThriftUDPTraceReceiver(TEST_PORT + 1);
        
        try {
            receiverWithoutProcessor.start();

            // Create test data
            Batch batch = createTestBatch(1);
            byte[] data = serializer.serialize(batch);

            // Send UDP packet to the port without processor
            sendUDPPacket(data, TEST_PORT + 1);

            // Wait a bit
            Thread.sleep(1000);

            // Should not crash or throw exceptions
            assertTrue(true, "Should handle missing processor gracefully");
        } finally {
            receiverWithoutProcessor.stop();
        }
    }

    @Test
    public void testCustomThreadCount() throws Exception {
        // Test with custom thread count
        JaegerThriftUDPTraceReceiver customReceiver = new JaegerThriftUDPTraceReceiver(TEST_PORT + 10, 3);
        TestTraceProcessor customProcessor = new TestTraceProcessor();
        customReceiver.registerProcessor(customProcessor);
        
        try {
            customReceiver.start();
            assertTrue(true, "Should start with custom thread count");
        } finally {
            customReceiver.stop();
        }
    }

    // Helper methods

    private Batch createTestBatch(int spanCount) {
        Batch batch = new Batch();
        batch.setProcess(createTestProcess());

        List<Span> spans = new ArrayList<>();
        for (int i = 1; i <= spanCount; i++) {
            spans.add(createTestSpan(i));
        }
        batch.setSpans(spans);

        return batch;
    }

    private Process createTestProcess() {
        Process process = new Process();
        process.setServiceName(TEST_SERVICE_NAME);
        return process;
    }

    private Span createTestSpan(int id) {
        long currentTime = System.currentTimeMillis() * 1000; // Convert to microseconds
        
        Span span = new Span();
        span.setTraceIdLow(0x1234567890ABCDEFL);
        span.setTraceIdHigh(0xFEDCBA0987654321L);
        span.setSpanId(id);
        span.setParentSpanId(0);
        span.setOperationName(TEST_OPERATION_NAME + "-" + id);
        span.setFlags(1); // Sampled
        span.setStartTime(currentTime);
        span.setDuration(1000); // 1ms duration

        return span;
    }

    private Tag createTag(String key, TagType type, String vStr, Double vDouble, boolean vBool, long vLong, byte[] vBinary) {
        Tag tag = new Tag();
        tag.setKey(key);
        tag.setVType(type);
        if (vStr != null) {
            tag.setVStr(vStr);
        }
        if (vDouble != null) {
            tag.setVDouble(vDouble);
        }
        tag.setVBool(vBool);
        tag.setVLong(vLong);
        if (vBinary != null) {
            tag.setVBinary(vBinary);
        }
        return tag;
    }

    private void sendUDPPacket(byte[] data) throws Exception {
        sendUDPPacket(data, TEST_PORT);
    }

    private void sendUDPPacket(byte[] data, int port) throws Exception {
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress address = InetAddress.getByName("localhost");
            DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
            socket.send(packet);
        }
    }

    /**
     * Custom ITraceProcessor implementation for testing
     */
    private static class TestTraceProcessor implements ITraceProcessor {
        private final List<TraceSpan> receivedSpans = new ArrayList<>();
        private final AtomicReference<CountDownLatch> latchRef = new AtomicReference<>();

        @Override
        public void process(String messageType, List<TraceSpan> spans) {
            synchronized (receivedSpans) {
                receivedSpans.addAll(spans);
                
                CountDownLatch latch = latchRef.get();
                if (latch != null) {
                    for (int i = 0; i < spans.size(); i++) {
                        latch.countDown();
                    }
                }
            }
        }

        @Override
        public void close() {
        }

        public List<TraceSpan> getReceivedSpans() {
            synchronized (receivedSpans) {
                return new ArrayList<>(receivedSpans);
            }
        }

        public boolean waitForSpans(int expectedCount, long timeoutMs) throws InterruptedException {
            CountDownLatch latch = new CountDownLatch(expectedCount);
            latchRef.set(latch);
            
            try {
                return latch.await(timeoutMs, TimeUnit.MILLISECONDS);
            } finally {
                latchRef.set(null);
            }
        }
    }
}
