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

package org.bithon.server.datasource.reader.jdbc;

import org.bithon.server.datasource.query.pipeline.Column;
import org.bithon.server.datasource.query.pipeline.ColumnarTable;
import org.bithon.server.datasource.query.pipeline.CompositeKey;

import java.time.Duration;
import java.util.List;


/**
 * @author frank.chen021@outlook.com
 * @date 4/5/25 1:00 pm
 */
public class SlidingWindowAggregator {

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
     * @param tsField The name of field where timestamp is stored (e.g., "_timestamp")
     * @param keys   List of key field names used for grouping (e.g., ["clusterName"])
     * @param window Size of the time window in seconds
     */
    public static ColumnarTable aggregate(
        ColumnarTable table,
        String tsField,
        List<String> keys,
        Duration window,
        String valueField
    ) {
        Column tsColumn = table.getColumn(tsField);
        Column valColumn = table.getColumn(valueField);
        List<Column> keyColumns = table.getColumns(keys);

        int rowCount = table.rowCount();
        Column aggregatedColumn = Column.create(valueField, valColumn.getDataType(), rowCount);

        CompositeKey prevKey = rowCount > 0 ? CompositeKey.from(keyColumns, 0) : null;

        // The starting window index of a group
        int windowStart = 0;
        double sum = 0.0;
        for (int i = 0; i < rowCount; i++) {
            long ts = tsColumn.getLong(i);
            CompositeKey currKey = CompositeKey.from(keyColumns, i);

            if (!currKey.equals(prevKey)) {
                // Reset window if group changes
                windowStart = i;
                sum = 0.0;
                prevKey = currKey;
            }

            // Slide window start to maintain [ts - window, ts]
            while (windowStart < i && tsColumn.getLong(windowStart) <= ts - window.getSeconds()) {
                sum -= valColumn.getDouble(windowStart);
                windowStart++;
            }

            sum += valColumn.getDouble(i);
            aggregatedColumn.addObject(sum);
        }

        table.addColumn(aggregatedColumn);
        return table;
    }
}
