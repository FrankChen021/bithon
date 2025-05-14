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
}
