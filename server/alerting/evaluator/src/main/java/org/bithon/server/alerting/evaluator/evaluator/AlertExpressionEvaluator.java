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

package org.bithon.server.alerting.evaluator.evaluator;

import lombok.extern.slf4j.Slf4j;
import org.bithon.server.alerting.common.evaluator.EvaluationContext;
import org.bithon.server.alerting.common.evaluator.metric.IMetricEvaluator;
import org.bithon.server.alerting.common.evaluator.metric.MetricEvaluatorWithLogger;
import org.bithon.server.alerting.common.evaluator.result.IEvaluationOutput;
import org.bithon.server.alerting.common.model.AlertExpression;
import org.bithon.server.commons.time.TimeSpan;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/8 3:29 下午
 */
@Slf4j
public class AlertExpressionEvaluator {

    private final AlertExpression expression;

    public AlertExpressionEvaluator(AlertExpression expression) {
        this.expression = expression;
    }

    public boolean evaluate(EvaluationContext context) {
        context.log(AlertExpressionEvaluator.class, "Evaluating expression: %s", expression.serializeToText());

        IMetricEvaluator metricEvaluator = expression.getMetricEvaluator();
        context.setEvaluatingExpression(this.expression);

        TimeSpan end = context.getIntervalEnd();
        TimeSpan start = end.before(expression.getWindow());
        IEvaluationOutput output = new MetricEvaluatorWithLogger(metricEvaluator).evaluate(context.getDataSourceApi(),
                                                                                           expression.getFrom(),
                                                                                           expression.getSelect(),
                                                                                           start,
                                                                                           context.getIntervalEnd(),
                                                                                           expression.getWhere(),
                                                                                           expression.getGroupBy(),
                                                                                           context);
        if (output == null || !output.isMatches()) {
            context.setEvaluationResult(expression.getId(), false, null);
            return false;
        }

        context.setEvaluationResult(expression.getId(), true, output);

        return true;
    }
}
