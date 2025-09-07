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
 * Builder for creating trace scopes with fluent API
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
     * 
     * @param traceId the trace ID of the parent trace
     * @param parentSpanId the span ID of the parent span
     * @return this builder for method chaining
     */
    public TraceScopeBuilder withParent(String traceId, String parentSpanId) {
        this.traceId = traceId;
        this.parentSpanId = parentSpanId;
        return this;
    }

    /**
     * Sets the tracing mode for this trace scope.
     * 
     * @param tracingMode the tracing mode to use
     * @return this builder for method chaining
     */
    public TraceScopeBuilder withTracingMode(TracingMode tracingMode) {
        this.tracingMode = tracingMode;
        return this;
    }

    /**
     * Creates and attaches the trace scope to the current thread.
     * This is equivalent to calling build().attach().
     * 
     * @return an attached ITraceScope ready for use in try-with-resources
     */
    public ITraceScope attach() {
        if (shouldLog()) {
            LOGGER.warning("The agent is not loaded.");
        }
        return NoopTraceScope.INSTANCE;
    }

    /**
     * Creates and attaches the trace scope to the current thread with control over span starting.
     * This is equivalent to calling build().attach(startSpan).
     * 
     * @param startSpan whether to start the root span immediately
     * @return an attached ITraceScope ready for use in try-with-resources
     */
    public ITraceScope attach(boolean startSpan) {
        if (shouldLog()) {
            LOGGER.warning("The agent is not loaded.");
        }
        return NoopTraceScope.INSTANCE;
    }

    // Public getters for agent interceptors
    public String getOperationName() {
        return operationName;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getParentSpanId() {
        return parentSpanId;
    }

    public TracingMode getTracingMode() {
        return tracingMode;
    }

    public boolean isRootTrace() {
        return traceId == null;
    }
}
