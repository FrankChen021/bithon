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

import java.util.Locale;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/5 8:51 下午
 */
public enum SpanKind {
    /**
     * Unspecified.
     */
    NONE,

    /**
     * A client is a termination of trace in current context.
     * It spreads the trace context to next hop.
     * <p>
     * For such type, 'targetType' and 'uri' must be filled in `tags`
     */
    CLIENT,

    /**
     * Indicates that the span covers server-side handling of an RPC or other remote network request.
     */
    SERVER,

    /**
     * Indicates that the span describes producer sending a message to a broker.
     * Unlike client and server, there is no direct critical path latency relationship between producer and consumer spans (e.g. publishing a message to a pubsub service).
     */
    PRODUCER,

    /**
     * Indicates that the span describes consumer receiving a message from a broker.
     * Unlike client and server, there is no direct critical path latency relationship between producer and consumer spans (e.g. receiving a message from a pubsub service subscription).
     */
    CONSUMER,

    /**
     * Bithon extended.
     * For timer-based routines such as spring-quartz or spring-scheduling or sth else.
     * Maybe it's not a good name.
     */
    TIMER;

    SpanKind() {
    }

    public static boolean isRootSpan(String kind) {
        kind = kind.toUpperCase(Locale.ENGLISH);
        return SERVER.name().equals(kind) || TIMER.name().equals(kind) || CONSUMER.name().equals(kind);
    }
}
