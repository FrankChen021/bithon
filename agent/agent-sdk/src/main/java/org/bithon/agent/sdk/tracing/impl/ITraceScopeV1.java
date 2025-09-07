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
import org.bithon.agent.sdk.tracing.TracingMode;

/**
 * @author frank.chen021@outlook.com
 * @date 7/9/25 3:48 pm
 */
public interface ITraceScopeV1 extends AutoCloseable {

    /**
     * Gets the trace ID associated with this scope
     *
     * @return the trace ID, or null if not available
     */
    String currentTraceId();

    /**
     * Gets the tracing mode for this scope
     *
     * @return the tracing mode
     */
    TracingMode tracingMode();

    /**
     * Gets the underlying span for direct manipulation.
     *
     * @return the underlying span, or a no-op span if not available.
     * NOTE: every call of this method returns a different instance of the span, even if the span is the same.
     */
    ISpan currentSpan();

    /**
     * Finishes the underlying span and detaches the tracing context from current thread.
     * This must be called in the same thread where the scope was created, or an exception {@link org.bithon.agent.sdk.expt.SdkException} is thrown
     */
    @Override
    void close();
}