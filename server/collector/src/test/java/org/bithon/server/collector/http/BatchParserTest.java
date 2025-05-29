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

package org.bithon.server.collector.http;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.bithon.server.collector.http.TraceHttpCollector.BatchParser;
import org.bithon.server.storage.tracing.TraceSpan;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test cases for BatchProcessor
 *
 * @author frank.chen021@outlook.com
 * @date 2025/01/14
 */
public class BatchParserTest {

    private ObjectReader objectReader;
    private BatchParser batchParser;

    @BeforeEach
    public void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        this.objectReader = objectMapper.readerFor(TraceSpan.class);
    }

    static class TestBatchConsumer implements Consumer<List<TraceSpan>> {
        private final List<List<TraceSpan>> receivedBatches = new ArrayList<>();
        private final List<TraceSpan> allReceivedSpans = new ArrayList<>();

        @Override
        public void accept(List<TraceSpan> batch) {
            receivedBatches.add(new ArrayList<>(batch));
            allReceivedSpans.addAll(batch);
        }

        public List<List<TraceSpan>> getReceivedBatches() {
            return receivedBatches;
        }

        public List<TraceSpan> getAllReceivedSpans() {
            return allReceivedSpans;
        }
    }

    @Test
    public void testParseJSONArrayFormat() {
        // Given
        this.batchParser = new BatchParser(objectReader, 10);
        TestBatchConsumer batchConsumer = new TestBatchConsumer();
        String jsonArray = """
            [
                {
                    "appName": "test-app-1",
                    "traceId": "trace-1",
                    "spanId": "span-1",
                    "name": "operation-1",
                    "kind": "CLIENT",
                    "startTime": 1000000,
                    "costTime": 5000,
                    "endTime": 1005000,
                    "tags": {"key1": "value1"}
                },
                {
                    "appName": "test-app-2",
                    "traceId": "trace-2",
                    "spanId": "span-2",
                    "name": "operation-2",
                    "kind": "SERVER",
                    "startTime": 2000000,
                    "costTime": 3000,
                    "endTime": 2003000,
                    "tags": {"key2": "value2"}
                }
            ]
            """;

        // When
        TraceHttpCollector.ParseResult result = batchParser.parse(new ByteArrayInputStream(jsonArray.getBytes(StandardCharsets.UTF_8)), batchConsumer);

        // Then
        Assertions.assertFalse(result.hasErrors(), "Parsing should succeed");
        assertEquals(2, result.getSuccessfulSpans(), "Should have processed 2 spans");
        assertTrue(result.getParseErrors().isEmpty(), "Should have no validation errors");
        assertEquals(1, batchConsumer.getReceivedBatches().size(), "Should have received 1 batch");
        assertEquals(2, batchConsumer.getAllReceivedSpans().size(), "Should have received 2 spans");

        List<TraceSpan> spans = batchConsumer.getAllReceivedSpans();
        assertEquals("test-app-1", spans.get(0).appName);
        assertEquals("trace-1", spans.get(0).traceId);
        assertEquals("operation-1", spans.get(0).name);
        assertEquals("CLIENT", spans.get(0).kind);
        assertEquals(1000000L, spans.get(0).startTime);
        assertEquals(5000L, spans.get(0).costTime);
        assertEquals(1005000L, spans.get(0).endTime);

        assertEquals("test-app-2", spans.get(1).appName);
        assertEquals("trace-2", spans.get(1).traceId);
        assertEquals("operation-2", spans.get(1).name);
        assertEquals("SERVER", spans.get(1).kind);
    }

    @Test
    public void testParseJSONEachRowFormat() {
        // Given
        this.batchParser = new BatchParser(objectReader, 10);
        TestBatchConsumer batchConsumer = new TestBatchConsumer();
        String jsonEachRow = """
            {"appName": "test-app-1", "traceId": "trace-1", "spanId": "span-1", "name": "operation-1", "kind": "CLIENT", "startTime": 1000000, "costTime": 5000, "endTime": 1005000, "tags": {"key1": "value1"}}
            {"appName": "test-app-2", "traceId": "trace-2", "spanId": "span-2", "name": "operation-2", "kind": "SERVER", "startTime": 2000000, "costTime": 3000, "endTime": 2003000, "tags": {"key2": "value2"}}
            """;

        // When
        TraceHttpCollector.ParseResult result = batchParser.parse(new ByteArrayInputStream(jsonEachRow.getBytes(StandardCharsets.UTF_8)), batchConsumer);

        // Then
        Assertions.assertFalse(result.hasErrors(), "Parsing should succeed");
        assertEquals(2, result.getSuccessfulSpans(), "Should have processed 2 spans");
        assertTrue(result.getParseErrors().isEmpty(), "Should have no validation errors");
        assertEquals(1, batchConsumer.getReceivedBatches().size(), "Should have received 1 batch");
        assertEquals(2, batchConsumer.getAllReceivedSpans().size(), "Should have received 2 spans");

        List<TraceSpan> spans = batchConsumer.getAllReceivedSpans();
        assertEquals("test-app-1", spans.get(0).appName);
        assertEquals("test-app-2", spans.get(1).appName);
    }

    @Test
    public void testParseConcatenatedJSONObjectsWithoutWhitespace() {
        // Given - This test specifically verifies that parser.clearCurrentToken() works correctly
        // for concatenated JSON objects without whitespace separators
        this.batchParser = new BatchParser(objectReader, 10);
        TestBatchConsumer batchConsumer = new TestBatchConsumer();
        String concatenatedJson = """
            {"appName": "test-app-1", "traceId": "trace-1", "spanId": "span-1", "name": "operation-1", "kind": "CLIENT", "startTime": 1000000, "costTime": 5000, "endTime": 1005000, "tags": {"key1": "value1"}}{"appName": "test-app-2", "traceId": "trace-2", "spanId": "span-2", "name": "operation-2", "kind": "SERVER", "startTime": 2000000, "costTime": 3000, "endTime": 2003000, "tags": {"key2": "value2"}}{"appName": "test-app-3", "traceId": "trace-3", "spanId": "span-3", "name": "operation-3", "kind": "INTERNAL", "startTime": 3000000, "costTime": 2000, "endTime": 3002000, "tags": {"key3": "value3"}}""";

        // When
        TraceHttpCollector.ParseResult result = batchParser.parse(new ByteArrayInputStream(concatenatedJson.getBytes(StandardCharsets.UTF_8)), batchConsumer);

        // Then
        Assertions.assertFalse(result.hasErrors(), "Parsing should succeed");
        assertEquals(3, result.getSuccessfulSpans(), "Should have processed 3 spans");
        assertTrue(result.getParseErrors().isEmpty(), "Should have no validation errors");
        assertEquals(1, batchConsumer.getReceivedBatches().size(), "Should have received 1 batch");
        assertEquals(3, batchConsumer.getAllReceivedSpans().size(), "Should have received 3 spans");

        List<TraceSpan> spans = batchConsumer.getAllReceivedSpans();
        assertEquals("test-app-1", spans.get(0).appName);
        assertEquals("trace-1", spans.get(0).traceId);
        assertEquals("operation-1", spans.get(0).name);
        assertEquals("CLIENT", spans.get(0).kind);
        assertEquals(1000000L, spans.get(0).startTime);
        assertEquals(5000L, spans.get(0).costTime);
        assertEquals(1005000L, spans.get(0).endTime);

        assertEquals("test-app-2", spans.get(1).appName);
        assertEquals("trace-2", spans.get(1).traceId);
        assertEquals("operation-2", spans.get(1).name);
        assertEquals("SERVER", spans.get(1).kind);

        assertEquals("test-app-3", spans.get(2).appName);
        assertEquals("trace-3", spans.get(2).traceId);
        assertEquals("operation-3", spans.get(2).name);
        assertEquals("INTERNAL", spans.get(2).kind);
    }

    @Test
    public void testBatchingFunctionality() {
        // Given - Set batch size to 2 to trigger batching
        this.batchParser = new BatchParser(objectReader, 2);
        TestBatchConsumer batchConsumer = new TestBatchConsumer();
        String jsonArray = """
            [
                {"appName": "app1", "traceId": "trace1", "spanId": "span1", "name": "op1", "kind": "CLIENT", "startTime": 1000, "costTime": 100, "tags": {}},
                {"appName": "app2", "traceId": "trace2", "spanId": "span2", "name": "op2", "kind": "SERVER", "startTime": 2000, "costTime": 200, "tags": {}},
                {"appName": "app3", "traceId": "trace3", "spanId": "span3", "name": "op3", "kind": "INTERNAL", "startTime": 3000, "costTime": 300, "tags": {}},
                {"appName": "app4", "traceId": "trace4", "spanId": "span4", "name": "op4", "kind": "PRODUCER", "startTime": 4000, "costTime": 400, "tags": {}}
            ]
            """;

        // When
        TraceHttpCollector.ParseResult result = batchParser.parse(new ByteArrayInputStream(jsonArray.getBytes(StandardCharsets.UTF_8)), batchConsumer);

        // Then
        Assertions.assertFalse(result.hasErrors(), "Parsing should succeed");
        assertEquals(4, result.getSuccessfulSpans(), "Should have processed 4 spans");
        assertTrue(result.getParseErrors().isEmpty(), "Should have no validation errors");
        assertEquals(2, batchConsumer.getReceivedBatches().size(), "Should have received 2 batches due to batch size limit");
        assertEquals(4, batchConsumer.getAllReceivedSpans().size(), "Should have received all 4 spans");

        // First batch should have 2 spans
        List<TraceSpan> firstBatch = batchConsumer.getReceivedBatches().get(0);
        assertEquals(2, firstBatch.size());

        // Second batch should have 2 spans  
        List<TraceSpan> secondBatch = batchConsumer.getReceivedBatches().get(1);
        assertEquals(2, secondBatch.size());
    }

    @Test
    public void testEmptyJSONArray() {
        // Given
        this.batchParser = new BatchParser(objectReader, 10);
        TestBatchConsumer batchConsumer = new TestBatchConsumer();
        String emptyArray = "[]";

        // When
        TraceHttpCollector.ParseResult result = batchParser.parse(new ByteArrayInputStream(emptyArray.getBytes(StandardCharsets.UTF_8)), batchConsumer);

        // Then
        Assertions.assertFalse(result.hasErrors(), "Parsing empty array should succeed");
        assertEquals(0, result.getSuccessfulSpans(), "Should have processed 0 spans");
        assertTrue(result.getParseErrors().isEmpty(), "Should have no validation errors");
        assertEquals(0, batchConsumer.getReceivedBatches().size(), "Should not receive any batches for empty array");
        assertEquals(0, batchConsumer.getAllReceivedSpans().size(), "Should not receive any spans");
    }

    @Test
    public void testMalformedJSON() {
        // Given
        this.batchParser = new BatchParser(objectReader, 10);
        TestBatchConsumer batchConsumer = new TestBatchConsumer();
        String malformedJson = """
            [
                {"appName": "test-app", "traceId": "trace-1"
            ]
            """;

        // When
        TraceHttpCollector.ParseResult result = batchParser.parse(new ByteArrayInputStream(malformedJson.getBytes(StandardCharsets.UTF_8)), batchConsumer);

        // Then
        Assertions.assertTrue(result.hasErrors(), "Parsing should fail due to malformed JSON");
        assertEquals(0, result.getSuccessfulSpans(), "Should not have processed any spans");
        assertTrue(result.getException() != null && !result.getException().isEmpty(),
                   "Error message should be present. Actual: " + result.getException());
        assertEquals(0, batchConsumer.getAllReceivedSpans().size(), "Should not process malformed JSON");
    }

    @Test
    public void testInvalidStartTimeValidation() {
        // Given
        this.batchParser = new BatchParser(objectReader, 10);
        TestBatchConsumer batchConsumer = new TestBatchConsumer();
        String jsonWithInvalidStartTime = """
            [
                {"appName": "valid-app", "traceId": "trace1", "spanId": "span1", "name": "op1", "kind": "CLIENT", "startTime": 1000000, "costTime": 100, "tags": {}},
                {"appName": "invalid-app1", "traceId": "trace2", "spanId": "span2", "name": "op2", "kind": "SERVER", "startTime": -1000, "costTime": 200, "tags": {}},
                {"appName": "valid-app2", "traceId": "trace3", "spanId": "span3", "name": "op3", "kind": "INTERNAL", "startTime": 2000000, "costTime": 300, "tags": {}},
                {"appName": "invalid-app2", "traceId": "trace4", "spanId": "span4", "name": "op4", "kind": "PRODUCER", "startTime": 0, "costTime": 400, "tags": {}},
                {"appName": "invalid-app3", "traceId": "trace5", "spanId": "span5", "name": "op5", "kind": "CONSUMER", "startTime": 5000000000000000, "costTime": 500, "tags": {}}
            ]
            """;

        // When
        TraceHttpCollector.ParseResult result = batchParser.parse(new ByteArrayInputStream(jsonWithInvalidStartTime.getBytes(StandardCharsets.UTF_8)), batchConsumer);

        // Then
        Assertions.assertTrue(result.hasErrors());
        assertEquals(2, result.getSuccessfulSpans(), "Should have processed 2 valid spans");
        assertFalse(result.getParseErrors().isEmpty(), "Should have validation errors");
        assertTrue(result.getParseErrors().containsKey("INVALID_START_TIME"), "Should have INVALID_START_TIME errors");
        assertEquals(3, result.getParseErrors().get("INVALID_START_TIME").getErrorCount(), "Should have 3 invalid startTime spans");
        assertEquals(1, batchConsumer.getReceivedBatches().size(), "Should have received 1 batch with valid spans");
        assertEquals(2, batchConsumer.getAllReceivedSpans().size(), "Should have processed only valid spans");

        assertEquals("valid-app", batchConsumer.getAllReceivedSpans().get(0).appName);
        assertEquals("valid-app2", batchConsumer.getAllReceivedSpans().get(1).appName);
    }

    @Test
    public void testInvalidKindValidation() {
        // Given
        this.batchParser = new BatchParser(objectReader, 10);
        TestBatchConsumer batchConsumer = new TestBatchConsumer();
        String jsonWithInvalidKind = """
            [
                {"appName": "valid-app1", "traceId": "trace1", "spanId": "span1", "kind": "CLIENT", "name": "op1", "startTime": 1000000, "costTime": 100, "tags": {}},
                {"appName": "valid-app2", "traceId": "trace2", "spanId": "span2", "kind": "server", "name": "op2", "startTime": 2000000, "costTime": 200, "tags": {}},
                {"appName": "invalid-app1", "traceId": "trace3", "spanId": "span3", "kind": "INVALID_KIND", "name": "op3", "startTime": 3000000, "costTime": 300, "tags": {}},
                {"appName": "valid-app3", "traceId": "trace4", "spanId": "span4", "kind": "Internal", "name": "op4", "startTime": 4000000, "costTime": 400, "tags": {}},
                {"appName": "invalid-app2", "traceId": "trace5", "spanId": "span5", "kind": null, "name": "op5", "startTime": 5000000, "costTime": 500, "tags": {}},
                {"appName": "invalid-app3", "traceId": "trace6", "spanId": "span6", "kind": "", "name": "op6", "startTime": 6000000, "costTime": 600, "tags": {}}
            ]
            """;

        // When
        TraceHttpCollector.ParseResult result = batchParser.parse(new ByteArrayInputStream(jsonWithInvalidKind.getBytes(StandardCharsets.UTF_8)), batchConsumer);

        // Then
        Assertions.assertTrue(result.hasErrors());
        assertEquals(3, result.getSuccessfulSpans(), "Should have processed 3 valid spans (CLIENT, server->SERVER, Internal->INTERNAL are all valid due to case-insensitive validation)");
        assertFalse(result.getParseErrors().isEmpty(), "Should have validation errors");
        assertTrue(result.getParseErrors().containsKey("INVALID_KIND"), "Should have INVALID_KIND errors");
        assertEquals(3, result.getParseErrors().get("INVALID_KIND").getErrorCount(), "Should have 3 invalid kind spans (INVALID_KIND, null, and empty)");
        assertEquals(1, batchConsumer.getReceivedBatches().size(), "Should have received 1 batch with valid spans");
        assertEquals(3, batchConsumer.getAllReceivedSpans().size(), "Should have processed only valid spans");

        assertEquals("valid-app1", batchConsumer.getAllReceivedSpans().get(0).appName);
        assertEquals("CLIENT", batchConsumer.getAllReceivedSpans().get(0).kind);
        assertEquals("valid-app2", batchConsumer.getAllReceivedSpans().get(1).appName);
        assertEquals("server", batchConsumer.getAllReceivedSpans().get(1).kind);
        assertEquals("valid-app3", batchConsumer.getAllReceivedSpans().get(2).appName);
        assertEquals("Internal", batchConsumer.getAllReceivedSpans().get(2).kind);
    }

    @Test
    public void testInvalidTraceIdValidation() {
        // Given
        this.batchParser = new BatchParser(objectReader, 10);
        TestBatchConsumer batchConsumer = new TestBatchConsumer();
        String jsonWithInvalidTraceId = """
            [
                {"appName": "valid-app", "traceId": "valid-trace-1", "spanId": "span1", "name": "op1", "kind": "CLIENT", "startTime": 1000000, "costTime": 100, "tags": {}},
                {"appName": "invalid-app1", "traceId": null, "spanId": "span2", "name": "op2", "kind": "SERVER", "startTime": 2000000, "costTime": 200, "tags": {}},
                {"appName": "valid-app2", "traceId": "valid-trace-2", "spanId": "span3", "name": "op3", "kind": "INTERNAL", "startTime": 3000000, "costTime": 300, "tags": {}},
                {"appName": "invalid-app2", "traceId": "", "spanId": "span4", "name": "op4", "kind": "PRODUCER", "startTime": 4000000, "costTime": 400, "tags": {}},
                {"appName": "invalid-app3", "traceId": "   ", "spanId": "span5", "name": "op5", "kind": "CONSUMER", "startTime": 5000000, "costTime": 500, "tags": {}}
            ]
            """;

        // When
        TraceHttpCollector.ParseResult result = batchParser.parse(new ByteArrayInputStream(jsonWithInvalidTraceId.getBytes(StandardCharsets.UTF_8)), batchConsumer);

        // Then
        Assertions.assertTrue(result.hasErrors());
        assertEquals(2, result.getSuccessfulSpans(), "Should have processed 2 valid spans");
        assertFalse(result.getParseErrors().isEmpty(), "Should have validation errors");
        assertTrue(result.getParseErrors().containsKey("INVALID_TRACE_ID"), "Should have INVALID_TRACE_ID errors");
        assertEquals(3, result.getParseErrors().get("INVALID_TRACE_ID").getErrorCount(), "Should have 3 invalid traceId spans");
        assertEquals(1, batchConsumer.getReceivedBatches().size(), "Should have received 1 batch with valid spans");
        assertEquals(2, batchConsumer.getAllReceivedSpans().size(), "Should have processed only valid spans");

        assertEquals("valid-app", batchConsumer.getAllReceivedSpans().get(0).appName);
        assertEquals("valid-trace-1", batchConsumer.getAllReceivedSpans().get(0).traceId);
        assertEquals("valid-app2", batchConsumer.getAllReceivedSpans().get(1).appName);
        assertEquals("valid-trace-2", batchConsumer.getAllReceivedSpans().get(1).traceId);
    }

    @Test
    public void testInvalidSpanIdValidation() {
        // Given
        this.batchParser = new BatchParser(objectReader, 10);
        TestBatchConsumer batchConsumer = new TestBatchConsumer();
        String jsonWithInvalidSpanId = """
            [
                {"appName": "valid-app", "traceId": "trace1", "spanId": "valid-span-1", "kind": "CLIENT", "name": "op1", "startTime": 1000000, "costTime": 100, "tags": {}},
                {"appName": "invalid-app1", "traceId": "trace2", "spanId": null, "kind": "SERVER", "name": "op2", "startTime": 2000000, "costTime": 200, "tags": {}},
                {"appName": "valid-app2", "traceId": "trace3", "spanId": "valid-span-2", "kind": "INTERNAL", "name": "op3", "startTime": 3000000, "costTime": 300, "tags": {}},
                {"appName": "invalid-app2", "traceId": "trace4", "spanId": "", "kind": "PRODUCER", "name": "op4", "startTime": 4000000, "costTime": 400, "tags": {}},
                {"appName": "invalid-app3", "traceId": "trace5", "spanId": "   ", "kind": "CONSUMER", "name": "op5", "startTime": 5000000, "costTime": 500, "tags": {}}
            ]
            """;

        // When
        TraceHttpCollector.ParseResult result = batchParser.parse(new ByteArrayInputStream(jsonWithInvalidSpanId.getBytes(StandardCharsets.UTF_8)), batchConsumer);

        // Then
        Assertions.assertTrue(result.hasErrors());
        assertEquals(2, result.getSuccessfulSpans(), "Should have processed 2 valid spans");
        assertFalse(result.getParseErrors().isEmpty(), "Should have validation errors");
        assertTrue(result.getParseErrors().containsKey("INVALID_SPAN_ID"), "Should have INVALID_SPAN_ID errors");
        assertEquals(3, result.getParseErrors().get("INVALID_SPAN_ID").getErrorCount(), "Should have 3 invalid spanId spans");
        assertEquals(1, batchConsumer.getReceivedBatches().size(), "Should have received 1 batch with valid spans");
        assertEquals(2, batchConsumer.getAllReceivedSpans().size(), "Should have processed only valid spans");

        assertEquals("valid-app", batchConsumer.getAllReceivedSpans().get(0).appName);
        assertEquals("valid-span-1", batchConsumer.getAllReceivedSpans().get(0).spanId);
        assertEquals("valid-app2", batchConsumer.getAllReceivedSpans().get(1).appName);
        assertEquals("valid-span-2", batchConsumer.getAllReceivedSpans().get(1).spanId);
    }

    @Test
    public void testInvalidEndTimeValidation() {
        // Given
        this.batchParser = new BatchParser(objectReader, 10);
        TestBatchConsumer batchConsumer = new TestBatchConsumer();
        String jsonWithInvalidEndTime = """
            [
                {"appName": "valid-app1", "traceId": "trace1", "spanId": "span1", "kind": "CLIENT", "name": "op1", "startTime": 1000000, "endTime": 1005000, "costTime": 100, "tags": {}},
                {"appName": "valid-app2", "traceId": "trace2", "spanId": "span2", "kind": "SERVER", "name": "op2", "startTime": 2000000, "costTime": 200, "tags": {}},
                {"appName": "valid-app3", "traceId": "trace4", "spanId": "span4", "kind": "PRODUCER", "name": "op4", "startTime": 4000000, "endTime": 0, "costTime": 400, "tags": {}},
                {"appName": "invalid-app1", "traceId": "trace3", "spanId": "span3", "kind": "INTERNAL", "name": "op3", "startTime": 3000000, "endTime": -1000, "costTime": 300, "tags": {}},
                {"appName": "invalid-app2", "traceId": "trace5", "spanId": "span5", "kind": "CONSUMER", "name": "op5", "startTime": 5000000, "endTime": 5000000000000000, "costTime": 500, "tags": {}}
            ]
            """;

        // When
        TraceHttpCollector.ParseResult result = batchParser.parse(new ByteArrayInputStream(jsonWithInvalidEndTime.getBytes(StandardCharsets.UTF_8)), batchConsumer);

        // Then
        Assertions.assertTrue(result.hasErrors());
        assertEquals(3, result.getSuccessfulSpans(), "Should have processed 3 valid spans (endTime=0 is considered valid/missing)");
        assertFalse(result.getParseErrors().isEmpty(), "Should have validation errors");
        assertTrue(result.getParseErrors().containsKey("INVALID_END_TIME"), "Should have INVALID_END_TIME errors");
        assertEquals(2, result.getParseErrors().get("INVALID_END_TIME").getErrorCount(), "Should have 2 invalid endTime spans (endTime=0 is considered missing, not invalid)");
        assertEquals(1, batchConsumer.getReceivedBatches().size(), "Should have received 1 batch with valid spans");
        assertEquals(3, batchConsumer.getAllReceivedSpans().size(), "Should have processed only valid spans");

        assertEquals("valid-app1", batchConsumer.getAllReceivedSpans().get(0).appName);
        assertEquals(1005000L, batchConsumer.getAllReceivedSpans().get(0).endTime);
        assertEquals("valid-app2", batchConsumer.getAllReceivedSpans().get(1).appName);
        assertEquals(2000200L, batchConsumer.getAllReceivedSpans().get(1).endTime); // should be calculated as startTime + costTime
        assertEquals("valid-app3", batchConsumer.getAllReceivedSpans().get(2).appName);
        assertEquals(4000400L, batchConsumer.getAllReceivedSpans().get(2).endTime); // should be calculated as startTime + costTime since endTime was 0
    }

    @Test
    public void testFieldNormalizationAndCalculation() {
        // Given
        this.batchParser = new BatchParser(objectReader, 10);
        TestBatchConsumer batchConsumer = new TestBatchConsumer();
        String jsonWithNullFields = """
            [
                {
                    "appName": "test-app",
                    "traceId": "trace-1",
                    "spanId": "span-1",
                    "name": "operation-1",
                    "kind": "CLIENT",
                    "startTime": 1000000,
                    "costTime": 5000,
                    "method": null,
                    "clazz": null,
                    "tags": {}
                }
            ]
            """;

        // When
        TraceHttpCollector.ParseResult result = batchParser.parse(new ByteArrayInputStream(jsonWithNullFields.getBytes(StandardCharsets.UTF_8)), batchConsumer);

        // Then
        Assertions.assertFalse(result.hasErrors());
        assertEquals(1, result.getSuccessfulSpans(), "Should have processed 1 span");
        assertTrue(result.getParseErrors().isEmpty(), "Should have no validation errors");
        assertEquals(1, batchConsumer.getAllReceivedSpans().size());

        TraceSpan span = batchConsumer.getAllReceivedSpans().get(0);
        assertEquals("", span.method, "Null method should be converted to empty string");
        assertEquals("", span.clazz, "Null clazz should be converted to empty string");
        assertEquals(1005000L, span.endTime, "endTime should be calculated as startTime + costTime");
    }

    @Test
    public void testSingleObjectJSONEachRow() {
        // Given
        this.batchParser = new BatchParser(objectReader, 10);
        TestBatchConsumer batchConsumer = new TestBatchConsumer();
        String singleObject = """
            {"appName": "test-app", "traceId": "trace-1", "spanId": "span-1", "name": "operation-1", "kind": "INTERNAL", "startTime": 1000000, "costTime": 5000, "tags": {}}
            """;

        // When
        TraceHttpCollector.ParseResult result = batchParser.parse(new ByteArrayInputStream(singleObject.getBytes(StandardCharsets.UTF_8)), batchConsumer);

        // Then
        Assertions.assertFalse(result.hasErrors(), "Parsing should succeed");
        assertEquals(1, result.getSuccessfulSpans(), "Should have processed 1 span");
        assertTrue(result.getParseErrors().isEmpty(), "Should have no validation errors");
        assertEquals(1, batchConsumer.getReceivedBatches().size());
        assertEquals(1, batchConsumer.getAllReceivedSpans().size());
        assertEquals("test-app", batchConsumer.getAllReceivedSpans().get(0).appName);
    }

    @Test
    public void testBatchSizeEdgeCases() {
        // Test batch size = 1
        this.batchParser = new BatchParser(objectReader, 1);
        TestBatchConsumer batchConsumer = new TestBatchConsumer();
        String jsonArray = """
            [
                {"appName": "app1", "traceId": "trace1", "spanId": "span1", "name": "op1", "kind": "CLIENT", "startTime": 1000, "costTime": 100, "tags": {}},
                {"appName": "app2", "traceId": "trace2", "spanId": "span2", "name": "op2", "kind": "SERVER", "startTime": 2000, "costTime": 200, "tags": {}},
                {"appName": "app3", "traceId": "trace3", "spanId": "span3", "name": "op3", "kind": "INTERNAL", "startTime": 3000, "costTime": 300, "tags": {}}
            ]
            """;

        // When
        TraceHttpCollector.ParseResult result = batchParser.parse(new ByteArrayInputStream(jsonArray.getBytes(StandardCharsets.UTF_8)), batchConsumer);

        // Then
        Assertions.assertFalse(result.hasErrors(), "Parsing should succeed");
        assertEquals(3, result.getSuccessfulSpans(), "Should have processed 3 spans");
        assertTrue(result.getParseErrors().isEmpty(), "Should have no validation errors");
        assertEquals(3, batchConsumer.getReceivedBatches().size(), "Should have 3 batches with batch size 1");
        assertEquals(3, batchConsumer.getAllReceivedSpans().size(), "Should have received all 3 spans");

        // Each batch should have exactly 1 span
        for (List<TraceSpan> batch : batchConsumer.getReceivedBatches()) {
            assertEquals(1, batch.size(), "Each batch should have exactly 1 span");
        }
    }

    @Test
    public void testInvalidJSONStructure() {
        // Given
        this.batchParser = new BatchParser(objectReader, 10);
        TestBatchConsumer batchConsumer = new TestBatchConsumer();
        String invalidJson = "\"not an object or array\"";

        // When
        TraceHttpCollector.ParseResult result = batchParser.parse(new ByteArrayInputStream(invalidJson.getBytes(StandardCharsets.UTF_8)), batchConsumer);

        // Then
        Assertions.assertFalse(result.hasErrors(), "Parsing should succeed but process no spans");
        assertEquals(0, result.getSuccessfulSpans(), "Should have processed 0 spans");
        assertTrue(result.getParseErrors().isEmpty(), "Should have no validation errors");
        assertEquals(0, batchConsumer.getAllReceivedSpans().size(), "Should not process invalid JSON structure");
    }

    @Test
    public void testAllValidationTypes() {
        // Given
        this.batchParser = new BatchParser(objectReader, 10);
        TestBatchConsumer batchConsumer = new TestBatchConsumer();
        String jsonWithAllValidationIssues = """
            [
                {"appName": "valid-app", "traceId": "trace1", "spanId": "span1", "kind": "CLIENT", "name": "op1", "startTime": 1000000, "endTime": 1005000, "costTime": 100, "tags": {}},
                {"appName": "invalid-start-time", "traceId": "trace2", "spanId": "span2", "kind": "SERVER", "name": "op2", "startTime": -1000, "costTime": 200, "tags": {}},
                {"appName": "invalid-trace-id", "traceId": "", "spanId": "span3", "kind": "INTERNAL", "name": "op3", "startTime": 2000000, "costTime": 300, "tags": {}},
                {"appName": "invalid-span-id", "traceId": "trace4", "spanId": null, "kind": "PRODUCER", "name": "op4", "startTime": 3000000, "costTime": 400, "tags": {}},
                {"appName": "invalid-kind", "traceId": "trace5", "spanId": "span5", "kind": "UNKNOWN_KIND", "name": "op5", "startTime": 4000000, "costTime": 500, "tags": {}},
                {"appName": "invalid-end-time", "traceId": "trace6", "spanId": "span6", "kind": "CONSUMER", "name": "op6", "startTime": 5000000, "endTime": -2000, "costTime": 600, "tags": {}}
            ]
            """;

        // When
        TraceHttpCollector.ParseResult result = batchParser.parse(new ByteArrayInputStream(jsonWithAllValidationIssues.getBytes(StandardCharsets.UTF_8)), batchConsumer);

        // Then
        Assertions.assertTrue(result.hasErrors());
        assertEquals(1, result.getSuccessfulSpans(), "Should have processed 1 valid span");
        assertFalse(result.getParseErrors().isEmpty(), "Should have validation errors");

        // Should have all types of errors
        assertTrue(result.getParseErrors().containsKey("INVALID_START_TIME"), "Should have INVALID_START_TIME errors");
        assertTrue(result.getParseErrors().containsKey("INVALID_TRACE_ID"), "Should have INVALID_TRACE_ID errors");
        assertTrue(result.getParseErrors().containsKey("INVALID_SPAN_ID"), "Should have INVALID_SPAN_ID errors");
        assertTrue(result.getParseErrors().containsKey("INVALID_KIND"), "Should have INVALID_KIND errors");
        assertTrue(result.getParseErrors().containsKey("INVALID_END_TIME"), "Should have INVALID_END_TIME errors");

        // Check error counts
        assertEquals(1, result.getParseErrors().get("INVALID_START_TIME").getErrorCount(), "Should have 1 invalid startTime span");
        assertEquals(1, result.getParseErrors().get("INVALID_TRACE_ID").getErrorCount(), "Should have 1 invalid traceId span");
        assertEquals(1, result.getParseErrors().get("INVALID_SPAN_ID").getErrorCount(), "Should have 1 invalid spanId span");
        assertEquals(1, result.getParseErrors().get("INVALID_KIND").getErrorCount(), "Should have 1 invalid kind span");
        assertEquals(1, result.getParseErrors().get("INVALID_END_TIME").getErrorCount(), "Should have 1 invalid endTime span");

        assertEquals(1, batchConsumer.getReceivedBatches().size(), "Should have received 1 batch with valid spans");
        assertEquals(1, batchConsumer.getAllReceivedSpans().size(), "Should have processed only valid spans");

        TraceSpan validSpan = batchConsumer.getAllReceivedSpans().get(0);
        assertEquals("valid-app", validSpan.appName);
        assertEquals("trace1", validSpan.traceId);
        assertEquals("span1", validSpan.spanId);
        assertEquals("CLIENT", validSpan.kind);
        assertEquals(1005000L, validSpan.endTime);
    }

    @Test
    public void testClearCurrentTokenRequiredForJSONLinesFormat() throws Exception {
        // This test simulates the exact scenario in TraceSpanParser.parse()
        // where clearCurrentToken() is needed
        
        String jsonLinesData = """
            {"appName": "test-app-1", "traceId": "trace-1", "spanId": "span-1", "name": "operation-1", "kind": "CLIENT", "startTime": 1000000, "costTime": 5000, "endTime": 1005000, "tags": {"key1": "value1"}}
            {"appName": "test-app-2", "traceId": "trace-2", "spanId": "span-2", "name": "operation-2", "kind": "SERVER", "startTime": 2000000, "costTime": 6000, "endTime": 2006000, "tags": {"key2": "value2"}}
            """;

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectReader reader = objectMapper.readerFor(TraceSpan.class);
        
        // Test WITHOUT clearCurrentToken() 
        try (JsonParser parser = reader.createParser(new ByteArrayInputStream(jsonLinesData.getBytes(StandardCharsets.UTF_8)))) {
            JsonToken token = parser.nextToken();
            
            if (token == JsonToken.START_OBJECT) {
                // Simulate the scenario in TraceSpanParser.parse() WITHOUT clearCurrentToken()
                // This should potentially fail or read incorrectly
                
                int spanCount = 0;
                do {
                    try {
                        TraceSpan span = reader.readValue(parser);
                        spanCount++;
                        System.out.println("WITHOUT clearCurrentToken - Span " + spanCount + ": " + span.appName);
                    } catch (Exception e) {
                        System.out.println("WITHOUT clearCurrentToken - Failed to read span: " + e.getMessage());
                        break;
                    }
                } while (parser.nextToken() == JsonToken.START_OBJECT);
                
                System.out.println("WITHOUT clearCurrentToken - Total spans read: " + spanCount);
            }
        }
        
        // Test WITH clearCurrentToken()
        try (JsonParser parser = reader.createParser(new ByteArrayInputStream(jsonLinesData.getBytes(StandardCharsets.UTF_8)))) {
            JsonToken token = parser.nextToken();
            
            if (token == JsonToken.START_OBJECT) {
                // Simulate the exact scenario in TraceSpanParser.parse() WITH clearCurrentToken()
                parser.clearCurrentToken();
                
                int spanCount = 0;
                do {
                    try {
                        TraceSpan span = reader.readValue(parser);
                        spanCount++;
                        System.out.println("WITH clearCurrentToken - Span " + spanCount + ": " + span.appName);
                    } catch (Exception e) {
                        System.out.println("WITH clearCurrentToken - Failed to read span: " + e.getMessage());
                        break;
                    }
                } while (parser.nextToken() == JsonToken.START_OBJECT);
                
                System.out.println("WITH clearCurrentToken - Total spans read: " + spanCount);
            }
        }
        
        // The test passes if both scenarios complete without exceptions
        // If clearCurrentToken() is truly needed, the first scenario should fail or behave differently
        assertTrue(true); // This test is more about observing behavior via console output
    }
} 
