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
import org.bithon.server.alerting.common.evaluator.result.EvaluationOutputs;
import org.bithon.server.alerting.common.evaluator.result.IEvaluationOutput;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.web.service.datasource.api.IDataSourceApi;
import org.bithon.server.web.service.datasource.api.QueryField;

import java.util.Set;

/**
 * @author frankchen
 * @date 2020-08-25 14:01:34
 */
public class MetricEvaluatorWithLogger implements IMetricEvaluator {

    private final IMetricEvaluator delegateEvaluator;

    public MetricEvaluatorWithLogger(IMetricEvaluator delegate) {
        this.delegateEvaluator = delegate;
    }

    @Override
    public EvaluationOutputs evaluate(IDataSourceApi dataSourceApi,
                                      String dataSource,
                                      QueryField metric,
                                      TimeSpan start,
                                      TimeSpan end,
                                      String filterExpression,
                                      Set<String> groupBy,
                                      EvaluationContext context) {
        try {
            context.log(delegateEvaluator.getClass(),
                        "Evaluating %s in interval [%s, %s)",
                        context.getEvaluatingExpression().serializeToText(false),
                        start.format("HH:mm"),
                        end.format("HH:mm"));

            EvaluationOutputs outputs = delegateEvaluator.evaluate(dataSourceApi,
                                                                   dataSource,
                                                                   metric,
                                                                   start,
                                                                   end,
                                                                   filterExpression,
                                                                   groupBy,
                                                                   context);
            if (outputs.isEmpty()) {
                context.log(delegateEvaluator.getClass(),
                            "Expected: [%s], Current: [null], Delta: [null], Result: [NOT Matched]",
                            delegateEvaluator.toString());
                return outputs;
            }

            for (IEvaluationOutput output : outputs) {
                if (output.getLabels().isEmpty()) {
                    context.log(delegateEvaluator.getClass(),
                                "Expected: [%s], Current: [%s], Delta: [%s], Result: [%s]",
                                delegateEvaluator.toString(),
                                output.getCurrentText(),
                                output.getDeltaText(),
                                output.isMatched() ? "Matched" : "NOT Matched");
                } else {
                    context.log(delegateEvaluator.getClass(),
                                "Expected: [%s], Current: [%s {%s}], Delta: [%s], Result: [%s]",
                                delegateEvaluator.toString(),
                                output.getCurrentText(),
                                output.getLabels(),
                                output.getDeltaText(),
                                output.isMatched() ? "Matched" : "NOT Matched");
                }
            }

            return outputs;
        } catch (Exception e) {
            context.logException(MetricEvaluatorWithLogger.class,
                                 e,
                                 "Exception during evaluation: %s",
                                 context.getEvaluatingExpression().getId(),
                                 delegateEvaluator.toString(),
                                 e.getMessage());
            return null;
        }
    }
}
