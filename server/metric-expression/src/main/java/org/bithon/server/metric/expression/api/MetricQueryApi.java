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

package org.bithon.server.metric.expression.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bithon.component.commons.Experimental;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.datasource.query.plan.physical.Column;
import org.bithon.server.datasource.query.plan.physical.IPhysicalPlan;
import org.bithon.server.datasource.query.plan.physical.PipelineQueryResult;
import org.bithon.server.metric.expression.pipeline.QueryPipelineBuilder;
import org.bithon.server.web.service.WebServiceModuleEnabler;
import org.bithon.server.web.service.datasource.api.IDataSourceApi;
import org.bithon.server.web.service.datasource.api.IntervalRequest;
import org.bithon.server.web.service.datasource.api.QueryResponse;
import org.bithon.server.web.service.datasource.api.TimeSeriesMetric;
import org.springframework.context.annotation.Conditional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 26/11/24 2:36 pm
 */
@CrossOrigin
@RestController
@Conditional(WebServiceModuleEnabler.class)
public class MetricQueryApi {
    private final IDataSourceApi dataSourceApi;

    public MetricQueryApi(IDataSourceApi dataSourceApi) {
        this.dataSourceApi = dataSourceApi;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricQueryRequest {
        /**
         * Metric QL expression such as "sum(cpu_usage) by host"
         */
        @NotBlank
        private String expression;

        /**
         * Extra condition
         */
        @Nullable
        private String condition;

        @NotNull
        @Valid
        private IntervalRequest interval;
    }

    @Experimental
    @PostMapping("/api/metric/timeseries")
    public QueryResponse<?> timeSeries(@Validated @RequestBody MetricQueryRequest request) throws Exception {
        IPhysicalPlan pipeline = QueryPipelineBuilder.builder()
                                                     .dataSourceApi(dataSourceApi)
                                                     .intervalRequest(request.getInterval())
                                                     .condition(request.getCondition())
                                                     .build(request.getExpression());

        Duration step = request.getInterval().calculateStep();
        TimeSpan start = request.getInterval().getStartISO8601();
        TimeSpan end = request.getInterval().getEndISO8601();

        return toTimeSeriesResultSet(start.floor(step).getMilliseconds(),
                                     end.floor(step).getMilliseconds(),
                                     step.toMillis(),
                                     pipeline.execute().get());
    }

    private QueryResponse<?> toTimeSeriesResultSet(long startTimestamp,
                                                   long endTimestamp,
                                                   long interval,
                                                   PipelineQueryResult queryResult) {
        // Because the end timestamp is inclusive, we need to add 1
        int count = 1 + (int) ((endTimestamp - startTimestamp) / interval);

        List<String> keys = new ArrayList<>(queryResult.getKeyColumns());
        if (keys.get(0).equals("_timestamp")) {
            keys.remove(0);
        } else {
            throw new IllegalStateException();
        }
        Column timestampCol = queryResult.getTable().getColumn("_timestamp");
        List<Column> dimCols = queryResult.getTable().getColumns(keys);
        List<Column> valCols = queryResult.getTable().getColumns(queryResult.getValColumns());

        Map<List<String>, TimeSeriesMetric> map = new LinkedHashMap<>(7);
        for (int i = 0; i < queryResult.getRows(); i++) {

            // the timestamp is seconds
            long timestamp = timestampCol.getLong(i) * 1000;
            long index = (timestamp - startTimestamp) / endTimestamp;

            for (int j = 0, valColsSize = valCols.size(); j < valColsSize; j++) {
                Column valCol = valCols.get(j);
                List<String> series = new ArrayList<>(dimCols.size() + 1);
                for (Column dimCol : dimCols) {
                    series.add(dimCol.getObject(i).toString());
                }
                series.add(queryResult.getValColumns().get(j));

                map.computeIfAbsent(series, k -> new TimeSeriesMetric(series, count))
                   .set((int) index, valCol.getObject(i));
            }
        }

        List<QueryResponse.QueryResponseColumn> responseColumns = new ArrayList<>(dimCols.size() + queryResult.getValColumns().size());
        for (Column dim : dimCols) {
            responseColumns.add(new QueryResponse.QueryResponseColumn(dim.getName(), dim.getDataType().name()));
        }
        for (Column val : valCols) {
            responseColumns.add(new QueryResponse.QueryResponseColumn(val.getName(), val.getDataType().name()));
        }

        return QueryResponse.builder()
                            .interval(interval)
                            .startTimestamp(startTimestamp)
                            .endTimestamp(endTimestamp)
                            .data(map.values())
                            .meta(responseColumns)
                            .build();
    }
}
