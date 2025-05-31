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

import io.jaegertracing.thriftjava.Span;
import io.jaegertracing.thriftjava.Tag;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.tracing.SpanKind;
import org.bithon.component.commons.tracing.Tags;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.tracing.TraceSpan;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
public class JaegerSpanConverter {

    public static TraceSpan convert(ApplicationInstance instance, Span jaegerSpan) {
        TraceSpan span = new TraceSpan();

        span.traceId = StringUtils.format("%016x%016x", jaegerSpan.getTraceIdHigh(), jaegerSpan.getTraceIdLow());
        span.spanId = Long.toHexString(jaegerSpan.getSpanId());
        span.parentSpanId = jaegerSpan.getParentSpanId() == 0 ? "" : Long.toHexString(jaegerSpan.getParentSpanId());
        span.name = jaegerSpan.getOperationName();
        span.startTime = jaegerSpan.getStartTime();
        span.endTime = jaegerSpan.getStartTime() + jaegerSpan.getDuration();
        span.costTime = jaegerSpan.getDuration();
        span.tags = convertTags(jaegerSpan);
        span.kind = convertSpanKind(span.tags.remove("span.kind")).name();

        // Set default values for required fields
        span.clazz = "";
        span.method = "";
        span.appName = instance.getApplicationName();
        span.instanceName = instance.getInstanceName();

        String status = span.tags.remove("http.status_code");
        if (status != null) {
            span.tags.put(Tags.Http.STATUS, status);
        }

        return span;
    }

    private static SpanKind convertSpanKind(String kind) {
        String spanKind = kind == null ? "" : kind.trim();
        return switch (spanKind.toLowerCase(Locale.ENGLISH)) {
            case "server" -> SpanKind.SERVER;
            case "client" -> SpanKind.CLIENT;
            case "producer" -> SpanKind.PRODUCER;
            case "consumer" -> SpanKind.CONSUMER;
            default -> SpanKind.INTERNAL;
        };
    }

    private static Map<String, String> convertTags(Span span) {
        Map<String, String> tags = new TreeMap<>();
        if (span.getTags() != null) {
            for (Tag tag : span.getTags()) {
                tags.put(tag.getKey(), getTagValue(tag));
            }
        }
        return tags;
    }

    private static String getTagValue(Tag tag) {
        return switch (tag.getVType()) {
            case STRING -> tag.getVStr();
            case BOOL -> String.valueOf(tag.isVBool());
            case LONG -> String.valueOf(tag.getVLong());
            case DOUBLE -> String.valueOf(tag.getVDouble());
            case BINARY -> new String(tag.getVBinary(), StandardCharsets.UTF_8);
        };
    }
}
