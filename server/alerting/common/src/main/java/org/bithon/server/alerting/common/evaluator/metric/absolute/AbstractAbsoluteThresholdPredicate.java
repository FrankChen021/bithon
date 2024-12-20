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

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.HumanReadableNumber;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.alerting.common.evaluator.EvaluationContext;
import org.bithon.server.alerting.common.evaluator.metric.IMetricEvaluator;
import org.bithon.server.alerting.common.evaluator.result.AbsoluteComparisonEvaluationOutput;
import org.bithon.server.alerting.common.evaluator.result.IEvaluationOutput;
import org.bithon.server.commons.time.TimeSpan;
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
 * @author frank.chen021@outlook.com
 * @date 2020/12/14 4:22 下午
 */
public abstract class AbstractAbsoluteThresholdPredicate implements IMetricEvaluator {

    @Getter
    @NotNull
    private final Object expected;

    /**
     * runtime property
     */
    @JsonIgnore
    private final Number expectedValue;

    private final String predicate;

    public AbstractAbsoluteThresholdPredicate(String predicate, Object expected) {
        this.predicate = Preconditions.checkArgumentNotNull("predicate", predicate);
        this.expected = Preconditions.checkArgumentNotNull("expected", expected);

        Number tmp;
        if (expected instanceof Number) {
            tmp = (Number) expected;
        } else if (expected instanceof String) {
            try {
                tmp = HumanReadableNumber.parse((String) expected);
            } catch (HumanReadableNumber.IAE e) {
                tmp = Double.parseDouble((String) expected);
            }
        } else {
            throw new IllegalArgumentException("invalid argument [expected]: " + expected);
        }
        this.expectedValue = tmp;
    }

    @Override
    public String toString() {
        if (expected instanceof String) {
            return predicate + " '" + expected + "'";
        } else {
            return predicate + " " + expected;
        }
    }

    @Override
    public IEvaluationOutput evaluate(IDataSourceApi dataSourceApi,
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
        if (CollectionUtils.isEmpty(now)) {
            return null;
        }

        // TODO: iterate the result set if groupBy has been set
        Number nowValue = (Number) now.get(0).get(metric.getName());
        if (nowValue == null) {
            return null;
        }

        IDataType valueType;
        if ("count".equals(metric.getAggregator())) {
            valueType = IDataType.LONG;
        } else {
            // For other aggregators, use the type of the column
            valueType = dataSourceApi.getSchemaByName(dataSource)
                                     .getColumnByName(metric.getName())
                                     .getDataType();
        }
        return new AbsoluteComparisonEvaluationOutput(start,
                                                      end,
                                                      valueType.format(nowValue),
                                                      expected.toString(),
                                                      valueType.format(valueType.diff(nowValue, expectedValue)),
                                                      matches(valueType, expectedValue, nowValue));
    }

    protected abstract boolean matches(IDataType valueType, Number threshold, Number now);
}
