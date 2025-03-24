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


import org.bithon.server.alerting.common.evaluator.EvaluationContext;
import org.bithon.server.alerting.common.model.AlertRule;
import org.bithon.server.alerting.evaluator.evaluator.AlertExpressionEvaluator;
import org.bithon.server.alerting.evaluator.state.IEvaluationStateManager;

/**
 * @author frank.chen021@outlook.com
 * @date 18/3/25 9:45 am
 */
public class ExpressionEvaluationStep implements IPipelineStep {
    @Override
    public void evaluate(IEvaluationStateManager stateManager, EvaluationContext context) {
        AlertRule alertRule = context.getAlertRule();

        if (alertRule.getFlattenExpressions().size() > 1) {
            // If the size is 1, the alertExpression is the SAME as the expression, so no need to log it again which will be logged in the ExpressionEvaluationStep
            context.log(ExpressionEvaluationStep.class, "Evaluating expression [%s]: %s ", alertRule.getName(), alertRule.getExpr());
        }

        boolean isTrue = new AlertExpressionEvaluator(context.getAlertRule().getAlertExpression()).evaluate(context);
        context.setExpressionEvaluatedAsTrue(isTrue);
    }
}
