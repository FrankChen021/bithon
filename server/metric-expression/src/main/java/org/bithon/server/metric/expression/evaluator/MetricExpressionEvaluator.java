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


import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.server.metric.expression.format.Column;
import org.bithon.server.metric.expression.format.ColumnarTable;
import org.bithon.server.web.service.datasource.api.IDataSourceApi;
import org.bithon.server.web.service.datasource.api.QueryField;
import org.bithon.server.web.service.datasource.api.QueryRequest;
import org.bithon.server.web.service.datasource.api.QueryResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 4/4/25 3:48 pm
 */
public class MetricExpressionEvaluator implements IEvaluator {
    private final QueryRequest queryRequest;
    private final IDataSourceApi dataSourceApi;
    private final boolean isScalar;

    // Make sure the evaluation is executed ONLY ONCE when the expression is referenced multiple times
    private volatile CompletableFuture<EvaluationResult> cachedResponse;

    public MetricExpressionEvaluator(QueryRequest queryRequest, IDataSourceApi dataSourceApi) {
        this.queryRequest = queryRequest;
        this.dataSourceApi = dataSourceApi;
        this.isScalar = CollectionUtils.isEmpty(queryRequest.getGroupBy())
                        && (queryRequest.getInterval().getBucketCount() != null && queryRequest.getInterval().getBucketCount() == 1
                            // TODO: judge with STEP and INTERVAL LENGTH
                        );
    }

    @Override
    public boolean isScalar() {
        return isScalar;
    }

    @Override
    public CompletableFuture<EvaluationResult> evaluate() {
        if (cachedResponse == null) {
            synchronized (this) {
                if (cachedResponse == null) {
                    cachedResponse = CompletableFuture.supplyAsync(() -> {
                        try {
                            QueryResponse<?> response = dataSourceApi.timeseriesV5(queryRequest);
                            List<String> keys = new ArrayList<>();
                            keys.add("_timestamp");
                            keys.addAll(queryRequest.getGroupBy() != null ? queryRequest.getGroupBy() : Collections.emptySet());

                            List<String> valNames = queryRequest.getFields().stream().map(QueryField::getName).toList();

                            return toEvaluationResult(keys, valNames, response);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
            }
        }
        return cachedResponse;
    }

    private EvaluationResult toEvaluationResult(List<String> keyNames,
                                                List<String> valNames,
                                                QueryResponse response) {
        List<QueryResponse.QueryResponseColumn> keyColumns = keyNames.stream()
                                                                     .map((key) -> {
                                                                         List<QueryResponse.QueryResponseColumn> cols = response.getMeta();
                                                                         return cols.stream()
                                                                                    .filter((QueryResponse.QueryResponseColumn col) -> col.getName().equals(key))
                                                                                    .findFirst()
                                                                                    .orElseThrow(() -> new IllegalArgumentException("Key column not found: " + key));

                                                                     }).collect(Collectors.toList());
        List<QueryResponse.QueryResponseColumn> valColumns = valNames.stream()
                                                                     .map((val) -> {
                                                                         List<QueryResponse.QueryResponseColumn> cols = response.getMeta();
                                                                         return cols.stream()
                                                                                    .filter((QueryResponse.QueryResponseColumn col) -> col.getName().equals(val))
                                                                                    .findFirst()
                                                                                    .orElseThrow(() -> new IllegalArgumentException("Key column not found: " + val));

                                                                     }).collect(Collectors.toList());
        List<Map<String, Object>> rows = (List<Map<String, Object>>) response.getData();

        // Create a ColumnarTable to hold the data
        ColumnarTable table = new ColumnarTable();

        // Add key columns
        for (QueryResponse.QueryResponseColumn keyColumn : keyColumns) {
            Column column = Column.create(keyColumn.getName(), keyColumn.getDataType(), rows.size());
            for (Map<String, Object> row : rows) {
                column.addObject(row.get(keyColumn.getName()));
            }
            table.addColumn(column);
        }

        // Add value columns
        for (QueryResponse.QueryResponseColumn valColumn : valColumns) {
            Column column = Column.create(valColumn.getName(), valColumn.getDataType(), rows.size());
            for (Map<String, Object> value : rows) {
                column.addObject(value.get(valColumn.getName()));
            }
            table.addColumn(column);
        }

        // Create and return the EvaluationResult
        return EvaluationResult.builder()
                               .rows(rows.size())
                               .keyColumns(keyColumns.stream().map(QueryResponse.QueryResponseColumn::getName).toList())
                               .valColumns(valColumns.stream().map((QueryResponse.QueryResponseColumn::getName)).toList())
                               .table(table)
                               .startTimestamp(response.getStartTimestamp())
                               .endTimestamp(response.getEndTimestamp())
                               .interval(response.getInterval())
                               .build();
    }
}
