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

package org.bithon.server.metric.expression.pipeline.step;

import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.server.datasource.query.pipeline.IQueryStep;
import org.bithon.server.datasource.query.pipeline.PipelineQueryResult;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;


/**
 * @author frank.chen021@outlook.com
 * @date 4/5/25 1:00 pm
 */
public class SlidingWindowAggregationStep implements IQueryStep {

    private final Duration step;
    private final String valueField;
    private final List<String> keys;
    private final IQueryStep delegate;

    public SlidingWindowAggregationStep(List<String> keys, String valueField, Duration step, IQueryStep delegate) {
        this.step = step;
        this.valueField = valueField;
        this.keys = keys;
        this.delegate = delegate;
    }

    @Override
    public boolean isScalar() {
        return delegate.isScalar();
    }

    @Override
    public CompletableFuture<PipelineQueryResult> execute() throws Exception {
        CompletableFuture<PipelineQueryResult> future = delegate.execute();
        future.thenApply((result) -> {
            // TODO: apply aggregate
            return result;
        });
        return future;
    }

    /**
     * Computes a moving window sum over a list of sorted time series records.
     *
     * <p>Each input row is expected to contain:
     * <ul>
     *   <li>A numeric "timestamp" field (in seconds)</li>
     *   <li>A numeric value field indicated by valueField parameter</li>
     *   <li>One or more grouping key fields (e.g., "clusterName")</li>
     * </ul>
     *
     * <p>The input list must be pre-sorted by the grouping keys (in order) and then by timestamp.
     * For each row, this method computes the sum of all "value" fields from rows with the same group key
     * whose timestamps fall within the range {@code [current.timestamp - windowSeconds, current.timestamp]}.
     *
     * <p>The result is a new list of maps, each containing all original key-value pairs from the input row,
     * plus an additional entry valueField, holding the computed sum.
     *
     * @param rows   List of input rows (must be sorted by group keys and timestamp)
     * @param keys   List of key field names used for grouping (e.g., ["clusterName"])
     * @param window Size of the time window in seconds
     */
    public static List<Map<String, Object>> aggregate(
        List<Map<String, Object>> rows,
        List<String> keys,
        Duration window,
        String valueField
    ) {
        List<Map<String, Object>> result = new ArrayList<>();

        // State per group key
        Map<CompositeKey, Deque<Map<String, Object>>> windowMap = new HashMap<>();
        Map<CompositeKey, Double> runningSum = new HashMap<>();

        for (Map<String, Object> row : rows) {
            long ts = ((Number) row.get("timestamp")).longValue();
            CompositeKey key = new CompositeKey(row, keys);

            // Get or init state for this group
            Deque<Map<String, Object>> deque = windowMap.computeIfAbsent(key, k -> new ArrayDeque<>());
            double sum = runningSum.getOrDefault(key, 0.0);

            // Evict old rows outside of window
            while (!deque.isEmpty()) {
                long oldestTs = ((Number) deque.peekFirst().get("timestamp")).longValue();
                if (oldestTs < ts - window.getSeconds()) {
                    Map<String, Object> evicted = deque.pollFirst();
                    sum -= ((Number) evicted.get(valueField)).doubleValue();
                } else {
                    break;
                }
            }

            // Add current row
            double currentVal = ((Number) row.get(valueField)).doubleValue();
            deque.addLast(row);
            sum += currentVal;

            // Update state
            runningSum.put(key, sum);

            // Output row with window_sum
            Map<String, Object> out = new LinkedHashMap<>(row);
            out.put(valueField, sum);
            result.add(out);
        }

        return result;
    }

    // Composite key wrapper
    private static class CompositeKey {
        private final List<Object> values;

        public CompositeKey(Map<String, Object> row, List<String> keys) {
            if (CollectionUtils.isEmpty(keys)) {
                values = List.of();
            } else {
                values = new ArrayList<>(keys.size());
                for (String key : keys) {
                    values.add(row.get(key));
                }
            }
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof CompositeKey)) {
                return false;
            }
            return values.equals(((CompositeKey) o).values);
        }

        @Override
        public int hashCode() {
            return values.hashCode();
        }
    }
}
