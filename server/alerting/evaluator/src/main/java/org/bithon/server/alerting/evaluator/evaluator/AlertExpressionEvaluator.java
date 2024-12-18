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
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.server.alerting.common.evaluator.EvaluationContext;
import org.bithon.server.alerting.common.evaluator.metric.IMetricEvaluator;
import org.bithon.server.alerting.common.evaluator.metric.MetricEvaluatorWithLogger;
import org.bithon.server.alerting.common.evaluator.result.EvaluationOutputs;
import org.bithon.server.alerting.common.evaluator.result.IEvaluationOutput;
import org.bithon.server.alerting.common.model.AlertExpression;
import org.bithon.server.alerting.common.model.IAlertExpressionVisitor;
import org.bithon.server.commons.time.TimeSpan;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/8 3:29 下午
 */
@Slf4j
public class AlertExpressionEvaluator {

    private final IExpression expression;

    public AlertExpressionEvaluator(IExpression expression) {
        this.expression = expression;
    }

    public boolean evaluate(EvaluationContext context) {
        return this.expression.accept(new IAlertExpressionVisitor<>() {
            @Override
            public Boolean visit(LogicalExpression expression) {
                if (expression instanceof LogicalExpression.AND) {
                    for (IExpression operand : expression.getOperands()) {
                        // TODO: JOIN the Output
                        if (!operand.accept(this)) {
                            return false;
                        }
                    }
                } else if (expression instanceof LogicalExpression.OR) {
                    return expression.getOperands().stream().anyMatch(e -> e.accept(this));
                } else {
                    throw new UnsupportedOperationException("Unsupported logical expression: " + expression.getClass().getName());
                }
            }

            @Override
            public Boolean visit(AlertExpression expression) {
                boolean isTrue = evaluate(expression, context);
                if (isTrue) {
                    EvaluationOutputs outputs = context.getEvaluationOutputs().get(expression.getId());
                    for (IEvaluationOutput output : outputs) {
                        context.getGroups().add(output.getLabelValues());
                    }
                }
                return isTrue;
            }
        });
    }

    private boolean evaluate(AlertExpression expression, EvaluationContext context) {
        context.log(AlertExpressionEvaluator.class, "Evaluating expression [%s]: %s", expression.getId(), expression.serializeToText());

        IMetricEvaluator metricEvaluator = expression.getMetricEvaluator();
        context.setEvaluatingExpression(expression);

        TimeSpan end = context.getIntervalEnd();
        TimeSpan start = end.before(expression.getMetricExpression().getWindow());
        EvaluationOutputs outputs = new MetricEvaluatorWithLogger(metricEvaluator).evaluate(context.getDataSourceApi(),
                                                                                            expression.getMetricExpression().getFrom(),
                                                                                            expression.getMetricExpression().getMetric(),
                                                                                            start,
                                                                                            context.getIntervalEnd(),
                                                                                            expression.getMetricExpression().getWhereText(),
                                                                                            expression.getMetricExpression().getGroupBy(),
                                                                                            context);
        if (outputs.isEmpty() || !outputs.isMatched()) {
            context.setEvaluationResult(expression.getId(), false, null);
            return false;
        }

        outputs.removeIf((output) -> !output.isMatched());
        context.setEvaluationResult(expression.getId(), true, outputs);

        return true;
    }
}
