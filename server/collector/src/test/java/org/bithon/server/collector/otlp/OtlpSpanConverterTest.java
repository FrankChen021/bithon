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

package org.bithon.server.collector.otlp;

import com.google.protobuf.ByteString;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.resource.v1.Resource;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import io.opentelemetry.proto.trace.v1.Status;
import org.bithon.component.commons.tracing.Tags;
import org.bithon.server.storage.tracing.TraceSpan;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class OtlpSpanConverterTest {

    @Test
    void convertsTypedAttributesAndNormalizesHttpStatus() {
        Span span = Span.newBuilder()
                        .setTraceId(ByteString.copyFromUtf8("trace-id-12345678"))
                        .setSpanId(ByteString.copyFromUtf8("span-id-"))
                        .setName("GET /api")
                        .addAttributes(attribute("http.status_code", AnyValue.newBuilder().setIntValue(204).build()))
                        .addAttributes(attribute("boolean", AnyValue.newBuilder().setBoolValue(true).build()))
                        .addAttributes(attribute("double", AnyValue.newBuilder().setDoubleValue(1.5).build()))
                        .addAttributes(attribute("bytes", AnyValue.newBuilder().setBytesValue(ByteString.copyFrom(new byte[]{1, 2})).build()))
                        .build();

        TraceSpan converted = new OtlpSpanConverter(List.of(ResourceSpans.newBuilder()
                                                                          .setResource(Resource.newBuilder()
                                                                                               .addAttributes(attribute("service.name", AnyValue.newBuilder().setStringValue("test-service").build())))
                                                                          .addScopeSpans(ScopeSpans.newBuilder().addSpans(span))
                                                                          .build()))
            .toSpanList()
            .get(0);

        assertEquals("204", converted.getTags().get("http.status"));
        assertFalse(converted.getTags().containsKey("http.status_code"));
        assertEquals("true", converted.getTags().get("boolean"));
        assertEquals("1.5", converted.getTags().get("double"));
        assertEquals("AQI=", converted.getTags().get("bytes"));
    }

    @Test
    void currentHttpResponseStatusTakesPrecedenceOverLegacyStatus() {
        Span span = Span.newBuilder()
                        .setTraceId(ByteString.copyFromUtf8("trace-id-12345678"))
                        .setSpanId(ByteString.copyFromUtf8("span-id-"))
                        .addAttributes(attribute("http.status_code", AnyValue.newBuilder().setIntValue(500).build()))
                        .addAttributes(attribute("http.response.status_code", AnyValue.newBuilder().setIntValue(201).build()))
                        .build();

        TraceSpan converted = new OtlpSpanConverter(List.of(ResourceSpans.newBuilder()
                                                                          .addScopeSpans(ScopeSpans.newBuilder().addSpans(span))
                                                                          .build()))
            .toSpanList()
            .get(0);

        assertEquals("201", converted.getTags().get("http.status"));
    }

    @Test
    void convertsSpanWithoutAttributes() {
        Span span = Span.newBuilder()
                        .setTraceId(ByteString.copyFromUtf8("trace-id-12345678"))
                        .setSpanId(ByteString.copyFromUtf8("span-id-"))
                        .setStatus(Status.newBuilder().setMessage("failed"))
                        .build();

        TraceSpan converted = new OtlpSpanConverter(List.of(ResourceSpans.newBuilder()
                                                                          .addScopeSpans(ScopeSpans.newBuilder().addSpans(span))
                                                                          .build()))
            .toSpanList()
            .get(0);

        assertEquals("failed", converted.getTags().get(Tags.Exception.MESSAGE));
    }

    private KeyValue attribute(String key, AnyValue value) {
        return KeyValue.newBuilder().setKey(key).setValue(value).build();
    }
}
