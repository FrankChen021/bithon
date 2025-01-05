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

package org.bithon.server.alerting.common.evaluator.metric.absolute;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.server.alerting.common.evaluator.EvaluationContext;
import org.bithon.server.alerting.common.evaluator.metric.IMetricEvaluator;
import org.bithon.server.alerting.common.evaluator.result.AbsoluteComparisonEvaluationOutput;
import org.bithon.server.alerting.common.evaluator.result.EvaluationOutputs;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.alerting.Labels;
import org.bithon.server.web.service.datasource.api.IDataSourceApi;
import org.bithon.server.web.service.datasource.api.IntervalRequest;
import org.bithon.server.web.service.datasource.api.QueryField;
import org.bithon.server.web.service.datasource.api.QueryRequest;
import org.bithon.server.web.service.datasource.api.QueryResponse;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Frank Chen
 * @date 28/3/22 10:53 PM
 */
public class NullValuePredicate implements IMetricEvaluator {

    @JsonCreator
    public NullValuePredicate() {
    }

    @Override
    public EvaluationOutputs evaluate(IDataSourceApi dataSourceApi,
                                      String dataSource,
                                      QueryField metric,
                                      TimeSpan start,
                                      TimeSpan end,
                                      String filterExpression,
                                      Set<String> groupBy,
                                      EvaluationContext context) throws IOException {
        QueryResponse response = dataSourceApi.groupBy(QueryRequest.builder()
                                                                   .dataSource(dataSource)
                                                                   .interval(IntervalRequest.builder()
                                                                                            .startISO8601(start.toISO8601())
                                                                                            .endISO8601(end.toISO8601())
                                                                                            .build())
                                                                   .filterExpression(filterExpression)
                                                                   .fields(Collections.singletonList(metric))
                                                                   .groupBy(groupBy)
                                                                   .build());

        //noinspection unchecked
        List<Map<String, Object>> now = (List<Map<String, Object>>) response.getData();
        boolean matches;
        Number nowValue = null;
        if (CollectionUtils.isEmpty(now) || !now.get(0).containsKey(metric.getName())) {
            matches = true;
        } else {
            nowValue = (Number) now.get(0).get(metric.getName());
            matches = nowValue == null;
        }

        IDataType valueType = dataSourceApi.getSchemaByName(dataSource).getColumnByName(metric.getName()).getDataType();
        return new EvaluationOutputs(new AbsoluteComparisonEvaluationOutput(start,
                                                                            end,
                                                                            new Labels(),
                                                                            nowValue == null ? null : valueType.format(nowValue),
                                                                            "null",
                                                                            nowValue == null ? null : nowValue.toString(),
                                                                            matches));
    }

    @Override
    public String toString() {
        return "is null";
    }
}
