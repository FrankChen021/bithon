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

    ITraceSpan tag(Throwable exception);

    default ITraceSpan tag(Consumer<ITraceSpan> config) {
        config.accept(this);
        return this;
    }

    ITraceSpan arg(String name, String value);

    Map<String, String> args();

    String parentApplication();

    ITraceSpan parentApplication(String sourceApp);

    String clazz();

    String method();

    ITraceSpan method(Executable method);

    ITraceSpan method(String method);

    long startTime();

    long endTime();

    ITraceSpan newChildSpan(String name);

    ITraceSpan start();

    void finish();

    default <T> ITraceSpan propagate(T injectedTo, PropagationSetter<T> setter) {
        context().propagate(injectedTo, setter);
        return this;
    }

    default boolean isNull() {
        return false;
    }
}
