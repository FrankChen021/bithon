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

package org.bithon.server.datasource.query.plan.physical;


import lombok.extern.slf4j.Slf4j;
import org.bithon.server.datasource.TimestampSpec;
import org.bithon.server.datasource.query.Interval;
import org.bithon.server.datasource.query.result.Column;
import org.bithon.server.datasource.query.result.ColumnarTable;
import org.bithon.server.datasource.query.result.PipelineQueryResult;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author frank.chen021@outlook.com
 * @date 5/5/25 6:20 pm
 */
@Slf4j
public class SlidingWindowAggregateStep implements IPhysicalPlan {
    private final String tsField;
    private final List<String> keyFields;
    private final List<String> valueFields;
    private final Duration window;
    private final List<String> resultFields;
    private final Interval interval;
    private final IPhysicalPlan source;

    public SlidingWindowAggregateStep(String tsField,
                                      List<String> keyFields,
                                      List<String> valueFields,
                                      List<String> resultFields,
                                      Duration window,
                                      Interval interval,
                                      IPhysicalPlan source) {
        this.tsField = tsField;
        this.keyFields = keyFields;
        this.valueFields = valueFields;
        this.resultFields = resultFields;
        this.window = window;
        this.interval = interval;
        this.source = source;
    }

    @Override
    public String toString() {
        return "SlidingWindowAggregationStep{" +
               "\n\ttsField='" + tsField + '\'' +
               "\n\tkeyFields=" + keyFields +
               "\n\tvalueFields=" + valueFields +
               "\n\twindow=" + window +
               "\n\tresultFields=" + resultFields +
               "\n\tinterval=" + interval +
               '}';
    }

    @Override
    public boolean isScalar() {
        return false;
    }

    @Override
    public CompletableFuture<PipelineQueryResult> execute() throws Exception {
        return source.execute()
                     .thenApply((result) -> {
                         ColumnarTable resultTable = result.getTable();

                         log.info("Applying sliding window aggregation: \n {}", this.toString());

                         // window aggregation
                         resultTable = doAggregate(resultTable,
                                                   this.tsField,
                                                   this.keyFields,
                                                   this.window,
                                                   this.valueFields);

                         //
                         // The query interval is extended to include the first record of the sliding window
                         // which is not included in the result set. So we need to filter out some records record
                         long startingPoint = this.interval.getStartTime()
                                                           .floor(interval.getStep())
                                                           .getSeconds();

                         Column tsColumn = resultTable.getColumn(TimestampSpec.COLUMN_ALIAS);

                         int selectionIndex = 0;
                         int[] selections = new int[resultTable.rowCount()];
                         for (int i = 0, rowCount = resultTable.rowCount(); i < rowCount; i++) {
                             long ts = tsColumn.getLong(i);
                             if (ts >= startingPoint) {
                                 selections[selectionIndex++] = i;
                             }
                         }

                         resultTable = resultTable.view(selections, selectionIndex);
                         return PipelineQueryResult.builder()
                                                   .table(resultTable)
                                                   .rows(resultTable.rowCount())
                                                   .keyColumns(result.getKeyColumns())
                                                   .valColumns(result.getValColumns())
                                                   .build();
                     });
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
     * @param tsField The name of field where timestamp is stored (e.g., "_timestamp")
     * @param keys    List of key field names used for grouping (e.g., ["clusterName"])
     * @param window  Size of the time window in seconds
     */
    public static ColumnarTable doAggregate(ColumnarTable table,
                                            String tsField,
                                            List<String> keys,
                                            Duration window,
                                            List<String> inputFields) {
        Column tsColumn = table.getColumn(tsField);

        List<Column> keyColumns = table.getColumns(keys);

        int rowCount = table.rowCount();
        List<Column> inputColumns = new ArrayList<>(inputFields.size());
        List<Column> outputColumns = new ArrayList<>(inputFields.size());
        for (String valueField : inputFields) {
            Column valColumn = table.getColumn(valueField);
            inputColumns.add(valColumn);
            outputColumns.add(Column.create(valueField, valColumn.getDataType(), rowCount));
        }

        CompositeKey prevKey = rowCount > 0 ? CompositeKey.from(keyColumns, 0) : null;

        // The starting window index of a group
        int windowStart = 0;
        double[] sums = new double[inputFields.size()];

        for (int i = 0; i < rowCount; i++) {
            long ts = tsColumn.getLong(i);
            CompositeKey currKey = CompositeKey.from(keyColumns, i);

            if (!currKey.equals(prevKey)) {
                // Reset window if group changes
                windowStart = i;
                Arrays.fill(sums, 0.0);
                prevKey = currKey;
            }

            // Slide window start to maintain [ts - window, ts]
            while (windowStart < i && tsColumn.getLong(windowStart) <= ts - window.getSeconds()) {
                for (int j = 0; j < inputColumns.size(); j++) {
                    sums[j] -= inputColumns.get(j).getDouble(windowStart);
                }
                windowStart++;
            }

            for (int j = 0; j < inputColumns.size(); j++) {
                sums[j] += inputColumns.get(j).getDouble(i);
                outputColumns.get(j).addObject(sums[j]);
            }
        }

        ColumnarTable resultTable = new ColumnarTable();
        resultTable.addColumns(keyColumns);
        resultTable.addColumn(tsColumn);
        resultTable.addColumns(outputColumns);
        return resultTable;
    }
}
