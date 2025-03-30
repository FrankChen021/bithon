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

package org.bithon.server.alerting.evaluator.evaluator.pipeline;


import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.server.alerting.common.evaluator.EvaluationContext;
import org.bithon.server.alerting.common.evaluator.metric.IMetricEvaluator;
import org.bithon.server.alerting.common.evaluator.metric.MetricEvaluatorWithLogger;
import org.bithon.server.alerting.common.evaluator.result.EvaluationOutput;
import org.bithon.server.alerting.common.evaluator.result.EvaluationOutputs;
import org.bithon.server.alerting.common.model.AlertExpression;
import org.bithon.server.alerting.common.model.AlertRule;
import org.bithon.server.alerting.common.model.IAlertExpressionVisitor;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.alerting.Label;
import org.bithon.server.storage.alerting.pojo.AlertStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/8 3:29 下午
 */
public class ExpressionEvaluationStep implements IPipelineStep {

    static class CombinableOutput {
        private final EvaluationOutputs nonGroupedOutputs;
        private Map<Label, EvaluationOutputs> groupedOutputs;

        public CombinableOutput() {
            this.nonGroupedOutputs = new EvaluationOutputs();
            this.groupedOutputs = null;
        }

        public CombinableOutput(EvaluationOutputs outputs) {
            if (outputs.size() == 1 && outputs.get(0).getLabel().isEmpty()) {
                this.nonGroupedOutputs = outputs;
                this.groupedOutputs = null;
            } else {
                this.nonGroupedOutputs = new EvaluationOutputs();

                this.groupedOutputs = new HashMap<>();
                for (EvaluationOutput output : outputs) {
                    groupedOutputs.computeIfAbsent(output.getLabel(), v -> new EvaluationOutputs())
                                  .add(output);
                }
            }
        }

        public void combine(CombinableOutput other) {
            this.nonGroupedOutputs.addAll(other.nonGroupedOutputs);

            if (other.groupedOutputs != null) {
                if (this.groupedOutputs == null) {
                    this.groupedOutputs = new HashMap<>(other.groupedOutputs);
                } else {
                    merge(this.groupedOutputs, other.groupedOutputs);
                }
            }
        }

        public boolean isMatched() {
            if (!this.nonGroupedOutputs.isEmpty()) {
                if (!this.nonGroupedOutputs.isMatched()) {
                    return false;
                }
            }

            if (groupedOutputs != null) {
                return groupedOutputs.values()
                                     .stream()
                                     // Since we merge all series together including non-matched, we use 'any' to find the matched one
                                     .anyMatch(EvaluationOutputs::isMatched);
            }

            return !this.nonGroupedOutputs.isEmpty();
        }

        public Map<Label, EvaluationOutputs> getFinalizedCombination() {
            Map<Label, EvaluationOutputs> outputs = new HashMap<>();
            if (this.groupedOutputs != null) {
                outputs.putAll(this.groupedOutputs);
            }
            if (!this.nonGroupedOutputs.isEmpty()) {
                outputs.put(Label.EMPTY, this.nonGroupedOutputs);
            }
            return outputs;
        }

        private void merge(Map<Label, EvaluationOutputs> lhs, Map<Label, EvaluationOutputs> rhs) {

            // For each element in lhs, if it's not in rhs, set its status to RESOLVED
            for (Map.Entry<Label, EvaluationOutputs> entry : lhs.entrySet()) {
                Label lLabel = entry.getKey();
                if (!lLabel.isEmpty() // This series is an output of GROUP-BY
                    && !rhs.containsKey(lLabel)) {
                    entry.getValue().setMatched(false);
                }
            }

            // For each element in rhs, if it's not in lhs, set its status to RESOLVED, otherwise merge the outputs
            for (Map.Entry<Label, EvaluationOutputs> right : rhs.entrySet()) {
                Label rLabel = right.getKey();
                if (!lhs.containsKey(rLabel)) {
                    if (!rLabel.isEmpty()) {
                        // this is the output of groupBy expression,
                        // and this series does not exist
                        right.getValue().setMatched(false);
                    }
                    lhs.put(rLabel, right.getValue());
                } else {
                    lhs.get(rLabel).addAll(right.getValue());
                }
            }
        }
    }

    @Override
    public void evaluate(EvaluationContext context) {
        AlertRule alertRule = context.getAlertRule();

        if (alertRule.getFlattenExpressions().size() > 1) {
            // If the size is 1, the alertExpression is the SAME as the expression, so no need to log it again which will be logged in the ExpressionEvaluationStep
            context.log(ExpressionEvaluationStep.class, "Evaluating expression [%s]: %s ", alertRule.getName(), alertRule.getExpr());
        }


        CombinableOutput combinedOutput = alertRule.getAlertExpression().accept(new IAlertExpressionVisitor<>() {
            @Override
            public CombinableOutput visit(LogicalExpression expression) {
                if (expression instanceof LogicalExpression.AND) {

                    CombinableOutput combinedOutput = new CombinableOutput();

                    List<IExpression> operands = expression.getOperands();
                    for (IExpression operand : operands) {
                        CombinableOutput output = operand.accept(this);
                        if (!output.isMatched()) {
                            return output;
                        }

                        combinedOutput.combine(output);
                    }

                    return combinedOutput;

                } else if (expression instanceof LogicalExpression.OR) {
                    for (IExpression operand : expression.getOperands()) {
                        CombinableOutput outputs = operand.accept(this);
                        if (outputs.isMatched()) {
                            return outputs;
                        }
                    }

                    return new CombinableOutput();
                } else {
                    throw new UnsupportedOperationException("Unsupported logical expression: " + expression.getClass().getName());
                }
            }

            @Override
            public CombinableOutput visit(AlertExpression expression) {
                return evaluate(expression, context);
            }
        });


        boolean isTrue = combinedOutput.isMatched();
        context.setExpressionEvaluatedAsTrue(isTrue);
        if (!isTrue) {
            return;
        }

        Map<Label, EvaluationOutputs> outputs = combinedOutput.getFinalizedCombination();
        for (Map.Entry<Label, EvaluationOutputs> entry : outputs.entrySet()) {
            Label label = entry.getKey();
            EvaluationOutputs evaluationOutputs = entry.getValue();
            evaluationOutputs.setStatus(evaluationOutputs.isMatched() ? AlertStatus.ALERTING : AlertStatus.RESOLVED);
            context.getOutputs()
                   .put(label, evaluationOutputs);
        }
    }

    private CombinableOutput evaluate(AlertExpression expression, EvaluationContext context) {
        context.log(ExpressionEvaluationStep.class, "Evaluating expression [%s]: %s", expression.getId(), expression.serializeToText());

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
        if (outputs == null) {
            return new CombinableOutput();
        }

        // Update some runtime variables
        outputs.forEach((output) -> {
            output.setExpressionId(expression.getId());
            output.setStart(start.getMilliseconds());
            output.setEnd(end.getMilliseconds());
        });

        if (outputs.isEmpty() || !outputs.isMatched()) {
            return new CombinableOutput();
        }

        // Remove all un-matched series and return values by Label
        EvaluationOutputs result = new EvaluationOutputs();
        for (EvaluationOutput output : outputs) {
            if (output.isMatched()) {
                result.add(output);
            }
        }
        return new CombinableOutput(result);
    }
}
