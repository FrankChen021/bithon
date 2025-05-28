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
import org.bithon.component.commons.tracing.SpanKind;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.tracing.TraceSpan;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class JaegerSpanConverter {

    public static TraceSpan convert(Span jaegerSpan) {
        TraceSpan span = new TraceSpan();

        span.traceId = StringUtils.format("%016x%016x", jaegerSpan.getTraceIdHigh(), jaegerSpan.getTraceIdLow());
        span.spanId = Long.toHexString(jaegerSpan.getSpanId());
        span.parentSpanId = jaegerSpan.getParentSpanId() == 0 ? "" : Long.toHexString(jaegerSpan.getParentSpanId());
        span.name = jaegerSpan.getOperationName();
        span.startTime = jaegerSpan.getStartTime();
        span.endTime = jaegerSpan.getStartTime() + jaegerSpan.getDuration();
        span.costTime = jaegerSpan.getDuration();
        span.kind = convertSpanKind(jaegerSpan).name();
        span.tags = convertTags(jaegerSpan);

        // Set default values for required fields
        span.clazz = "";
        span.method = "";
        span.appName = "";
        span.instanceName = "";

        // Logs in the span are ignored

        return span;
    }

    private static SpanKind convertSpanKind(Span span) {
        String spanKind = getTagValue(span, "span.kind", "internal");
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

    private static String getTagValue(Span span, String key, String defaultValue) {
        if (span.getTags() != null) {
            for (Tag tag : span.getTags()) {
                if (tag.getKey().equals(key)) {
                    return getTagValue(tag);
                }
            }
        }
        return defaultValue;
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
