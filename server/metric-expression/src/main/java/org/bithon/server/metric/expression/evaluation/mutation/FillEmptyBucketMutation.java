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

package org.bithon.server.metric.expression.evaluation.mutation;


import org.bithon.server.metric.expression.evaluation.EvaluationResult;
import org.bithon.server.metric.expression.evaluation.IEvaluator;
import org.bithon.server.metric.expression.format.Column;
import org.bithon.server.web.service.datasource.api.QueryResponse;
import org.bithon.server.web.service.datasource.api.TimeSeriesMetric;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 5/4/25 9:17 pm
 */
public class FillEmptyBucketMutation {
    private final IEvaluator evaluator;

    public FillEmptyBucketMutation(IEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    /// TODO:
    /// Change the format as:
    ///     labels -> Map<String, String>
    ///
    ///  Input:
    ///
    /// |appName, instanceName | metric1 | metric2 |
    /// |-----------------------|--------|---------|
    /// | {timestamp1, key1, key2} | value1 | value2 |
    /// | {timestamp2, key3, key4} | value3 | value4 |
    ///
    /// Output:
    ///   intermediate output: a Map, key is the series labels + metric name, value is a list of values
    ///   final output: turn the map into a collection
    ///
    /// Process:
    /// for each line, calculate the bucket index by the timestamp,
    ///  2. create a key by the values of series label(excluding the timestamp) and metric name,
    ///  3. find the key from map
    ///  4. if not found from the map, then create an empty double[] and put it into the map
    ///  5. set the value based the index calculated in step 1 to the double[]
    public QueryResponse<?> evaluate() throws Exception {
        EvaluationResult evaluationResult = evaluator.evaluate().get();

        int count = (int) ((evaluationResult.getEndTimestamp() - evaluationResult.getStartTimestamp()) / evaluationResult.getInterval());

        List<String> keys = evaluationResult.getKeyColumns();
        if (keys.get(0).equals("_timestamp")) {
            keys.remove(0);
        } else {
            throw new IllegalStateException();
        }
        Column timestampCol = evaluationResult.getTable().getColumn("_timestamp");
        List<Column> dimCols = evaluationResult.getTable().getColumns(keys);
        List<Column> valCols = evaluationResult.getTable().getColumns(evaluationResult.getValColumns());

        Map<List<String>, TimeSeriesMetric> map = new LinkedHashMap<>(7);
        for (int i = 0; i < evaluationResult.getRows(); i++) {

            // the timestamp is seconds
            long timestamp = timestampCol.getLong(i) * 1000;
            long index = (timestamp - evaluationResult.getStartTimestamp()) / evaluationResult.getInterval();

            for (int j = 0, valColsSize = valCols.size(); j < valColsSize; j++) {
                Column valCol = valCols.get(j);
                List<String> tags = new ArrayList<>(dimCols.size() + 1);
                for (Column dimCol : dimCols) {
                    Object v = dimCol.getObject(i);
                }
                tags.add(evaluationResult.getValColumns().get(j));

                map.computeIfAbsent(tags, k -> new TimeSeriesMetric(tags, count))
                   .set((int) index, valCol.getObject(i));
            }
        }

        return QueryResponse.builder()
                            .interval(evaluationResult.getInterval())
                            .startTimestamp(evaluationResult.getStartTimestamp())
                            .endTimestamp(evaluationResult.getEndTimestamp())
                            .data(map.values())
                            .build();
    }
}
