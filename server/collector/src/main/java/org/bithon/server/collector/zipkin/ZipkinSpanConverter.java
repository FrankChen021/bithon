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

package org.bithon.server.collector.zipkin;

import org.bithon.component.commons.tracing.SpanKind;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.tracing.TraceSpan;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Converts Zipkin spans to Bithon trace spans
 */
public class ZipkinSpanConverter {

    public static TraceSpan convertToTraceSpan(ZipkinSpan zipkinSpan) {
        TraceSpan bithonSpan = new TraceSpan();

        // Set basic span attributes
        bithonSpan.traceId = zipkinSpan.getTraceId();
        bithonSpan.spanId = zipkinSpan.getId();
        bithonSpan.parentSpanId = StringUtils.getOrEmpty(zipkinSpan.getParentId());
        bithonSpan.name = zipkinSpan.getName();
        bithonSpan.clazz = "";
        bithonSpan.method = "";

        // Map Zipkin kind to Bithon kind
        if (zipkinSpan.getKind() != null) {
            switch (zipkinSpan.getKind().toUpperCase(Locale.ENGLISH)) {
                case "CLIENT":
                    bithonSpan.kind = SpanKind.CLIENT.name();
                    break;
                case "SERVER":
                    bithonSpan.kind = SpanKind.SERVER.name();
                    break;
                case "PRODUCER":
                    bithonSpan.kind = SpanKind.PRODUCER.name();
                    break;
                case "CONSUMER":
                    bithonSpan.kind = SpanKind.CONSUMER.name();
                    break;
                default:
                    bithonSpan.kind = SpanKind.INTERNAL.name();
                    break;
            }
        } else {
            bithonSpan.kind = bithonSpan.parentSpanId.isEmpty() ? SpanKind.SERVER.name() : SpanKind.INTERNAL.name();
        }

        bithonSpan.startTime = zipkinSpan.getTimestamp();
        bithonSpan.costTime = zipkinSpan.getDuration();
        bithonSpan.endTime = bithonSpan.startTime + zipkinSpan.getDuration();

        // Copy tags
        Map<String, String> tags = new TreeMap<>();
        if (zipkinSpan.getTags() != null) {
            tags.putAll(zipkinSpan.getTags());
        }

        // Add endpoint information to tags
        if (zipkinSpan.getLocalEndpoint() != null) {
            if (zipkinSpan.getLocalEndpoint().getServiceName() != null) {
                bithonSpan.appName = zipkinSpan.getLocalEndpoint().getServiceName();
            }
            bithonSpan.instanceName = zipkinSpan.getLocalEndpoint().getAddress();
        }

        if (zipkinSpan.getRemoteEndpoint() != null) {
            if (zipkinSpan.getRemoteEndpoint().getServiceName() != null) {
                tags.put("net.peer.name", zipkinSpan.getRemoteEndpoint().getServiceName());
            }
            tags.put("net.peer", zipkinSpan.getRemoteEndpoint().getAddress());
        }

        // Add annotations as tags
        if (zipkinSpan.getAnnotations() != null) {
            for (ZipkinSpan.ZipkinAnnotation annotation : zipkinSpan.getAnnotations()) {
                if (annotation.getValue() != null) {
                    tags.put("annotation." + annotation.getTimestamp(), annotation.getValue());
                }
            }
        }

        // Set debug and shared flags
        if (zipkinSpan.getDebug() != null && zipkinSpan.getDebug()) {
            tags.put("debug", "true");
        }
        if (zipkinSpan.getShared() != null && zipkinSpan.getShared()) {
            tags.put("shared", "true");
        }

        bithonSpan.tags = tags;

        return bithonSpan;
    }
}
