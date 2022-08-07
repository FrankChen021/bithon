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

package org.bithon.agent.core.tracing.context;

import org.bithon.agent.core.tracing.propagation.injector.PropagationSetter;

import java.lang.reflect.Executable;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 22/10/21 9:16 pm
 */
public class NullTraceSpan implements ITraceSpan {

    public static final ITraceSpan INSTANCE = new NullTraceSpan() {
        @Override
        public <T> ITraceSpan propagate(T injectedTo, PropagationSetter<T> setter) {
            return this;
        }
    };

    @Override
    public ITraceContext context() {
        return null;
    }

    @Override
    public String spanId() {
        return null;
    }

    @Override
    public String parentSpanId() {
        return null;
    }

    @Override
    public SpanKind kind() {
        return null;
    }

    @Override
    public ITraceSpan kind(SpanKind kind) {
        return this;
    }

    @Override
    public String component() {
        return null;
    }

    @Override
    public ITraceSpan component(String component) {
        return this;
    }

    @Override
    public Map<String, String> tags() {
        return null;
    }

    @Override
    public ITraceSpan tag(String name, String value) {
        return this;
    }

    @Override
    public ITraceSpan tag(Throwable exception) {
        return this;
    }

    @Override
    public String parentApplication() {
        return null;
    }

    @Override
    public ITraceSpan parentApplication(String sourceApp) {
        return this;
    }

    @Override
    public String clazz() {
        return null;
    }

    @Override
    public String method() {
        return null;
    }

    @Override
    public ITraceSpan method(Executable method) {
        return this;
    }

    @Override
    public ITraceSpan method(String method) {
        return this;
    }

    @Override
    public ITraceSpan clazz(String clazz) {
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
    public ITraceSpan newChildSpan(String name) {
        return this;
    }

    @Override
    public ITraceSpan start() {
        return this;
    }

    @Override
    public void finish() {

    }

    @Override
    public boolean isNull() {
        return true;
    }
}
