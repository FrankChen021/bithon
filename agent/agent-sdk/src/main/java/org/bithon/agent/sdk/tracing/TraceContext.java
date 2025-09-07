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


import org.bithon.agent.sdk.tracing.impl.NoopSpan;
import org.bithon.agent.sdk.tracing.impl.NoopTraceScope;

import java.util.logging.Logger;

/**
 * @author frank.chen021@outlook.com
 * @date 8/5/25 5:47 pm
 */
public class TraceContext {
    private static final Logger LOGGER = Logger.getLogger(TraceContext.class.getName());
    private static final long LOG_INTERVAL_MS = 5000; // 5 seconds
    private static long lastLogTime = 0;

    private static synchronized boolean shouldLog() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastLogTime > LOG_INTERVAL_MS) {
            lastLogTime = currentTime;
            return true;
        }
        return false;
    }

    public static String currentTraceId() {
        if (shouldLog()) {
            LOGGER.warning("The agent is not loaded.");
        }
        return null;
    }

    public static String currentSpanId() {
        if (shouldLog()) {
            LOGGER.warning("The agent is not loaded.");
        }
        return null;
    }

    public static ISpan newScopedSpan() {
        if (shouldLog()) {
            LOGGER.warning("The agent is not loaded.");
        }
        return NoopSpan.INSTANCE;
    }

    /**
     * Creates a new trace scope builder for the given operation.
     * Use the builder to configure the trace and then call attach() to start tracing.
     * 
     * <p>Examples:</p>
     * <pre>{@code
     * // Simple root trace
     * try (ITraceScope scope = TraceContext.newTrace("operation").attach()) {
     *     // do your work here
     * }
     * 
     * // Root trace with custom tracing mode
     * try (ITraceScope scope = TraceContext.newTrace("operation")
     *         .withTracingMode(TracingMode.LOGGING)
     *         .attach()) {
     *     // do your work here
     * }
     * 
     * // Cross-thread continuation
     * String traceId = TraceContext.currentTraceId();
     * String parentSpanId = TraceContext.currentSpanId();
     * ITraceScope asyncTrace = TraceContext.newTrace("background-work")
     *     .withParent(traceId, parentSpanId)
     *     .attach();
     * // pass asyncTrace to another thread...
     * }</pre>
     * 
     * @param operationName the name of the operation
     * @return a TraceScopeBuilder for configuring and creating the trace
     */
    public static TraceScopeBuilder newTrace(String operationName) {
        return new TraceScopeBuilder(operationName);
    }

    /**
     * Convenience method to create a trace continuation from the current trace context.
     * This is equivalent to calling:
     * <pre>{@code
     * TraceContext.newTrace(operationName)
     *     .withParent(TraceContext.currentTraceId(), TraceContext.currentSpanId())
     * }</pre>
     * 
     * @param operationName the name of the operation
     * @return a TraceScopeBuilder configured with the current trace context as parent
     * @throws IllegalStateException if there is no current trace context
     */
    public static TraceScopeBuilder newTraceFromCurrent(String operationName) {
        String traceId = currentTraceId();
        String parentSpanId = currentSpanId();
        if (traceId == null) {
            throw new IllegalStateException("No current trace context");
        }
        return new TraceScopeBuilder(operationName).withParent(traceId, parentSpanId);
    }
}
