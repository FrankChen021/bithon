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

package org.bithon.component.commons.tracing;

/**
 * @author frank.chen021@outlook.com
 * @date 25/12/21 5:56 PM
 */
public class Tags {
    /**
     * https://opentelemetry.io/docs/reference/specification/trace/semantic_conventions/http/#common-attributes
     */
    public static class Http {
        public static final String VERSION = "http.version";

        public static final String METHOD = "http.method";

        /**
         * For a {@link SpanKind#CLIENT}, the uri must be in the format of URI where the scheme represent the target service
         * <p>
         * For example: http://localhost:8080
         * <p>
         * The OpenTelemetry standard defines it as 'http.url' but for backward compatibility, it's http.uri still
         */
        public static final String URL = "http.uri";

        public static final String STATUS = "http.status";

        public static final String REQUEST_HEADER_PREFIX = "http.request.header.";
        public static final String RESPONSE_HEADER_PREFIX = "http.response.header.";

        /*
         * Below definitions are for http server
         */

        /**
         * The matched HTTP Route(Can be seen as normalized url)
         */
        public static final String ROUTE = "http.route";

        /**
         * The full request target as passed in a HTTP request line or equivalent.
         */
        public static final String TARGET = "http.target";

        public static final String CLIENT_IP = "http.client_ip";

        public static final String SCHEME = "http.scheme";
    }

    public static class Net {
        public static final String VERSION = "net.protocol.version";
        public static final String PEER_ADDR = "net.sock.peer.addr";
        public static final String PEER_PORT = "net.sock.peer.port";
    }

    public static final String SQL = "sql";

    /**
     *  redis://127.0.0.1:6379
     *  mongodb://127.0.0.1:8000
     *  mysql://127.0.0.1:3309
     */
    public static final String REMOTE_TARGET = "target";

    public static final String CLIENT_TYPE = "client.type";

    /**
     * See <a href="https://opentelemetry.io/docs/reference/specification/trace/semantic_conventions/exceptions/">Exception</a>
     */
    public static class Exception {
        public static final String MESSAGE = "exception.message";
        public static final String STACKTRACE = "exception.stacktrace";

        /**
         * The type of the exception (its fully-qualified class name, if applicable).
         * The dynamic type of the exception should be preferred over the static type in languages that support it.
         */
        public static final String TYPE = "exception.type";
    }
}
