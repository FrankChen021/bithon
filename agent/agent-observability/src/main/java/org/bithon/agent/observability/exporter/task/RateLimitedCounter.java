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
public class RateLimitedCounter {

    private final long intervalMs;
    private final Object lock = new Object();
    private volatile long lastReportTime = 0;
    private volatile int accumulatedCount = 0;

    /**
     * Creates a rate-limited counter with the specified interval.
     *
     * @param interval the minimum interval between reports
     */
    public RateLimitedCounter(Duration interval) {
        this.intervalMs = interval.toMillis();
    }

    /**
     * Adds a count to the accumulator.
     * This method is thread-safe and can be called from multiple threads.
     *
     * @param count the count to add
     */
    public void add(int count) {
        synchronized (lock) {
            accumulatedCount += count;
        }
    }

    /**
     * Checks if enough time has passed since last report and returns the result if ready.
     * This method is thread-safe and can be called from multiple threads.
     *
     * @return CounterResult containing the accumulated count and interval if ready to report, null otherwise
     */
    public CounterResult checkAndReport() {
        synchronized (lock) {
            long currentTime = System.currentTimeMillis();

            if (currentTime - lastReportTime >= intervalMs) {
                int totalCount = accumulatedCount;
                long actualInterval = currentTime - lastReportTime;

                // Reset for next interval
                lastReportTime = currentTime;
                accumulatedCount = 0;

                return new CounterResult(totalCount, actualInterval);
            }

            return null; // Not ready to report yet
        }
    }

    /**
     * Adds a count and returns the accumulated total if enough time has passed since last report.
     * This is a convenience method that combines add() and checkAndReport().
     * This method is thread-safe and can be called from multiple threads.
     *
     * @param count the count to add
     * @return CounterResult containing the accumulated count and interval if ready to report, null otherwise
     */
    public CounterResult limit(int count) {
        synchronized (lock) {
            accumulatedCount += count;
            long currentTime = System.currentTimeMillis();

            if (currentTime - lastReportTime >= intervalMs) {
                int totalCount = accumulatedCount;
                long actualInterval = currentTime - lastReportTime;

                // Reset for next interval
                lastReportTime = currentTime;
                accumulatedCount = 0;

                return new CounterResult(totalCount, actualInterval);
            }

            return null; // Not ready to report yet
        }
    }

    /**
     * Result of a counter check, containing the accumulated count and actual interval.
     */
    public static class CounterResult {
        private final int count;
        private final long intervalMs;

        CounterResult(int count, long intervalMs) {
            this.count = count;
            this.intervalMs = intervalMs;
        }

        public int getCount() {
            return count;
        }

        public long getIntervalMs() {
            return intervalMs;
        }
    }
}
