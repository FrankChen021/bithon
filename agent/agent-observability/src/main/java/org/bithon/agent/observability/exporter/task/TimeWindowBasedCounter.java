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

package org.bithon.agent.observability.exporter.task;

import java.time.Duration;

/**
 * A thread-safe counter that accumulates values and provides them at configurable intervals.
 * Useful for rate-limited logging to avoid log flooding while still capturing important metrics.
 *
 * @author frankchen
 */
public class TimeWindowBasedCounter {

    /**
     * window in milliseconds
     */
    private final long timeWindow;
    private volatile long lastResetTimestamp = 0;
    private volatile int counter = 0;

    /**
     * Creates a rate-limited counter with the specified interval.
     *
     * @param timeWindow the minimum interval between reports
     */
    public TimeWindowBasedCounter(Duration timeWindow) {
        this.timeWindow = timeWindow.toMillis();
    }

    /**
     * Adds a count and returns the accumulated total if enough time has passed since last report.
     * This is a convenience method that combines add() and checkAndReport().
     * This method is thread-safe and can be called from multiple threads.
     *
     * @param count the count to add
     * @return 0 if this operation is still within the current time window
     */
    public synchronized int add(int count) {
        counter += count;

        long now = System.currentTimeMillis();
        if (now - lastResetTimestamp >= timeWindow) {
            int ret = counter;

            counter = 0;
            lastResetTimestamp = now;

            return ret;
        }

        return 0;
    }
}
