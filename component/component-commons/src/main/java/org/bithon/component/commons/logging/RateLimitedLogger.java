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

package org.bithon.component.commons.logging;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * A rate-limited logger wrapper that prevents log flooding by limiting log frequency
 * for each exception type or log key.
 *
 * @author frank.chen021@outlook.com
 * @date 2025/8/13 18:30
 */
public class RateLimitedLogger {

    static class LoggingState {
        private long lastLogTimestamp = 0;
        private long suppressedCount = 0;
    }

    private final ILogAdaptor logger;
    private final long logIntervalMs;
    private final Map<String, LoggingState> lastLogTimes = new HashMap<>();

    public RateLimitedLogger(ILogAdaptor logger, Duration logInterval) {
        this.logger = logger;
        this.logIntervalMs = logInterval.toMillis();
    }

    public void warn(Throwable exception, String message, Object... args) {
        warn(exception.getClass().getSimpleName(), message, args);
    }

    public void warn(String key, String messageFormat, Object... args) {
        long suppressedCount = shouldLogAndGetSuppressedCount(key);
        if (suppressedCount > 0) {
            if (suppressedCount > 1) {
                messageFormat = "(" + (suppressedCount + 1) + " logs suppressed since last logging) " + messageFormat;
            }
            logger.warn(messageFormat, args);
        }
    }

    /**
     * Return the suppressed count of the last interval. If it's non-zero, we should log the message.
     */
    private synchronized long shouldLogAndGetSuppressedCount(String key) {
        LoggingState state = lastLogTimes.computeIfAbsent(key, k -> new LoggingState());

        long now = System.currentTimeMillis();
        if (now - state.lastLogTimestamp > logIntervalMs) {
            long suppressedCount = state.suppressedCount;

            state.lastLogTimestamp = now;
            state.suppressedCount = 0;

            // Return suppressedCount, but ensure first log always gets logged (return at least 1)
            return Math.max(1, suppressedCount);
        } else {
            state.suppressedCount++;
            return 0;
        }
    }
}
