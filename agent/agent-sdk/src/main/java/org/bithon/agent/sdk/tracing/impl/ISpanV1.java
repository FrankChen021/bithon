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

package org.bithon.agent.sdk.tracing.impl;


import org.bithon.agent.sdk.tracing.ISpan;
import org.bithon.agent.sdk.tracing.SpanKind;

import java.lang.reflect.Executable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 14/5/25 8:29 pm
 */
@SuppressWarnings("ALL")
public interface ISpanV1 extends AutoCloseable {
    String traceId();

    String spanId();

    String parentId();

    String name();

    ISpanV1 name(String name);

    SpanKind kind();

    ISpanV1 kind(SpanKind kind);

    Map<String, String> tags();

    ISpanV1 tag(String name, int value);

    ISpanV1 tag(String name, long value);

    ISpanV1 tag(String name, String value);

    ISpanV1 tag(String name, SocketAddress address);

    ISpanV1 tag(String name, InetSocketAddress address);

    ISpanV1 tag(String name, Object value);

    /**
     * @return the exception Non null
     */
    ISpanV1 tag(Throwable exception);

    String clazz();

    /**
     * @param clazz
     */
    ISpanV1 clazz(String clazz);

    String method();

    /**
     * @param method Non null
     */
    ISpanV1 method(Executable method);

    /**
     * @param clazz  Non null
     * @param method Non null
     */
    ISpanV1 method(Class<?> clazz, String method);

    /**
     * @param clazz  Non null
     * @param method Non null
     */
    ISpanV1 method(String clazz, String method);

    /**
     * start timestamp in microseconds
     */
    long startTime();

    /**
     * end timestamp in microseconds
     */
    long endTime();

    ISpan start();

    /**
     * Finish a span.
     * The implementation should guarantee that multiple calls on this method are safe.
     */
    void finish();

    /**
     * Override to remove the throws specification
     */
    @Override
    void close();
}
