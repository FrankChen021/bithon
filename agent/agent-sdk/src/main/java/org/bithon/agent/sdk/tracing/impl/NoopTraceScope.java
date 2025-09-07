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
import org.bithon.agent.sdk.tracing.TraceScope;
import org.bithon.agent.sdk.tracing.TracingMode;

/**
 * No-op implementation of TraceScope used when the agent is not loaded
 * 
 * @author frank.chen021@outlook.com
 * @date 8/5/25 6:30 pm
 */
public class NoopTraceScope implements TraceScope {
    public static final NoopTraceScope INSTANCE = new NoopTraceScope();

    @Override
    public String getTraceId() {
        return "";
    }

    @Override
    public String getSpanId() {
        return "";
    }

    @Override
    public TracingMode getTracingMode() {
        return TracingMode.LOGGING;
    }

    @Override
    public TraceScope attach() {
        return this;
    }

    @Override
    public TraceScope detach() {
        return this;
    }

    @Override
    public ISpan startSpan(String operationName) {
        return NoopSpan.INSTANCE;
    }

    @Override
    public ISpan getSpan() {
        return NoopSpan.INSTANCE;
    }

    @Override
    public void close() {
        // No-op
    }
}
