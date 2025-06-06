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

import lombok.Data;
import org.bithon.component.commons.tracing.SpanKind;
import org.bithon.component.commons.tracing.Tags;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.tracing.TraceSpan;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Zipkin Span V2 format as defined in
 * https://zipkin.io/zipkin-api/zipkin2-api.yaml
 */
@Data
public class ZipkinSpan {
    private String id;
    private String traceId;
    private String parentId;
    private String name;
    private String kind;

    /**
     * timestamp in microseconds
     */
    private long timestamp;
    private Long duration;
    private Boolean debug;
    private Boolean shared;
    private Map<String, String> tags;
    private List<ZipkinAnnotation> annotations;
    private ZipkinEndpoint localEndpoint;
    private ZipkinEndpoint remoteEndpoint;

    @Data
    public static class ZipkinAnnotation {
        private Long timestamp;
        private String value;
    }

    @Data
    public static class ZipkinEndpoint {
        private String serviceName;
        private String ipv4;
        private String ipv6;
        private Integer port;

        public String getAddress() {
            if (ipv4 != null) {
                return port == null ? ipv4 : ipv4 + ":" + port;
            } else if (ipv6 != null) {
                return port == null ? ipv6 : "[" + ipv6 + "]:" + port;
            }

            // SHOULD never happen
            return null;
        }
    }

    public TraceSpan toTraceSpan() {
        TraceSpan span = new TraceSpan();

        // Set basic span attributes
        span.traceId = this.getTraceId();
        span.spanId = this.getId();
        span.parentSpanId = StringUtils.getOrEmpty(this.getParentId());
        span.name = this.getName();
        span.clazz = "";
        span.method = "";

        // Map Zipkin kind to Bithon kind
        if (this.getKind() != null) {
            switch (this.getKind().toUpperCase(Locale.ENGLISH)) {
                case "CLIENT":
                    span.kind = SpanKind.CLIENT.name();
                    break;
                case "SERVER":
                    span.kind = SpanKind.SERVER.name();
                    break;
                case "PRODUCER":
                    span.kind = SpanKind.PRODUCER.name();
                    break;
                case "CONSUMER":
                    span.kind = SpanKind.CONSUMER.name();
                    break;
                default:
                    span.kind = SpanKind.INTERNAL.name();
                    break;
            }
        } else {
            span.kind = span.parentSpanId.isEmpty() ? SpanKind.SERVER.name() : SpanKind.INTERNAL.name();
        }

        span.startTime = this.getTimestamp();
        span.costTime = this.getDuration();
        span.endTime = span.startTime + this.getDuration();

        // Copy tags
        Map<String, String> tags = new TreeMap<>();
        if (this.getTags() != null) {
            tags.putAll(this.getTags());
        }

        // Add endpoint information to tags
        if (this.getLocalEndpoint() != null) {
            if (this.getLocalEndpoint().getServiceName() != null) {
                span.appName = this.getLocalEndpoint().getServiceName();
            }
            span.instanceName = this.getLocalEndpoint().getAddress();
        }

        if (this.getRemoteEndpoint() != null) {
            if (this.getRemoteEndpoint().getServiceName() != null) {
                tags.put("net.peer.name", this.getRemoteEndpoint().getServiceName());
            }
            tags.put("net.peer", this.getRemoteEndpoint().getAddress());
        }

        // Add annotations as tags
        if (this.getAnnotations() != null) {
            for (ZipkinSpan.ZipkinAnnotation annotation : this.getAnnotations()) {
                if (annotation.getValue() != null) {
                    tags.put("annotation." + annotation.getTimestamp(), annotation.getValue());
                }
            }
        }

        if ("SERVER".equals(span.kind)) {
            String statusCode = tags.remove("http.status_code");
            if (statusCode == null || statusCode.isEmpty()) {
                // If no status code is provided, assume 200 OK
                tags.put(Tags.Http.STATUS, "200");
            } else {
                tags.put(Tags.Http.STATUS, statusCode);
            }
        }

        span.tags = tags;

        return span;
    }
}
