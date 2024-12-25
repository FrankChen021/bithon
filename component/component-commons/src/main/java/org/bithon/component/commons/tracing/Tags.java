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
 * https://github.com/open-telemetry/opentelemetry-specification/blob/v1.20.0/specification/trace/semantic_conventions
 *
 * @author frank.chen021@outlook.com
 * @date 25/12/21 5:56 PM
 */
public class Tags {
    /**
     * See: https://github.com/open-telemetry/opentelemetry-specification/blob/v1.20.0/specification/trace/semantic_conventions/http.md
     */
    public static class Http {
        /**
         * Non-standard name
         * The type name of http server.
         * For example, webflux, tomcat, undertow...
         */
        public static final String SERVER = "http.server";
        /**
         * Non-standard name
         * The type name of http client
         */
        public static final String CLIENT = "http.client";

        public static final String VERSION = "http.version";

        public static final String METHOD = "http.method";

        /**
         * For a {@link SpanKind#CLIENT}, the uri must be in the format of URI where the scheme represent the target service
         * <p>
         * For example: http://localhost:8080
         * <p>
         */
        public static final String URL = "http.url";

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
         * The full request target as passed in an HTTP request line or equivalent.
         */
        public static final String TARGET = "http.target";

        public static final String CLIENT_IP = "http.client_ip";

        public static final String SCHEME = "http.scheme";
    }

    public static class Net {
        public static final String VERSION = "net.protocol.version";

        /**
         * Not an OpenTelemetry standard, it's net.peer.name + net.peer.port
         */
        public static final String PEER = "net.peer";

        public static final String PEER_NAME = "net.peer.name";
        public static final String PEER_PORT = "net.peer.port";

        public static final String PEER_ADDR = "net.sock.peer.addr";
    }

    /**
     * redis://127.0.0.1:6379
     * mongodb://127.0.0.1:8000
     * mysql://127.0.0.1:3309
     */
    public static final String REMOTE_TARGET = "target";

    /**
     * See <a href="https://opentelemetry.io/docs/reference/specification/trace/semantic_conventions/exceptions/">Exception</a>
     */
    public static class Exception {
        // Not a standard
        public static final String CODE = "exception.code";

        public static final String MESSAGE = "exception.message";
        public static final String STACKTRACE = "exception.stacktrace";

        /**
         * The type of the exception (its fully-qualified class name, if applicable).
         * The dynamic type of the exception should be preferred over the static type in languages that support it.
         */
        public static final String TYPE = "exception.type";
    }

    /**
     * https://opentelemetry.io/docs/reference/specification/trace/semantic_conventions/database/#connection-level-attributes
     */
    public static class Database {
        public static final String PREFIX = "db.";

        /**
         * The name of database, such as mysql, db2, sqlite, etc.
         * <p>
         * Connection level
         */
        public static final String SYSTEM = "db.system";

        public static final String CONNECTION_STRING = "db.connection_string";

        public static final String USER = "db.user";

        /**
         * database name
         */
        public static final String NAME = "db.name";

        /**
         * The statement(SQL) that is being executed
         */
        public static final String STATEMENT = "db.statement";

        /**
         * The type of statement such as select/delete/update
         */
        public static final String OPERATION = "db.operation";

        public static final String REDIS_DB_INDEX = "db.redis.database_index";
        public static final String MONGODB_DB_COLLECTION = "db.mongodb.collection";

        // Non standard
        public static final String MONGODB_DB_COMMAND = "db.mongodb.command";
    }

    /**
     * https://opentelemetry.io/docs/reference/specification/trace/semantic_conventions/messaging/
     */
    public static class Messaging {
        public static final String SYSTEM = "messaging.system";
        public static final String COUNT = "messaging.batch.message_count";
        public static final String BYTES = "messaging.message.payload_size_bytes";

        public static final String KAFKA_HEADER_PREFIX = "message.kafka.header.";
        /**
         * This is custom tag name
         * The spec uses different tag name for producer and consumer, it's complicated.
         */
        public static final String KAFKA_TOPIC = "messaging.kafka.topic";

        public static final String KAFKA_CONSUMER_GROUP = "messaging.kafka.consumer.group";
        public static final String KAFKA_CLIENT_ID = "messaging.kafka.client_id";
        public static final String KAFKA_SOURCE_PARTITION = "messaging.kafka.source.partition";
    }

    /**
     * https://opentelemetry.io/docs/reference/specification/trace/semantic_conventions/span-general/#general-thread-attributes
     */
    public static class Thread {
        public static final String ID = "thread.id";
        public static final String NAME = "thread.name";

        // Non standard
        public static final String POOL_CLASS = "thread.pool.class";
        public static final String POOL_NAME = "thread.pool.name";
        public static final String POOL_PARALLELISM = "thread.pool.parallelism";
    }

    /**
     *
     */
    public static class Rpc {
        /**
         * example: grpc
         */
        public static final String SYSTEM = "rpc.system";

        public static final String REQUEST_META_PREFIX = "rpc.grpc.request.metadata.";

        /**
         * https://opentelemetry.io/docs/specs/semconv/rpc/rpc-metrics/#metric-rpcserverrequestsize
         */
        public static final String RPC_SERVER_REQ_SIZE = "rpc.server.request.size";
        public static final String RPC_SERVER_RSP_SIZE = "rpc.server.response.size";

        /**
         * https://opentelemetry.io/docs/specs/semconv/rpc/rpc-metrics/#metric-rpcclientrequestsize
         */
        public static final String RPC_CLIENT_REQ_SIZE = "rpc.client.request.size";
        public static final String RPC_CLIENT_RSP_SIZE = "rpc.client.response.size";
    }
}
