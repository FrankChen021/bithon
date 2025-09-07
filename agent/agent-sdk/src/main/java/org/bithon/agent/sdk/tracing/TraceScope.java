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

package org.bithon.agent.sdk.tracing;

/**
 * Represents a tracing scope that can be safely passed between threads
 * and provides context management for cross-thread tracing operations.
 * 
 * @author frank.chen021@outlook.com
 * @date 2025/09/09 15:32
 */
public interface TraceScope extends AutoCloseable {
    
    /**
     * Gets the trace ID associated with this scope
     * @return the trace ID, or null if not available
     */
    String getTraceId();
    
    /**
     * Gets the span ID associated with this scope
     * @return the span ID, or null if not available
     */
    String getSpanId();
    
    /**
     * Gets the tracing mode for this scope
     * @return the tracing mode
     */
    TracingMode getTracingMode();
    
    /**
     * Attaches this scope's context to the current thread
     * @return this scope for method chaining
     */
    TraceScope attach();
    
    /**
     * Detaches this scope's context from the current thread
     * @return this scope for method chaining
     */
    TraceScope detach();
    
    /**
     * Creates a new child span within this scope
     * @param operationName the name of the operation
     * @return a new span, or a no-op span if tracing is not available
     */
    ISpan startSpan(String operationName);
    
    /**
     * Gets the underlying span for direct manipulation
     * @return the underlying span, or a no-op span if not available
     */
    ISpan getSpan();
    
    /**
     * Finishes the underlying span and detaches from current thread
     */
    @Override
    void close();
}
