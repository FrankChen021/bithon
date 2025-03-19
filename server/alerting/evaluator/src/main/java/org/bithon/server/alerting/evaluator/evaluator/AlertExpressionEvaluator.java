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
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.server.alerting.common.evaluator.EvaluationContext;
import org.bithon.server.alerting.common.evaluator.metric.IMetricEvaluator;
import org.bithon.server.alerting.common.evaluator.metric.MetricEvaluatorWithLogger;
import org.bithon.server.alerting.common.evaluator.result.EvaluationOutputs;
import org.bithon.server.alerting.common.model.AlertExpression;
import org.bithon.server.alerting.common.model.IAlertExpressionVisitor;
import org.bithon.server.commons.time.TimeSpan;

import java.util.List;

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
        EvaluationOutputs outputs = this.expression.accept(new IAlertExpressionVisitor<>() {
            @Override
            public EvaluationOutputs visit(LogicalExpression expression) {
                if (expression instanceof LogicalExpression.AND) {

                    EvaluationOutputs mergedOutputs = null;

                    List<IExpression> operands = expression.getOperands();
                    for (IExpression operand : operands) {
                        EvaluationOutputs outputs = operand.accept(this);
                        if (!outputs.isMatched()) {
                            return outputs;
                        }
                        if (mergedOutputs == null) {
                            mergedOutputs = outputs;
                        } else {
                            mergedOutputs = mergedOutputs.intersect(outputs);
                        }
                    }

                    return mergedOutputs;

                } else if (expression instanceof LogicalExpression.OR) {
                    for (IExpression operand : expression.getOperands()) {
                        EvaluationOutputs outputs = operand.accept(this);
                        if (outputs.isMatched()) {
                            return outputs;
                        }
                    }
                    return EvaluationOutputs.EMPTY;
                } else {
                    throw new UnsupportedOperationException("Unsupported logical expression: " + expression.getClass().getName());
                }
            }

            @Override
            public EvaluationOutputs visit(AlertExpression expression) {
                return evaluate(expression, context);
            }
        });

        context.setOutputs(outputs);
        return outputs.isMatched();
    }

    private EvaluationOutputs evaluate(AlertExpression expression, EvaluationContext context) {
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
                                                                                            CollectionUtils.emptyOrOriginal(expression.getMetricExpression().getGroupBy()),
                                                                                            context);
        if (outputs.isEmpty() || !outputs.isMatched()) {
            context.setEvaluationResult(expression.getId(), false, null);
            return outputs;
        }

        outputs.removeIf((output) -> !output.isMatched());
        context.setEvaluationResult(expression.getId(), true, outputs);
        return outputs;
    }
}
