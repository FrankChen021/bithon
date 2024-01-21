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

package org.bithon.server.alerting.common.evaluator.metric;

import org.bithon.server.alerting.common.evaluator.EvaluationContext;
import org.bithon.server.alerting.common.evaluator.result.IEvaluationOutput;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.web.service.datasource.api.IDataSourceApi;
import org.bithon.server.web.service.datasource.api.QueryField;

import java.util.List;

/**
 * @author frankchen
 * @date 2020-08-25 14:01:34
 */
public class MetricEvaluatorWithLogger implements IMetricEvaluator {

    private final IMetricEvaluator delegate;

    public MetricEvaluatorWithLogger(IMetricEvaluator delegate) {
        this.delegate = delegate;
    }

    @Override
    public IEvaluationOutput evaluate(IDataSourceApi dataSourceApi,
                                      String dataSource,
                                      QueryField metric,
                                      TimeSpan start,
                                      TimeSpan end,
                                      String filterExpression,
                                      List<String> groupBy,
                                      EvaluationContext context) {
        try {
            context.log(delegate.getClass(), "Evaluating metric: %s", context.getEvaluatingExpression().serializeToText(false));

            IEvaluationOutput output = delegate.evaluate(dataSourceApi,
                                                         dataSource,
                                                         metric,
                                                         start,
                                                         end,
                                                         filterExpression,
                                                         groupBy,
                                                         context);

            context.log(delegate.getClass(),
                        "Expected: [%s], Current: [%s], Incremental: [%s], Result: [%s]",
                        delegate.toString(),
                        output == null ? "null" : output.getCurrentText(),
                        output == null ? "null" : output.getDeltaText(),
                        (output != null && output.isMatches()) ? "Matched" : "NOT Matched");

            return output;
        } catch (Exception e) {
            context.logException(MetricEvaluatorWithLogger.class,
                                 e,
                                 "Exception during evaluation: %s",
                                 context.getEvaluatingExpression().getId(),
                                 context.getEvaluatingMetric().toString(),
                                 e.getMessage());
            return null;
        }
    }
}
