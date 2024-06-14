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

package org.bithon.agent.observability.tracing.context;

import org.bithon.component.commons.tracing.SpanKind;

import java.lang.reflect.Executable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/5 8:49 下午
 */
public interface ITraceSpan {

    ITraceContext context();

    default String traceId() {
        return context().traceId();
    }

    String spanId();

    String parentSpanId();

    SpanKind kind();

    ITraceSpan kind(SpanKind kind);

    String component();

    ITraceSpan component(String component);

    Map<String, String> tags();

    ITraceSpan tag(String name, String value);

    default ITraceSpan tag(String name, SocketAddress address) {
        if (address != null) {
            if (address instanceof InetSocketAddress) {
                return tag(name, (InetSocketAddress) address);
            } else {
                tag(name, address.toString());
            }
        }
        return this;
    }

    default ITraceSpan tag(String name, InetSocketAddress address) {
        if (address == null) {
            return this;
        }
        InetAddress addr = address.getAddress();
        if (addr == null) {
            return this;
        }

        tag(name, addr.getHostAddress() + ":" + address.getPort());
        return this;
    }

    default ITraceSpan tag(String name, int value) {
        tag(name, String.valueOf(value));
        return this;
    }

    default ITraceSpan tag(String name, long value) {
        tag(name, String.valueOf(value));
        return this;
    }

    default ITraceSpan tag(String name, Object value) {
        if (value != null) {
            tag(name, value.toString());
        }
        return this;
    }

    ITraceSpan tag(Throwable exception);

    default ITraceSpan config(Consumer<ITraceSpan> config) {
        config.accept(this);
        return this;
    }

    default ITraceSpan configIfTrue(boolean expression, Consumer<ITraceSpan> config) {
        if (expression) {
            config.accept(this);
        }
        return this;
    }

    String parentApplication();

    ITraceSpan parentApplication(String sourceApp);

    String clazz();

    String method();

    default ITraceSpan method(Executable method) {
        return method(method.getDeclaringClass().getName(), method.getName());
    }

    default ITraceSpan method(Class<?> clazz, String method) {
        return method(clazz.getName(), method);
    }

    /**
     * @param clazz  Non null
     * @param method Non null
     */
    ITraceSpan method(String clazz, String method);

    ITraceSpan clazz(String clazz);

    /**
     * start timestamp in microseconds
     */
    long startTime();

    /**
     * end timestamp in microseconds
     */
    long endTime();

    ITraceSpan newChildSpan(String name);

    ITraceSpan start();

    /**
     * Finish a span.
     * The implementation should guarantee that multiple calls on this method are safe.
     */
    void finish();
}
