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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Zipkin Span V2 format as defined in
 * https://zipkin.io/zipkin-api/zipkin2-api.yaml
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ZipkinSpan {
    private String id;
    private String traceId;
    private String parentId;
    private String name;
    private String kind;
    private Long timestamp;
    private Long duration;
    private Boolean debug;
    private Boolean shared;
    private Map<String, String> tags;
    private List<ZipkinAnnotation> annotations;
    private ZipkinEndpoint localEndpoint;
    private ZipkinEndpoint remoteEndpoint;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ZipkinAnnotation {
        private Long timestamp;
        private String value;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ZipkinEndpoint {
        private String serviceName;
        private String ipv4;
        private String ipv6;
        private Integer port;
    }
} 