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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

/**
 * A highly optimized deserializer for TraceSpan to avoid extra overhead inside the jackson library.
 *
 * @author frank.chen021@outlook.com
 * @date 2025/10/11
 */
public class TraceSpanDeserializer extends StdDeserializer<TraceSpan> {

    // Pre-computed hash codes as compile-time constants for switch statement
    // String.hashCode() is deterministic, so we can compute these at compile time
    private static final int HASH_TRACE_ID = -1067401920;        // "traceId".hashCode()
    private static final int HASH_SPAN_ID = -896182779;          // "spanId".hashCode()
    private static final int HASH_START_TIME = -2129294769;      // "startTime".hashCode()
    private static final int HASH_END_TIME = -1607243192;        // "endTime".hashCode()
    private static final int HASH_COST_TIME = -424687558;        // "costTime".hashCode()
    private static final int HASH_KIND = 3292052;                // "kind".hashCode()
    private static final int HASH_NAME = 3373707;                // "name".hashCode()
    private static final int HASH_APP_NAME = -794136500;         // "appName".hashCode()
    private static final int HASH_INSTANCE_NAME = -737857344;    // "instanceName".hashCode()
    private static final int HASH_PARENT_SPAN_ID = 1059234639;   // "parentSpanId".hashCode()
    private static final int HASH_PARENT_APPLICATION = -1683655098; // "parentApplication".hashCode()
    private static final int HASH_APP_TYPE = -793934597;         // "appType".hashCode()
    private static final int HASH_TAGS = 3552281;                // "tags".hashCode()
    private static final int HASH_CLAZZ = 94743128;              // "clazz".hashCode()
    private static final int HASH_METHOD = -1077554975;          // "method".hashCode()
    private static final int HASH_STATUS = -892481550;           // "status".hashCode()
    private static final int HASH_NORMALIZED_URI = -1397173515;  // "normalizedUri".hashCode()
    private static final int HASH_COST_TIME_MS = -102847616;     // "costTimeMs".hashCode()
    private static final int HASH_START_TIME_US = -1847837363;   // "startTimeUs".hashCode()

    public TraceSpanDeserializer() {
        super(TraceSpan.class);
    }

    @Override
    public TraceSpan deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentToken() != JsonToken.START_OBJECT) {
            ctxt.reportWrongTokenException(this, JsonToken.START_OBJECT, null);
        }

        TraceSpan span = new TraceSpan();
        span.tags = new TreeMap<>();

        while (p.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = p.currentName();

            JsonToken valToken = p.nextToken();
            if (valToken == JsonToken.VALUE_NULL) {
                continue;
            }

            switch (fieldName.hashCode()) {
                case HASH_TRACE_ID:
                    if ("traceId".equals(fieldName)) {
                        span.traceId = p.getText();
                    }
                    break;
                case HASH_SPAN_ID:
                    if ("spanId".equals(fieldName)) {
                        span.spanId = p.getText();
                    }
                    break;
                case HASH_START_TIME:
                    if ("startTime".equals(fieldName)) {
                        span.startTime = p.getLongValue();
                    }
                    break;
                case HASH_END_TIME:
                    if ("endTime".equals(fieldName)) {
                        span.endTime = p.getLongValue();
                    }
                    break;
                case HASH_COST_TIME:
                    if ("costTime".equals(fieldName)) {
                        span.costTime = p.getLongValue();
                    }
                    break;
                case HASH_KIND:
                    if ("kind".equals(fieldName)) {
                        span.kind = p.getText();
                    }
                    break;
                case HASH_NAME:
                    if ("name".equals(fieldName)) {
                        span.name = p.getText();
                    }
                    break;
                case HASH_APP_NAME:
                    if ("appName".equals(fieldName)) {
                        span.appName = p.getText();
                    }
                    break;
                case HASH_INSTANCE_NAME:
                    if ("instanceName".equals(fieldName)) {
                        span.instanceName = p.getText();
                    }
                    break;
                case HASH_PARENT_SPAN_ID:
                    if ("parentSpanId".equals(fieldName)) {
                        span.parentSpanId = p.getText();
                    }
                    break;
                case HASH_PARENT_APPLICATION:
                    if ("parentApplication".equals(fieldName)) {
                        span.parentApplication = p.getText();
                    }
                    break;
                case HASH_APP_TYPE:
                    if ("appType".equals(fieldName)) {
                        span.appType = p.getText();
                    }
                    break;
                case HASH_TAGS:
                    if ("tags".equals(fieldName)) {
                        deserializeTags(p, span.tags);
                    }
                    break;
                case HASH_CLAZZ:
                    if ("clazz".equals(fieldName)) {
                        span.clazz = p.getText();
                    }
                    break;
                case HASH_METHOD:
                    if ("method".equals(fieldName)) {
                        span.method = p.getText();
                    }
                    break;
                case HASH_STATUS:
                    if ("status".equals(fieldName)) {
                        span.status = p.getText();
                    }
                    break;
                case HASH_NORMALIZED_URI:
                    if ("normalizedUri".equals(fieldName)) {
                        span.normalizedUri = p.getText();
                    }
                    break;
                case HASH_COST_TIME_MS:
                    if ("costTimeMs".equals(fieldName)) {
                        span.costTime = p.getLongValue();
                    }
                    break;
                case HASH_START_TIME_US:
                    if ("startTimeUs".equals(fieldName)) {
                        span.startTime = p.getLongValue();
                    }
                    break;
                default:
                    // Unknown field or hash collision - skip it
                    p.skipChildren();
                    break;
            }
        }

        return span;
    }

    /**
     * Optimized manual deserialization for tags Map&lt;String, String&gt;.
     * Faster than using Jackson's generic TypeReference deserialization.
     */
    private void deserializeTags(JsonParser p, Map<String, String> tags) throws IOException {
        // Expect START_OBJECT token
        if (p.currentToken() != JsonToken.START_OBJECT) {
            return;
        }

        // Parse key-value pairs directly
        while (p.nextToken() != JsonToken.END_OBJECT) {
            String key = p.currentName();
            JsonToken val = p.nextToken();

            // Handle null values gracefully
            if (val != JsonToken.VALUE_NULL) {
                String value = p.getText();
                tags.put(key, value);
            }
        }
    }
}

