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

package org.bithon.agent.plugin.bithon.sdk.tracing;


import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.sdk.tracing.ISpan;
import org.bithon.agent.sdk.tracing.SpanKind;
import org.bithon.agent.sdk.tracing.impl.ISpanV1;

import java.lang.reflect.Executable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 14/5/25 8:40 pm
 */
public class SpanImpl implements ISpan {
    private final ITraceSpan delegate;

    public SpanImpl(ITraceSpan delegate) {
        this.delegate = delegate.kind(org.bithon.component.commons.tracing.SpanKind.INTERNAL)
                                .name("")
                                .method("", "");
    }

    @Override
    public String traceId() {
        return delegate.traceId();
    }

    @Override
    public String spanId() {
        return delegate.spanId();
    }

    @Override
    public String parentId() {
        return delegate.parentSpanId();
    }

    @Override
    public String name() {
        return delegate.name();
    }

    @Override
    public ISpanV1 name(String name) {
        if (name != null) {
            delegate.name(name.trim());
        }
        return this;
    }

    @Override
    public ISpanV1 kind(SpanKind kind) {
        if (kind == SpanKind.CLIENT) {
            delegate.kind(org.bithon.component.commons.tracing.SpanKind.CLIENT);
        } else if (kind == SpanKind.SERVER) {
            delegate.kind(org.bithon.component.commons.tracing.SpanKind.SERVER);
        } else if (kind == SpanKind.PRODUCER) {
            delegate.kind(org.bithon.component.commons.tracing.SpanKind.PRODUCER);
        } else if (kind == SpanKind.CONSUMER) {
            delegate.kind(org.bithon.component.commons.tracing.SpanKind.CONSUMER);
        } else if (kind == SpanKind.INTERNAL) {
            delegate.kind(org.bithon.component.commons.tracing.SpanKind.INTERNAL);
        }
        return this;
    }

    @Override
    public SpanKind kind() {
        org.bithon.component.commons.tracing.SpanKind kind = delegate.kind();
        if (kind == org.bithon.component.commons.tracing.SpanKind.CLIENT) {
            return SpanKind.CLIENT;
        }
        if (kind == org.bithon.component.commons.tracing.SpanKind.SERVER) {
            return SpanKind.SERVER;
        }
        if (kind == org.bithon.component.commons.tracing.SpanKind.PRODUCER) {
            return SpanKind.PRODUCER;
        }
        if (kind == org.bithon.component.commons.tracing.SpanKind.CONSUMER) {
            return SpanKind.CONSUMER;
        }
        if (kind == org.bithon.component.commons.tracing.SpanKind.INTERNAL) {
            return SpanKind.INTERNAL;
        }
        throw new IllegalStateException("Unknown span kind: " + kind);
    }


    @Override
    public Map<String, String> tags() {
        return delegate.tags();
    }

    @Override
    public ISpanV1 tag(String name, int value) {
        delegate.tag(name, value);
        return this;
    }

    @Override
    public ISpanV1 tag(String name, long value) {
        delegate.tag(name, value);
        return this;
    }

    @Override
    public ISpanV1 tag(String name, String value) {
        delegate.tag(name, value);
        return this;
    }

    @Override
    public ISpanV1 tag(String name, Object value) {
        delegate.tag(name, value);
        return this;
    }

    @Override
    public ISpanV1 tag(String name, SocketAddress address) {
        delegate.tag(name, address);
        return this;
    }

    @Override
    public ISpanV1 tag(String name, InetSocketAddress address) {
        delegate.tag(name, address);
        return this;
    }

    @Override
    public ISpanV1 tag(Throwable exception) {
        delegate.tag(exception);
        return this;
    }

    @Override
    public String clazz() {
        return delegate.clazz();
    }

    @Override
    public ISpanV1 clazz(String clazz) {
        if (clazz != null) {
            this.delegate.clazz(clazz.trim());
        }
        return this;
    }

    @Override
    public String method() {
        return delegate.method();
    }

    @Override
    public ISpanV1 method(String clazz, String method) {
        if (clazz != null && method != null) {
            this.delegate.clazz(clazz.trim());
        }
        return this;
    }

    @Override
    public ISpanV1 method(Executable method) {
        if (method != null) {
            delegate.method(method);
        }
        return this;
    }

    @Override
    public ISpanV1 method(Class<?> clazz, String method) {
        delegate.method(clazz, method);
        return this;
    }

    @Override
    public long startTime() {
        return delegate.startTime();
    }

    @Override
    public long endTime() {
        return delegate.endTime();
    }

    @Override
    public ISpan start() {
        delegate.start();
        return this;
    }

    @Override
    public void finish() {
        delegate.finish();
    }

    @Override
    public void close() {
        delegate.finish();
    }
}
