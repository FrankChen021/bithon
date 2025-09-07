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

import org.bithon.agent.sdk.tracing.impl.NoopTraceScope;

import java.util.logging.Logger;

/**
 * Builder for creating trace scopes with fluent API.
 * Use {@link TraceContext#newTrace(String)} to create an instance.
 * For example,
 * <pre>{@code
 * try (ITraceScope scope = TraceContext.newTrace("operation")
 *                                      .kind(SpanKind.SERVER)              // MUST give
 *                                      .parent(traceId, parentSpanId)      // optional
 *                                      .tracingMode(TracingMode.LOGGING)   // optional, default is TracingMode.TRACING
 *                                      .attach()) {
 *          // Update tags if needed
 *          ISpan span = scope.currentSpan();
 *          span.tag("key", "value");
 *
 *          // Business logic here
 *   }
 *   }</pre>
 *
 * @author frank.chen021@outlook.com
 * @date 8/5/25 6:00 pm
 */
public class TraceScopeBuilder {
    private static final Logger LOGGER = Logger.getLogger(TraceScopeBuilder.class.getName());
    private static final long LOG_INTERVAL_MS = 5000; // 5 seconds
    private static long lastLogTime = 0;

    private final String operationName;
    private String traceId;
    private String parentSpanId;
    private TracingMode tracingMode = TracingMode.TRACING; // default
    private SpanKind kind;

    private static synchronized boolean shouldLog() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastLogTime > LOG_INTERVAL_MS) {
            lastLogTime = currentTime;
            return true;
        }
        return false;
    }

    TraceScopeBuilder(String operationName) {
        this.operationName = operationName;
    }

    /**
     * Sets the parent trace context for this trace scope.
     * Use this to continue an existing trace in another thread or context.
     * <p>
     * If not set, a new trace(with auto generated trace id and empty parent span id) will be created.
     *
     * @param traceId      the trace ID of the parent trace
     * @param parentSpanId the span ID of the parent span
     * @return this builder for method chaining
     */
    public TraceScopeBuilder parent(String traceId, String parentSpanId) {
        this.traceId = traceId;
        this.parentSpanId = parentSpanId;
        return this;
    }

    public TraceScopeBuilder kind(SpanKind kind) {
        this.kind = kind;
        return this;
    }

    /**
     * Sets the tracing mode for this trace scope.
     *
     * @param tracingMode the tracing mode to use
     * @return this builder for method chaining
     */
    public TraceScopeBuilder tracingMode(TracingMode tracingMode) {
        this.tracingMode = tracingMode;
        return this;
    }

    /**
     * Creates and attaches the trace scope to the current thread.
     * If current thread already has a trace context attached, an exception({@link org.bithon.agent.sdk.expt.SdkException}) will be thrown.
     *
     * @return an attached ITraceScope ready for use in try-with-resources.
     * Callers MUST ensure to close the scope to avoid resource leaks.
     */
    public ITraceScope attach() {
        if (shouldLog()) {
            LOGGER.warning("The agent is not loaded.");
        }
        return NoopTraceScope.INSTANCE;
    }

    // Public getters for agent interceptors
    public String operationName() {
        return operationName;
    }

    public String traceId() {
        return traceId;
    }

    public String parentSpanId() {
        return parentSpanId;
    }

    public TracingMode tracingMode() {
        return tracingMode;
    }

    public SpanKind kind() {
        return kind;
    }
}
