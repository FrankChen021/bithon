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

package org.bithon.agent.observability.event;

import org.bithon.agent.observability.tracing.context.ITraceContext;
import org.bithon.agent.observability.tracing.context.TraceContextHolder;
import org.bithon.agent.observability.tracing.context.propagation.TraceMode;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/4/13 22:37
 */
public class ExceptionBuilder {
    private Map<String, Object> map;

    public static ExceptionBuilder builder(Map<String, String> extraArgs) {
        return new ExceptionBuilder(extraArgs);
    }

    public static ExceptionBuilder builder() {
        return new ExceptionBuilder(Collections.emptyMap());
    }

    private ExceptionBuilder(Map<String, String> extraArgs) {
        map = new HashMap<>(extraArgs);

        map.put("thread", Thread.currentThread().getName());

        ITraceContext traceContext = TraceContextHolder.current();
        if (traceContext != null && traceContext.traceMode().equals(TraceMode.TRACING)) {
            map.put("traceId", traceContext.traceId());
        }
    }

    public ExceptionBuilder exceptionClass(Class<?> clazz) {
        map.put("exceptionClass", clazz.getName());
        return this;
    }

    public ExceptionBuilder message(String message) {
        map.put("message", message);
        return this;
    }

    public ExceptionBuilder stack(Throwable throwable) {
        StringWriter stackTrace = new StringWriter(512);
        throwable.printStackTrace(new PrintWriter(stackTrace));
        map.put("stack", stackTrace.toString());
        return this;
    }

    public ExceptionBuilder stack(String stack) {
        map.put("stack", stack);
        return this;
    }

    public Map<String, Object> build() {
        return map;
    }
}
