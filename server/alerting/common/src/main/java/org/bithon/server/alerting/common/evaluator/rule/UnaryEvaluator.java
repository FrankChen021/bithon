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

package org.bithon.server.alerting.common.evaluator.rule;

import lombok.extern.slf4j.Slf4j;
import org.bithon.server.alerting.common.evaluator.EvaluatorContext;
import org.bithon.server.alerting.common.evaluator.EvaluatorDecoratorWithLogger;
import org.bithon.server.alerting.common.evaluator.result.EvaluationResult;
import org.bithon.server.alerting.common.evaluator.result.IEvaluationOutput;
import org.bithon.server.alerting.common.model.AlertCondition;
import org.bithon.server.alerting.common.model.IMetricCondition;
import org.bithon.server.commons.time.TimeSpan;

import java.util.concurrent.TimeUnit;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/8 3:29 下午
 */
@Slf4j
public class UnaryEvaluator implements IAlertRuleEvaluator {

    private final AlertCondition condition;

    public UnaryEvaluator(AlertCondition condition) {
        this.condition = condition;
    }

    @Override
    public IAlertRuleEvaluator or(IAlertRuleEvaluator rightOp) {
        return new BinaryOrEvaluator(this, rightOp);
    }

    @Override
    public IAlertRuleEvaluator and(IAlertRuleEvaluator rightOp) {
        return new BinaryAndEvaluator(this, rightOp);
    }

    @Override
    public boolean test(EvaluatorContext context) {
        context.log(UnaryEvaluator.class, "Evaluating condition [%s]...", condition.getId());

        EvaluationResult result = context.getConditionEvaluationResult(condition.getId());
        if (EvaluationResult.MATCHED.equals(result)) {
            context.log(UnaryEvaluator.class, "Condition [%s] Satisfied", condition.getId());
            return true;
        }
        if (EvaluationResult.UNMATCHED.equals(result)) {
            context.log(UnaryEvaluator.class, "Condition [%s] NOT Satisfied", condition.getId());
            return false;
        }

        IMetricCondition metricCondition = condition.getMetric();

        context.setEvaluatingCondition(this.condition);
        context.setEvaluatingMetric(metricCondition);

        TimeSpan end = context.getIntervalEnd();
        TimeSpan start = end.before(this.condition.getMetric().getWindow(), TimeUnit.MINUTES);
        IEvaluationOutput output = new EvaluatorDecoratorWithLogger(metricCondition).evaluate(context.getDataSourceApi(),
                                                                                              condition.getDataSource(),
                                                                                              start,
                                                                                              context.getIntervalEnd(),
                                                                                              condition.getDimensions(),
                                                                                              context);
        if (output == null || !output.isMatches()) {
            context.setConditionEvaluationResult(condition.getId(), false, null);
            return false;
        }

        context.setConditionEvaluationResult(condition.getId(), true, output);

        return true;
    }
}
