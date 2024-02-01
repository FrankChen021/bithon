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

package org.bithon.server.collector.otel;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.ExtensionRegistryLite;
import com.google.protobuf.util.JsonFormat;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import io.opentelemetry.proto.trace.v1.Status;
import io.opentelemetry.proto.trace.v1.TracesData;
import org.bithon.component.commons.tracing.SpanKind;
import org.bithon.component.commons.tracing.Tags;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.tracing.TraceSpan;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/9/2 11:12
 */
class OtelSpanConverter {

    public static OtelSpanConverter fromJson(InputStream is) throws IOException {
        TracesData.Builder builder = TracesData.newBuilder();

        try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            JsonFormat.parser().merge(reader, builder);
        }

        return new OtelSpanConverter(builder.build().getResourceSpansList()) {
            @Override
            protected String getTraceId(ByteString id) {
                return StringUtils.encodeBase64String(id.toByteArray()).toLowerCase(Locale.ENGLISH);
            }

            @Override
            protected String getSpanId(ByteString id) {
                return StringUtils.encodeBase64String(id.toByteArray()).toLowerCase(Locale.ENGLISH);
            }
        };
    }

    public static OtelSpanConverter fromBinary(InputStream is) throws IOException {
        TracesData.Builder builder = TracesData.newBuilder();

        CodedInputStream.newInstance(is)
                        .readMessage(builder,
                                     ExtensionRegistryLite.getEmptyRegistry());

        return new OtelSpanConverter(builder.build().getResourceSpansList());
    }

    private final List<ResourceSpans> resourceSpansList;

    OtelSpanConverter(List<ResourceSpans> resourceSpansList) {
        this.resourceSpansList = resourceSpansList;
    }

    public List<TraceSpan> toSpanList() {
        List<TraceSpan> spans = new ArrayList<>(32);
        for (ResourceSpans resourceSpans : resourceSpansList) {
            Map<String, String> resourceAttributes = toAttributeMap(resourceSpans.getResource().getAttributesList());

            for (ScopeSpans scopeSpans : resourceSpans.getScopeSpansList()) {
                for (Span span : scopeSpans.getSpansList()) {
                    spans.add(toInternalSpan(resourceAttributes, scopeSpans, span));
                }
            }
        }
        return spans;
    }

    protected String getTraceId(ByteString id) {
        return id.toStringUtf8();
    }

    protected String getSpanId(ByteString id) {
        return id.toStringUtf8();
    }

    private TraceSpan toInternalSpan(Map<String, String> resourceAttributes,
                                     ScopeSpans scopeSpans,
                                     Span span) {
        TraceSpan internalSpan = new TraceSpan();

        internalSpan.traceId = getTraceId(span.getTraceId());
        internalSpan.spanId = getSpanId(span.getSpanId());
        internalSpan.parentSpanId = getSpanId(span.getParentSpanId());

        internalSpan.kind = toSpanKind(span.getKind()).toString();
        internalSpan.name = span.getName();

        internalSpan.setTags(toAttributeMap(span.getAttributesList()));
        internalSpan.setStartTime(span.getStartTimeUnixNano() / 1000);
        internalSpan.setEndTime(span.getEndTimeUnixNano() / 1000);
        internalSpan.setCostTime(internalSpan.endTime - internalSpan.startTime);
        internalSpan.setStatus(toStatus(span.getStatus()));
        if (StringUtils.hasText(span.getStatus().getMessage())) {
            internalSpan.setTag(Tags.Exception.MESSAGE, span.getStatus().getMessage());
        }

        internalSpan.appName = resourceAttributes.getOrDefault("service.name",
                                                               scopeSpans.getScope().getName());
        internalSpan.instanceName = "";
        internalSpan.method = "";

        return internalSpan;
    }

    private String toStatus(Status status) {
        if (status.getCode() == Status.StatusCode.STATUS_CODE_OK) {
            return "200";
        }
        if (status.getCode() == Status.StatusCode.STATUS_CODE_ERROR) {
            return "500";
        }
        return "";
    }

    private Map<String, String> toAttributeMap(List<KeyValue> attributes) {
        if (CollectionUtils.isEmpty(attributes)) {
            return Collections.emptyMap();
        }
        Map<String, String> maps = new TreeMap<>();
        attributes.forEach((kv) -> maps.put(kv.getKey(), kv.getValue().getStringValue()));
        return maps;
    }

    private SpanKind toSpanKind(Span.SpanKind kind) {
        switch (kind) {
            case SPAN_KIND_INTERNAL:
                return SpanKind.INTERNAL;
            case SPAN_KIND_CLIENT:
                return SpanKind.CLIENT;
            case SPAN_KIND_CONSUMER:
                return SpanKind.CONSUMER;
            case SPAN_KIND_PRODUCER:
                return SpanKind.PRODUCER;
            case SPAN_KIND_SERVER:
                return SpanKind.SERVER;
            case SPAN_KIND_UNSPECIFIED:
                return SpanKind.UNSPECIFIED;
            default:
                throw new UnsupportedOperationException("Not supported kind " + kind);
        }
    }
}
