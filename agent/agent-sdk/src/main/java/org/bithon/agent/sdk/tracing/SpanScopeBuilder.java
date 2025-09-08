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

import java.util.logging.Logger;

/**
 * Builder for creating scoped spans with fluent API.
 * Use {@link TraceContext#newScopedSpan(String)} to create an instance.
 * For example,
 * <pre>{@code
 * try (ISpanScope span = TraceContext.newScopedSpan("operation")
 *                                    .kind(SpanKind.CLIENT)              // optional, default is SpanKind.INTERNAL
 *                                    .create()) {
 *          // Update tags if needed
 *          span.tag("key", "value");
 *
 *          // Business logic here
 *   }
 *   }</pre>
 *
 * @author frank.chen021@outlook.com
 * @date 2025/01/20
 */
public class SpanScopeBuilder {
    private static final Logger LOGGER = Logger.getLogger(SpanScopeBuilder.class.getName());
    private static final long LOG_INTERVAL_MS = 5000; // 5 seconds
    private static long lastLogTime = 0;

    private final String operationName;
    private SpanKind kind = SpanKind.INTERNAL; // default

    private static synchronized boolean shouldLog() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastLogTime > LOG_INTERVAL_MS) {
            lastLogTime = currentTime;
            return true;
        }
        return false;
    }

    public SpanScopeBuilder(String operationName) {
        this.operationName = operationName;
    }

    /**
     * Sets the span kind for this scoped span.
     *
     * @param kind the span kind to use
     * @return this builder for method chaining
     */
    public SpanScopeBuilder kind(SpanKind kind) {
        this.kind = kind;
        return this;
    }

    /**
     * Creates a scoped span within the current tracing context.
     * <p>
     * The created span will be a child of the current active span.
     * If the {@link #kind(SpanKind)} is not called, the default kind is {@link SpanKind#INTERNAL}.
     * 
     * @return an ISpan ready for use in try-with-resources.
     * Callers MUST ensure to close the span to avoid resource leaks.
     *
     * @throws org.bithon.agent.sdk.expt.SdkException if there's no active tracing context.
     */
    public ISpanScope create() {
        if (shouldLog()) {
            LOGGER.warning("The agent is not loaded.");
        }
        return ISpanScope.NOOP_INSTANCE;
    }

    // Public getters for agent interceptors
    public String operationName() {
        return operationName;
    }

    public SpanKind kind() {
        return kind;
    }
}
