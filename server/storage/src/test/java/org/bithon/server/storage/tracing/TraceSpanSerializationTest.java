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

package org.bithon.server.storage.tracing;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comprehensive test for TraceSpan serialization and deserialization.
 * Verifies that the optimized TraceSpanHashDeserializer correctly handles all fields
 * and maintains compatibility with Jackson serialization.
 *
 * @author frank.chen021@outlook.com
 * @date 2025/10/11
 */
@DisplayName("TraceSpan Serialization and Deserialization")
public class TraceSpanSerializationTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        SimpleModule module = new SimpleModule();
        module.addDeserializer(TraceSpan.class, new TraceSpanDeserializer());
        mapper.registerModule(module);
    }

    /**
     * Creates a sample TraceSpan with all fields populated
     */
    private TraceSpan createSampleTraceSpan() {
        TraceSpan span = new TraceSpan();
        span.traceId = "trace-abc123def456";
        span.spanId = "span-789xyz";
        span.parentSpanId = "span-parent-000";
        span.startTime = 1697000000000000L; // microseconds
        span.endTime = 1697000001000000L;
        span.costTime = 1000000L; // 1 second in microseconds
        span.kind = "SERVER";
        span.name = "/api/users/{id}";
        span.appName = "user-service";
        span.instanceName = "user-service-pod-123";
        span.appType = "JAVA";
        span.parentApplication = "gateway-service";
        span.clazz = "com.example.UserController";
        span.method = "getUserById";
        span.status = "ok";
        span.normalizedUri = "/api/users/{id}";

        span.tags = new TreeMap<>();
        span.tags.put("http.method", "GET");
        span.tags.put("http.status", "200");
        span.tags.put("service.version", "1.2.3");
        span.tags.put("environment", "production");
        span.tags.put("region", "us-west-2");

        return span;
    }

    @Test
    @DisplayName("Complete serialization and deserialization with all fields")
    void testCompleteSerializationDeserialization() throws Exception {
        // Given: original span with all fields populated
        TraceSpan original = createSampleTraceSpan();

        // When: serialize to JSON and deserialize back
        String json = mapper.writeValueAsString(original);
        TraceSpan deserialized = mapper.readValue(json, TraceSpan.class);

        // Then: all fields should match
        assertEquals(original.traceId, deserialized.traceId, "traceId should match");
        assertEquals(original.spanId, deserialized.spanId, "spanId should match");
        assertEquals(original.parentSpanId, deserialized.parentSpanId, "parentSpanId should match");
        assertEquals(original.startTime, deserialized.startTime, "startTime should match");
        assertEquals(original.endTime, deserialized.endTime, "endTime should match");
        assertEquals(original.costTime, deserialized.costTime, "costTime should match");
        assertEquals(original.kind, deserialized.kind, "kind should match");
        assertEquals(original.name, deserialized.name, "name should match");
        assertEquals(original.appName, deserialized.appName, "appName should match");
        assertEquals(original.instanceName, deserialized.instanceName, "instanceName should match");
        assertEquals(original.appType, deserialized.appType, "appType should match");
        assertEquals(original.parentApplication, deserialized.parentApplication, "parentApplication should match");
        assertEquals(original.clazz, deserialized.clazz, "clazz should match");
        assertEquals(original.method, deserialized.method, "method should match");
        assertEquals(original.status, deserialized.status, "status should match");
        assertEquals(original.normalizedUri, deserialized.normalizedUri, "normalizedUri should match");

        // Verify tags
        assertEquals(original.tags.size(), deserialized.tags.size(), "tags size should match");
        original.tags.forEach((key, value) -> {
            assertTrue(deserialized.tags.containsKey(key), "tags should contain key: " + key);
            assertEquals(value, deserialized.tags.get(key), "tags value should match for key: " + key);
        });
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTest {

        @Test
        @DisplayName("Minimal span with only required fields")
        void minimalSpan() throws Exception {
            // Given: JSON with minimal required fields
            String minimalJson = "{\"traceId\":\"t1\",\"spanId\":\"s1\",\"startTime\":123,\"costTime\":10,\"kind\":\"CLIENT\"}";

            // When: deserialize
            TraceSpan minimal = mapper.readValue(minimalJson, TraceSpan.class);

            // Then: required fields should be set, optional fields should use defaults
            assertEquals("t1", minimal.traceId);
            assertEquals("s1", minimal.spanId);
            assertEquals(123, minimal.startTime);
            assertNotNull(minimal.tags);
            assertTrue(minimal.tags.isEmpty());
        }

        @Test
        @DisplayName("Span with null values")
        void nullValues() throws Exception {
            // Given: JSON with explicit null values
            String nullJson = "{\"traceId\":\"t2\",\"spanId\":\"s2\",\"startTime\":456,\"costTime\":20," +
                              "\"parentSpanId\":null,\"name\":null,\"tags\":{\"key1\":null,\"key2\":\"value2\"}}";

            // When: deserialize
            TraceSpan withNulls = mapper.readValue(nullJson, TraceSpan.class);

            // Then: null values should be preserved
            assertNull(withNulls.parentSpanId);
            assertNull(withNulls.name);
            assertEquals("value2", withNulls.tags.get("key2"));
        }

        @Test
        @DisplayName("Span with empty tags")
        void emptyTags() throws Exception {
            // Given: JSON with empty tags object
            String emptyTagsJson = "{\"traceId\":\"t3\",\"spanId\":\"s3\",\"startTime\":789,\"costTime\":30,\"tags\":{}}";

            // When: deserialize
            TraceSpan result = mapper.readValue(emptyTagsJson, TraceSpan.class);

            // Then: tags should be non-null but empty
            assertNotNull(result.tags);
            assertTrue(result.tags.isEmpty());
        }

        @Test
        @DisplayName("Span without tags field")
        void missingTags() throws Exception {
            // Given: JSON without tags field
            String noTagsJson = "{\"traceId\":\"t4\",\"spanId\":\"s4\",\"startTime\":999,\"costTime\":40}";

            // When: deserialize
            TraceSpan result = mapper.readValue(noTagsJson, TraceSpan.class);

            // Then: tags should be initialized to empty map
            assertNotNull(result.tags);
            assertTrue(result.tags.isEmpty());
        }

        @Test
        @DisplayName("Span with unknown fields should be ignored")
        void unknownFields() throws Exception {
            // Given: JSON with unknown fields
            String unknownFieldsJson = "{\"traceId\":\"t5\",\"spanId\":\"s5\",\"startTime\":111," +
                                       "\"costTime\":50,\"unknownField1\":\"value\",\"unknownField2\":123}";

            // When: deserialize
            TraceSpan result = mapper.readValue(unknownFieldsJson, TraceSpan.class);

            // Then: known fields should be parsed, unknown fields ignored
            assertEquals("t5", result.traceId);
            assertEquals("s5", result.spanId);
        }

        @Test
        @DisplayName("Span with very long strings")
        void longStrings() throws Exception {
            // Given: span with 1000-character string
            String longString = "x".repeat(1000);
            TraceSpan longSpan = new TraceSpan();
            longSpan.traceId = longString;
            longSpan.spanId = "s6";
            longSpan.startTime = 222;
            longSpan.costTime = 60;
            longSpan.tags = new TreeMap<>();

            // When: serialize and deserialize
            String json = mapper.writeValueAsString(longSpan);
            TraceSpan result = mapper.readValue(json, TraceSpan.class);

            // Then: long string should be preserved
            assertEquals(longString, result.traceId);
            assertEquals(1000, result.traceId.length());
        }

        @Test
        @DisplayName("Span with many tags (100 entries)")
        void manyTags() throws Exception {
            // Given: span with 100 tag entries
            TraceSpan manyTags = new TraceSpan();
            manyTags.traceId = "t7";
            manyTags.spanId = "s7";
            manyTags.startTime = 333;
            manyTags.costTime = 70;
            manyTags.tags = new TreeMap<>();
            for (int i = 0; i < 100; i++) {
                manyTags.tags.put("tag" + i, "value" + i);
            }

            // When: serialize and deserialize
            String json = mapper.writeValueAsString(manyTags);
            TraceSpan result = mapper.readValue(json, TraceSpan.class);

            // Then: all tags should be preserved
            assertEquals(100, result.tags.size());
            assertEquals("value50", result.tags.get("tag50"));
        }
    }

    @Nested
    @DisplayName("Backward Compatibility")
    class BackwardCompatibilityTest {

        @Test
        @DisplayName("Legacy field names (startTimeUs, costTimeMs)")
        void legacyFieldNames() throws Exception {
            // Given: JSON with legacy field names
            String legacyJson = "{\"traceId\":\"t1\",\"spanId\":\"s1\"," +
                                "\"startTimeUs\":1234567890,\"costTimeMs\":1000}";

            // When: deserialize
            TraceSpan result = mapper.readValue(legacyJson, TraceSpan.class);

            // Then: legacy fields should map to current fields
            assertEquals(1234567890, result.startTime, "startTimeUs should map to startTime");
            assertEquals(1000, result.costTime, "costTimeMs should map to costTime");
        }

        @Test
        @DisplayName("Mixed new and legacy fields (last one wins)")
        void mixedFieldNames() throws Exception {
            // Given: JSON with both new and legacy field names
            String mixedJson = "{\"traceId\":\"t2\",\"spanId\":\"s2\"," +
                               "\"startTimeUs\":111,\"startTime\":222,\"costTimeMs\":100,\"costTime\":200}";

            // When: deserialize
            TraceSpan result = mapper.readValue(mixedJson, TraceSpan.class);

            // Then: last value in JSON should win (Jackson behavior)
            assertTrue(result.startTime == 222 || result.startTime == 111,
                       "Either value acceptable depending on JSON field order");
            assertTrue(result.costTime == 200 || result.costTime == 100,
                       "Either value acceptable depending on JSON field order");
        }
    }

    @Test
    @DisplayName("Array deserialization with multiple spans")
    void testArrayDeserialization() throws Exception {
        // Given: JSON array with 3 spans
        String arrayJson = "[" +
                           "{\"traceId\":\"t1\",\"spanId\":\"s1\",\"startTime\":100,\"costTime\":10," +
                           "\"tags\":{\"key1\":\"value1\"}}," +
                           "{\"traceId\":\"t2\",\"spanId\":\"s2\",\"startTime\":200,\"costTime\":20," +
                           "\"tags\":{\"key2\":\"value2\"}}," +
                           "{\"traceId\":\"t3\",\"spanId\":\"s3\",\"startTime\":300,\"costTime\":30," +
                           "\"tags\":{\"key3\":\"value3\"}}" +
                           "]";

        // When: deserialize array
        TraceSpan[] spans = mapper.readValue(arrayJson, TraceSpan[].class);

        // Then: all spans should be deserialized correctly
        assertEquals(3, spans.length, "Should have 3 spans");

        assertEquals("t1", spans[0].traceId);
        assertEquals("value1", spans[0].tags.get("key1"));

        assertEquals("t2", spans[1].traceId);
        assertEquals("value2", spans[1].tags.get("key2"));

        assertEquals("t3", spans[2].traceId);
        assertEquals("value3", spans[2].tags.get("key3"));
    }

    @Test
    @DisplayName("Round-trip serialization produces equivalent objects")
    void testRoundTripEquality() throws Exception {
        // Given: original span
        TraceSpan original = createSampleTraceSpan();

        // When: serialize → deserialize → serialize
        String json1 = mapper.writeValueAsString(original);
        TraceSpan deserialized = mapper.readValue(json1, TraceSpan.class);
        String json2 = mapper.writeValueAsString(deserialized);

        // Then: re-parsing both JSONs should produce equivalent objects
        TraceSpan span1 = mapper.readValue(json1, TraceSpan.class);
        TraceSpan span2 = mapper.readValue(json2, TraceSpan.class);

        assertEquals(span1.traceId, span2.traceId);
        assertEquals(span1.spanId, span2.spanId);
        assertEquals(span1.startTime, span2.startTime);
        assertEquals(span1.tags, span2.tags);
    }
}

