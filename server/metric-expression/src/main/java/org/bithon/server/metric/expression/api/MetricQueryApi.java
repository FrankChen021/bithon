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
import org.bithon.server.metric.expression.evaluator.EvaluatorBuilder;
import org.bithon.server.metric.expression.evaluator.IEvaluator;
import org.bithon.server.web.service.WebServiceModuleEnabler;
import org.bithon.server.web.service.datasource.api.IDataSourceApi;
import org.bithon.server.web.service.datasource.api.IntervalRequest;
import org.bithon.server.web.service.datasource.api.QueryResponse;
import org.springframework.context.annotation.Conditional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nullable;

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
    public QueryResponse timeSeries(@Validated @RequestBody MetricQueryRequest request) throws Exception {
        IEvaluator evaluator = EvaluatorBuilder.builder()
                                               .dataSourceApi(dataSourceApi)
                                               .intervalRequest(request.getInterval())
                                               .condition(request.getCondition())
                                               .build(request.getExpression());

        return evaluator.evaluate()
                        .get()
                        .toTimeSeriesResultSet();
    }
}
