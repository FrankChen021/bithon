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
                            "Current: [null], Expected: [%s], Delta: [null], Result: [NOT Matched]",
                            delegateEvaluator.toString());
                return outputs;
            }

            for (IEvaluationOutput output : outputs) {
                if (output.getLabel().isEmpty()) {
                    context.log(delegateEvaluator.getClass(),
                                "%s %s, [%s], Delta: [%s]",
                                output.getCurrentText(),
                                delegateEvaluator.toString(),
                                output.isMatched() ? "MATCHED" : "NOT Matched",
                                output.getDeltaText());
                } else {
                    context.log(delegateEvaluator.getClass(),
                                "%s{%s} %s, [%s], Delta: [%s]",
                                output.getCurrentText(),
                                output.getLabel(),
                                delegateEvaluator.toString(),
                                output.isMatched() ? "MATCHED" : "NOT Matched",
                                output.getDeltaText());
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
