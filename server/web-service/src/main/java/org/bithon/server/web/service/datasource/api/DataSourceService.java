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

package org.bithon.server.web.service.datasource.api;

import org.bithon.server.datasource.TimestampSpec;
import org.bithon.server.datasource.column.ExpressionColumn;
import org.bithon.server.datasource.column.IColumn;
import org.bithon.server.datasource.column.aggregatable.IAggregatableColumn;
import org.bithon.server.datasource.query.IDataSourceReader;
import org.bithon.server.datasource.query.Query;
import org.bithon.server.datasource.query.ast.ExpressionNode;
import org.bithon.server.datasource.query.ast.Selector;
import org.bithon.server.datasource.query.pipeline.ColumnarTable;
import org.bithon.server.storage.metrics.IMetricStorage;
import org.bithon.server.web.service.WebServiceModuleEnabler;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 8/1/22 9:57 PM
 */
@Service
@Conditional(WebServiceModuleEnabler.class)
public class DataSourceService {

    private final IMetricStorage metricStorage;

    public DataSourceService(IMetricStorage metricStorage) {
        this.metricStorage = metricStorage;
    }

    /**
     * @return Map<Tags, Val>
     * Tags - dimension of a series
     * Val - an array of all data points. Each element represents a data point of a timestamp.
     */
    public TimeSeriesQueryResult timeseriesQuery(Query query) throws IOException {
        // Remove any dimensions
        List<String> metrics = query.getSelectors()
                                    .stream()
                                    .filter((selectColumn) -> {
                                        if (selectColumn.getSelectExpression() instanceof ExpressionNode) {
                                            // Support the metrics defined directly at the client side.
                                            // TODO: check if the fields involved in the expression are all metrics
                                            return true;
                                        }

                                        IColumn column = query.getSchema().getColumnByName(selectColumn.getOutputName());
                                        return column instanceof IAggregatableColumn || column instanceof ExpressionColumn;
                                    })
                                    .map((Selector::getOutputName))
                                    .collect(Collectors.toList());

        try (IDataSourceReader reader = query.getSchema()
                                             .getDataStoreSpec()
                                             .createReader()) {

            List<Map<String, Object>> result = reader.timeseries(query)
                                                     .toRowFormat();

            // Convert to the result format and fills in missed data points
            return TimeSeriesQueryResult.build(query.getInterval().getStartTime(),
                                               query.getInterval().getEndTime(),
                                               query.getInterval().getStep().getSeconds(),
                                               result,
                                               TimestampSpec.COLUMN_ALIAS,
                                               query.getGroupBy(),
                                               metrics);
        }
    }

    public ColumnarTable timeseriesQuery2(Query query) throws IOException {
        try (IDataSourceReader reader = query.getSchema()
                                             .getDataStoreSpec()
                                             .createReader()) {
            return reader.timeseries(query);
        }
    }

    public List<String> getBaseline() {
        return metricStorage.getBaselineDates();
    }

    public void addToBaseline(String date, int keepDays) {
        metricStorage.saveBaseline(date, keepDays);
    }
}
