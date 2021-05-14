/*
 *    Copyright 2020 bithon.cn
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

package com.sbss.bithon.agent.core.tracing.context;

import java.lang.reflect.Executable;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/4/7 8:44 下午
 */
public class TraceSpanBuilder {

    static class NoopSpan extends TraceSpan {
        public NoopSpan(String spanId,
                        String parentSpanId,
                        TraceContext traceContext) {
            super(spanId, parentSpanId, traceContext);
        }

        static NoopSpan INSTANCE = new NoopSpan(null, null, null);

        @Override
        public boolean isNull() {
            return true;
        }

        @Override
        public TraceSpan start() {
            return this;
        }

        @Override
        public void finish() {
        }

        @Override
        public TraceSpan tag(String name, String value) {
            return this;
        }

        @Override
        public TraceSpan tag(Throwable exception) {
            return this;
        }

        @Override
        public TraceSpan clazz(Class<?> clazz) {
            return this;
        }

        @Override
        public TraceSpan method(Executable method) {
            return this;
        }
    }

    public static TraceSpan build(String name) {
        TraceContext traceContext = TraceContextHolder.get();
        if (traceContext == null) {
            return NoopSpan.INSTANCE;
        }

        TraceSpan parentSpan = traceContext.currentSpan();
        if (parentSpan == null) {
            return NoopSpan.INSTANCE;
        }

        // create a span and save it in user-context
        return parentSpan.newChildSpan(name);
    }
}
