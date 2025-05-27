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

package org.bithon.server.pipeline.tracing.exporter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.bithon.server.pipeline.common.FixedSizeOutputStream;
import org.bithon.server.storage.tracing.TraceSpan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test cases for TraceSpanListSerializer that reproduce the original deserialization issues
 * and verify that our fix resolves them.
 *
 * @author Frank Chen
 */
public class TraceSpanListSerializerTest {

    private ObjectMapper objectMapper;
    private FixedSizeOutputStream outputStream;
    private List<String> sentBatches;
    private AtomicInteger batchCount;
    private ObjectReader objectReader;

    @BeforeEach
    public void setUp() {
        objectMapper = new ObjectMapper();
        outputStream = new FixedSizeOutputStream(1024); // 1KB buffer for testing
        sentBatches = new ArrayList<>();
        batchCount = new AtomicInteger(0);
        objectReader = objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).readerFor(TraceSpan.class);
    }

    private Consumer<FixedSizeOutputStream> createBatchCollector() {
        return (stream) -> {
            if (stream.size() > 1) {
                sentBatches.add(new String(stream.toBytes()));
                batchCount.incrementAndGet();
            }
        };
    }

    private TraceSpan createTestSpan(String spanId, String name) {
        TraceSpan span = new TraceSpan();
        span.appName = "test-app";
        span.instanceName = "test-instance";
        span.traceId = "trace-123";
        span.spanId = spanId;
        span.parentSpanId = "parent-123";
        span.name = name;
        span.clazz = "com.example.TestClass";
        span.method = "testMethod";
        span.kind = "INTERNAL";
        span.startTime = System.currentTimeMillis() * 1000; // microseconds
        span.endTime = span.startTime + 1000; // 1ms duration
        span.costTime = span.endTime - span.startTime;
        span.tags = new TreeMap<>();
        span.tags.put("tag1", "value1");
        span.tags.put("tag2", "value2");
        return span;
    }

    /**
     * Simulates the consumer's parsing logic to verify that produced messages can be consumed correctly
     */
    private List<TraceSpan> parseMessageLikeConsumer(String message) throws IOException {
        List<TraceSpan> spans = new ArrayList<>();

        try (JsonParser jsonParser = objectReader.createParser(message.getBytes())) {
            JsonToken token = jsonParser.nextToken();
            if (token == JsonToken.START_ARRAY) {
                // JSONArray format
                while (jsonParser.nextToken() == JsonToken.START_OBJECT) {
                    TraceSpan span = objectReader.readValue(jsonParser);
                    spans.add(span);
                }
            } else if (token == JsonToken.START_OBJECT) {
                // JSONEachRow format - this is what our implementation should produce
                MappingIterator<TraceSpan> iterator = objectReader.readValues(jsonParser);
                iterator.forEachRemaining(spans::add);
            }
        }

        return spans;
    }

    @Test
    public void testSerializeSingleSpan() throws IOException {
        // Arrange
        TraceSpan span = createTestSpan("span-1", "test-span");
        ToKafkaExporter.TraceSpanListSerializer serializer = new ToKafkaExporter.TraceSpanListSerializer(
            outputStream, objectMapper, 1000);

        // Act
        serializer.serialize(Collections.singletonList(span), createBatchCollector());

        // Assert
        assertEquals(1, batchCount.get(), "Should send exactly one batch");
        assertEquals(1, sentBatches.size(), "Should have one batch in collection");

        String batch = sentBatches.get(0);
        assertTrue(batch.contains("\"spanId\":\"span-1\""), "Batch should contain span ID");
        assertTrue(batch.contains("\"name\":\"test-span\""), "Batch should contain span name");
        assertTrue(batch.endsWith("\n"), "Batch should end with newline");

        // Verify consumer can parse the message
        List<TraceSpan> parsedSpans = parseMessageLikeConsumer(batch);
        assertEquals(1, parsedSpans.size(), "Consumer should parse exactly one span");
        assertEquals("span-1", parsedSpans.get(0).spanId, "Parsed span should have correct ID");
    }

    @Test
    public void testSerializeMultipleSpans() throws IOException {
        // Arrange
        List<TraceSpan> spans = Arrays.asList(
            createTestSpan("span-1", "span-one"),
            createTestSpan("span-2", "span-two"),
            createTestSpan("span-3", "span-three")
        );
        ToKafkaExporter.TraceSpanListSerializer serializer = new ToKafkaExporter.TraceSpanListSerializer(
            new FixedSizeOutputStream(16384),
            objectMapper,
            1000);

        // Act
        serializer.serialize(spans, createBatchCollector());

        // Assert
        assertEquals(1, batchCount.get(), "Should send exactly one batch");
        String batch = sentBatches.get(0);

        // Count newlines to verify all spans are included
        long newlineCount = batch.chars().filter(ch -> ch == '\n').count();
        assertEquals(3, newlineCount, "Should have 3 newlines for 3 spans");

        // Verify consumer can parse the message
        List<TraceSpan> parsedSpans = parseMessageLikeConsumer(batch);
        assertEquals(3, parsedSpans.size(), "Consumer should parse exactly 3 spans");
        assertEquals("span-1", parsedSpans.get(0).spanId, "First span should have correct ID");
        assertEquals("span-2", parsedSpans.get(1).spanId, "Second span should have correct ID");
        assertEquals("span-3", parsedSpans.get(2).spanId, "Third span should have correct ID");
    }

    @Test
    public void testBufferOverflowProducesValidJson() throws IOException {
        // Arrange - Use a small buffer to force overflow
        FixedSizeOutputStream smallBuffer = new FixedSizeOutputStream(500);
        List<TraceSpan> spans = Arrays.asList(
            createTestSpan("span-id", "span-1"),
            createTestSpan("spand-id-2", "span-2"),
            createTestSpan("spand-id-3", "span-")
        );

        ToKafkaExporter.TraceSpanListSerializer serializer = new ToKafkaExporter.TraceSpanListSerializer(
            smallBuffer, objectMapper, 1000);

        // Act
        serializer.serialize(spans, createBatchCollector());

        // Assert
        assertTrue(batchCount.get() >= 2, "Should send multiple batches due to buffer overflow");

        // Verify each batch contains valid JSON that can be parsed by consumer
        List<TraceSpan> allParsedSpans = new ArrayList<>();
        for (String batch : sentBatches) {
            assertFalse(batch.trim().isEmpty(), "Batch should not be empty");
            assertTrue(batch.endsWith("\n"), "Each batch should end with newline");

            // This is the critical test - consumer should be able to parse each batch
            List<TraceSpan> parsedSpans = assertDoesNotThrow(() -> parseMessageLikeConsumer(batch),
                                                             "Consumer should be able to parse batch without exceptions");

            allParsedSpans.addAll(parsedSpans);
        }

        // Verify all spans were serialized and can be parsed
        assertTrue(allParsedSpans.size() >= 2, "Should have parsed at least 2 spans across all batches");

        // Verify span IDs are present
        boolean hasSpan1 = allParsedSpans.stream().anyMatch(s -> "span-id".equals(s.spanId));
        boolean hasSpan2 = allParsedSpans.stream().anyMatch(s -> "span-id-2".equals(s.spanId));
        assertTrue(hasSpan1 || hasSpan2, "Should have parsed at least one of the large spans");
    }

    @Test
    public void testMaxRowsLimitProducesValidJson() throws IOException {
        // Arrange
        List<TraceSpan> spans = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            spans.add(createTestSpan("span-" + i, "span-" + i));
        }

        // Set max rows to 2, so we should get 3 batches: [2 spans], [2 spans], [1 span]
        ToKafkaExporter.TraceSpanListSerializer serializer = new ToKafkaExporter.TraceSpanListSerializer(
            outputStream, objectMapper, 2);

        // Act
        serializer.serialize(spans, createBatchCollector());

        // Assert
        assertEquals(3, batchCount.get(), "Should send 3 batches due to max rows limit");

        // Verify each batch contains valid JSON that can be parsed by consumer
        List<TraceSpan> allParsedSpans = new ArrayList<>();
        for (int i = 0; i < sentBatches.size(); i++) {
            String batch = sentBatches.get(i);

            // This is the critical test - consumer should be able to parse each batch
            List<TraceSpan> parsedSpans = assertDoesNotThrow(() -> parseMessageLikeConsumer(batch),
                                                             "Consumer should be able to parse batch " + i + " without exceptions");

            allParsedSpans.addAll(parsedSpans);
        }

        // Verify all 5 spans were serialized and can be parsed
        assertEquals(5, allParsedSpans.size(), "Should have parsed all 5 spans across all batches");

        // Verify span IDs
        for (int i = 1; i <= 5; i++) {
            final int spanNum = i;
            boolean hasSpan = allParsedSpans.stream().anyMatch(s -> ("span-" + spanNum).equals(s.spanId));
            assertTrue(hasSpan, "Should have parsed span-" + i);
        }
    }

    @Test
    public void testJsonEachRowFormat() throws IOException {
        // Arrange
        List<TraceSpan> spans = Arrays.asList(
            createTestSpan("span-1", "span-one"),
            createTestSpan("span-2", "span-two")
        );
        ToKafkaExporter.TraceSpanListSerializer serializer = new ToKafkaExporter.TraceSpanListSerializer(
            outputStream, objectMapper, 1000);

        // Act
        serializer.serialize(spans, createBatchCollector());

        // Assert
        String batch = sentBatches.get(0);
        String[] lines = batch.split("\n");

        // Should have 2 non-empty lines (one for each span)
        List<String> nonEmptyLines = Arrays.stream(lines)
                                           .filter(line -> !line.trim().isEmpty())
                                           .toList();
        assertEquals(2, nonEmptyLines.size(), "Should have 2 JSON lines");

        // Each line should be valid JSON
        for (String line : nonEmptyLines) {
            assertDoesNotThrow(() -> objectMapper.readValue(line, TraceSpan.class),
                               "Each line should be valid JSON: " + line);
        }

        // Verify the format matches JSONEachRow expectations
        TraceSpan span1 = objectMapper.readValue(nonEmptyLines.get(0), TraceSpan.class);
        TraceSpan span2 = objectMapper.readValue(nonEmptyLines.get(1), TraceSpan.class);

        assertEquals("span-1", span1.spanId, "First line should contain first span");
        assertEquals("span-2", span2.spanId, "Second line should contain second span");
    }

    @Test
    public void testEmptySpanList() throws IOException {
        // Arrange
        ToKafkaExporter.TraceSpanListSerializer serializer = new ToKafkaExporter.TraceSpanListSerializer(
            outputStream, objectMapper, 1000);

        // Act
        serializer.serialize(Collections.emptyList(), createBatchCollector());

        // Assert
        assertEquals(0, batchCount.get(), "Should not send any batches for empty list");
        assertTrue(sentBatches.isEmpty(), "Should have no batches");
    }

    @Test
    public void testSingleSpanTooLarge() {
        // Arrange
        FixedSizeOutputStream tinyBuffer = new FixedSizeOutputStream(50); // Tiny buffer
        TraceSpan largeSpan = createTestSpan("span-id", "large-span");

        ToKafkaExporter.TraceSpanListSerializer serializer = new ToKafkaExporter.TraceSpanListSerializer(
            tinyBuffer, objectMapper, 1000);

        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> serializer.serialize(Collections.singletonList(largeSpan), createBatchCollector()));

        // The span is too large, so no batches should be sent
        assertEquals(0, batchCount.get(), "Should not send any batches when span is too large");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5, 10, 100})
    public void testVariousMaxRowsLimitsProduceValidJson(int maxRows) throws IOException {
        // Arrange
        List<TraceSpan> spans = new ArrayList<>();
        int totalSpans = 23; // Prime number to test edge cases
        for (int i = 1; i <= totalSpans; i++) {
            spans.add(createTestSpan("span-" + i, "span-" + i));
        }

        ToKafkaExporter.TraceSpanListSerializer serializer = new ToKafkaExporter.TraceSpanListSerializer(
            // Large enough buffer to hold 100 spans
            new FixedSizeOutputStream(16384),
            objectMapper,
            maxRows);

        // Act
        serializer.serialize(spans, createBatchCollector());

        // Assert
        int expectedBatches = (int) Math.ceil((double) totalSpans / maxRows);
        assertEquals(expectedBatches, batchCount.get(),
                     "Should send correct number of batches for maxRows=" + maxRows);

        // Verify all batches contain valid JSON that can be parsed by consumer
        List<TraceSpan> allParsedSpans = new ArrayList<>();
        for (String batch : sentBatches) {
            List<TraceSpan> parsedSpans = assertDoesNotThrow(() -> parseMessageLikeConsumer(batch),
                                                             "Consumer should be able to parse batch without exceptions for maxRows=" + maxRows);
            allParsedSpans.addAll(parsedSpans);
        }

        assertEquals(totalSpans, allParsedSpans.size(), "Total parsed spans should match original for maxRows=" + maxRows);
    }

    @Test
    public void testBufferStateAfterSerialization() throws IOException {
        // Arrange
        TraceSpan span = createTestSpan("span-1", "test-span");
        ToKafkaExporter.TraceSpanListSerializer serializer = new ToKafkaExporter.TraceSpanListSerializer(
            outputStream, objectMapper, 1000);

        // Act
        serializer.serialize(Collections.singletonList(span), createBatchCollector());

        // Assert
        assertEquals(0, outputStream.size(), "Buffer should be empty after serialization");
    }

    @Test
    public void testNullTagsHandling() {
        // Arrange
        TraceSpan span = createTestSpan("span-1", "test-span");
        span.tags = null; // Set tags to null

        ToKafkaExporter.TraceSpanListSerializer serializer = new ToKafkaExporter.TraceSpanListSerializer(
            outputStream, objectMapper, 1000);

        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> serializer.serialize(Collections.singletonList(span), createBatchCollector()));

        assertEquals(1, batchCount.get(), "Should send one batch even with null tags");

        // Verify consumer can parse the message with null tags
        String batch = sentBatches.get(0);
        List<TraceSpan> parsedSpans = assertDoesNotThrow(() -> parseMessageLikeConsumer(batch),
                                                         "Consumer should be able to parse span with null tags");
        assertEquals(1, parsedSpans.size(), "Should parse one span with null tags");
    }
}
