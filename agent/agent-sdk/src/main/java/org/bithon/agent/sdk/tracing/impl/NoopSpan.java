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
import java.util.Collections;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 14/5/25 10:49 pm
 */
public class NoopSpan implements ISpan {
    public static final NoopSpan INSTANCE = new NoopSpan();

    @Override
    public String traceId() {
        return "";
    }

    @Override
    public String spanId() {
        return "";
    }

    @Override
    public String parentId() {
        return "";
    }

    @Override
    public String name() {
        return "";
    }

    @Override
    public ISpanV1 name(String name) {
        return this;
    }

    @Override
    public SpanKind kind() {
        return null;
    }

    @Override
    public ISpanV1 kind(SpanKind kind) {
        return this;
    }

    @Override
    public Map<String, String> tags() {
        return Collections.emptyMap();
    }

    @Override
    public ISpanV1 tag(String name, int value) {
        return this;
    }

    @Override
    public ISpanV1 tag(String name, long value) {
        return this;
    }

    @Override
    public ISpanV1 tag(String name, String value) {
        return this;
    }

    @Override
    public ISpanV1 tag(String name, SocketAddress address) {
        return this;
    }

    @Override
    public ISpanV1 tag(String name, InetSocketAddress address) {
        return this;
    }

    @Override
    public ISpanV1 tag(String name, Object value) {
        return this;
    }

    @Override
    public ISpanV1 tag(Throwable exception) {
        return this;
    }

    @Override
    public String clazz() {
        return "";
    }

    @Override
    public ISpanV1 clazz(String clazz) {
        return this;
    }

    @Override
    public String method() {
        return "";
    }

    @Override
    public ISpanV1 method(Executable method) {
        return this;
    }

    @Override
    public ISpanV1 method(Class<?> clazz, String method) {
        return this;
    }

    @Override
    public ISpanV1 method(String clazz, String method) {
        return this;
    }

    @Override
    public long startTime() {
        return 0;
    }

    @Override
    public long endTime() {
        return 0;
    }

    @Override
    public ISpan start() {
        return this;
    }

    @Override
    public void finish() {
    }

    @Override
    public void close() {
    }
}
