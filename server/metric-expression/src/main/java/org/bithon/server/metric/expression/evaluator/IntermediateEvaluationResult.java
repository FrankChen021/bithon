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

package org.bithon.server.metric.expression.evaluator;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bithon.server.metric.expression.format.Column;
import org.bithon.server.metric.expression.format.ColumnarTable;
import org.bithon.server.web.service.datasource.api.QueryResponse;
import org.bithon.server.web.service.datasource.api.TimeSeriesMetric;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// @author frank.chen021@outlook.com
/// @date 4/4/25 6:23 pm
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntermediateEvaluationResult {
    private int rows;

    private List<String> keyColumns;
    private List<String> valColumns;
    private ColumnarTable table;

    private long startTimestamp;
    private long endTimestamp;
    private long interval;

    public QueryResponse<?> toTimeSeriesResultSet() {
        IntermediateEvaluationResult intermediateResult = this;

        // Because the end timestamp is inclusive, we need to add 1
        int count = 1 + (int) ((intermediateResult.getEndTimestamp() - intermediateResult.getStartTimestamp()) / intermediateResult.getInterval());

        List<String> keys = new ArrayList<>(intermediateResult.getKeyColumns());
        if (keys.get(0).equals("_timestamp")) {
            keys.remove(0);
        } else {
            throw new IllegalStateException();
        }
        Column timestampCol = intermediateResult.getTable().getColumn("_timestamp");
        List<Column> dimCols = intermediateResult.getTable().getColumns(keys);
        List<Column> valCols = intermediateResult.getTable().getColumns(intermediateResult.getValColumns());

        Map<List<String>, TimeSeriesMetric> map = new LinkedHashMap<>(7);
        for (int i = 0; i < intermediateResult.getRows(); i++) {

            // the timestamp is seconds
            long timestamp = timestampCol.getLong(i) * 1000;
            long index = (timestamp - intermediateResult.getStartTimestamp()) / intermediateResult.getInterval();

            for (int j = 0, valColsSize = valCols.size(); j < valColsSize; j++) {
                Column valCol = valCols.get(j);
                List<String> series = new ArrayList<>(dimCols.size() + 1);
                for (Column dimCol : dimCols) {
                    series.add(dimCol.getObject(i).toString());
                }
                series.add(intermediateResult.getValColumns().get(j));

                map.computeIfAbsent(series, k -> new TimeSeriesMetric(series, count))
                   .set((int) index, valCol.getObject(i));
            }
        }

        return QueryResponse.builder()
                            .interval(intermediateResult.getInterval())
                            .startTimestamp(intermediateResult.getStartTimestamp())
                            .endTimestamp(intermediateResult.getEndTimestamp())
                            .data(map.values())
                            .build();
    }
}

