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
import org.bithon.agent.sdk.tracing.ITraceScope;
import org.bithon.agent.sdk.tracing.TracingMode;

/**
 * @author frank.chen021@outlook.com
 * @date 7/9/25 3:48 pm
 */
public interface ITraceScopeV1 extends AutoCloseable {

    /**
     * Gets the trace ID associated with this scope
     * @return the trace ID, or null if not available
     */
    String currentTraceId();

    /**
     * Gets the tracing mode for this scope
     * @return the tracing mode
     */
    TracingMode tracingMode();

    /**
     * Attaches this scope's context to the current thread
     * @return this scope for method chaining
     */
    ITraceScope attach();

    /**
     * Attaches this scope's context to the current thread with option to start the root span
     * @param startSpan whether to start the root span when attaching
     * @return this scope for method chaining
     */
    ITraceScope attach(boolean startSpan);

    /**
     * Detaches this scope's context from the current thread
     */
    void detach();

    /**
     * Gets the underlying span for direct manipulation
     * @return the underlying span, or a no-op span if not available
     */
    ISpan currentSpan();

    /**
     * Finishes the underlying span and detaches from current thread
     */
    @Override
    void close();
}