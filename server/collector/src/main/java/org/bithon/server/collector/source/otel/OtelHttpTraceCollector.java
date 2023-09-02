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

package org.bithon.server.collector.source.otel;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.ExtensionRegistryLite;
import com.google.protobuf.util.JsonFormat;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import io.opentelemetry.proto.trace.v1.Status;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.tracing.SpanKind;
import org.bithon.component.commons.tracing.Tags;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.sink.tracing.ITraceMessageSink;
import org.bithon.server.storage.tracing.TraceSpan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/9/1 20:59
 */
@Slf4j
@RestController
@ConditionalOnProperty(value = "collector-otel.enabled", havingValue = "true")
public class OtelHttpTraceCollector {

    private final ITraceMessageSink traceSink;

    public OtelHttpTraceCollector(@Qualifier("trace-sink-collector") ITraceMessageSink traceSink) {
        this.traceSink = traceSink;
    }

    @PostMapping("/api/collector/otel/trace")
    public void collectBinaryFormattedTrace(HttpServletRequest request,
                                            HttpServletResponse response) throws IOException {

        InputStream is;
        String encoding = request.getHeader("Content-Encoding");
        if (!StringUtils.isEmpty(encoding)) {
            if ("gzip".equals(encoding)) {
                is = new GZIPInputStream(request.getInputStream());
            } else if ("deflate".equals(encoding)) {
                is = new InflaterInputStream(request.getInputStream());
            } else {
                String message = StringUtils.format("Not supported Content-Encoding [%s] from remote [%s]", encoding, request.getRemoteAddr());
                response.getWriter().println(message);
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                return;
            }
        } else {
            is = request.getInputStream();
        }

        ResourceSpans resourceSpans;
        if ("application/x-protobuf".equals(request.getContentType())) {
            resourceSpans = fromBinary(is);
        } else if ("application/json".equals(request.getContentType())) {
            resourceSpans = fromJson(is);
        } else {
            String message = StringUtils.format("Not supported Content-Type [%s] from remote [%s]", request.getContentType(), request.getRemoteAddr());
            response.getWriter().println(message);
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            return;
        }

        List<TraceSpan> spans = new ArrayList<>(32);
        for (ScopeSpans scopeSpans : resourceSpans.getScopeSpansList()) {
            for (Span span : scopeSpans.getSpansList()) {
                spans.add(toInternalSpan(scopeSpans, span));
            }
        }

        if (CollectionUtils.isEmpty(spans)) {
            return;
        }

        this.traceSink.process("trace", spans);
    }

    private ResourceSpans fromJson(InputStream is) throws IOException {
        ResourceSpans.Builder builder = ResourceSpans.newBuilder();

        try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            JsonFormat.parser().merge(reader, builder);
        }

        return builder.build();
    }

    private ResourceSpans fromBinary(InputStream is) throws IOException {
        ResourceSpans.Builder builder = ResourceSpans.newBuilder();

        CodedInputStream.newInstance(is)
                        .readMessage(builder,
                                     ExtensionRegistryLite.getEmptyRegistry());

        return builder.build();
    }

    private TraceSpan toInternalSpan(ScopeSpans scopeSpans, Span span) {
        TraceSpan internalSpan = new TraceSpan();
        internalSpan.traceId = span.getTraceId().toStringUtf8();
        internalSpan.spanId = span.getSpanId().toStringUtf8();
        internalSpan.parentSpanId = span.getParentSpanId().toStringUtf8();
        internalSpan.kind = toSpanKind(span.getKind()).toString();
        internalSpan.name = span.getName();
        internalSpan.setTags(toAttributeMap(span.getAttributesList()));
        internalSpan.setStartTime(span.getStartTimeUnixNano() / 1000);
        internalSpan.setEndTime(span.getEndTimeUnixNano() / 1000);
        internalSpan.setStatus(toStatus(span.getStatus()));
        if (StringUtils.hasText(span.getStatus().getMessage())) {
            internalSpan.setTag(Tags.Exception.MESSAGE, span.getStatus().getMessage());
        }

        internalSpan.appName = scopeSpans.getScope().getName();

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
