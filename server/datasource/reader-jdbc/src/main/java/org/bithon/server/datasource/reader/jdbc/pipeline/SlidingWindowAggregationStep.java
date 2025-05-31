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

package org.bithon.server.datasource.reader.jdbc.pipeline;


import lombok.extern.slf4j.Slf4j;
import org.bithon.server.datasource.TimestampSpec;
import org.bithon.server.datasource.query.Interval;
import org.bithon.server.datasource.query.pipeline.Column;
import org.bithon.server.datasource.query.pipeline.ColumnarTable;
import org.bithon.server.datasource.query.pipeline.IQueryStep;
import org.bithon.server.datasource.query.pipeline.PipelineQueryResult;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author frank.chen021@outlook.com
 * @date 5/5/25 6:20 pm
 */
@Slf4j
public class SlidingWindowAggregationStep implements IQueryStep {
    private final String tsField;
    private final List<String> keyFields;
    private final List<String> valueFields;
    private final Duration window;
    private final List<String> resultFields;
    private final Interval interval;
    private final IQueryStep source;

    public SlidingWindowAggregationStep(String tsField,
                                        List<String> keyFields,
                                        List<String> valueFields,
                                        List<String> resultFields,
                                        Duration window,
                                        Interval interval,
                                        IQueryStep source) {
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
                         resultTable = SlidingWindowAggregator.aggregate(resultTable,
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
}
