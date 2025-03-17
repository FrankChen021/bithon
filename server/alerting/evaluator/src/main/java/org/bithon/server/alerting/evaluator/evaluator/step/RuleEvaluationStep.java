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

package org.bithon.server.alerting.evaluator.evaluator.step;

import org.bithon.server.alerting.common.evaluator.EvaluationContext;
import org.bithon.server.alerting.common.model.AlertRule;
import org.bithon.server.alerting.evaluator.state.IEvaluationStateManager;
import org.bithon.server.storage.alerting.Label;
import org.bithon.server.storage.alerting.pojo.AlertState;
import org.bithon.server.storage.alerting.pojo.AlertStatus;

import java.time.Duration;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/11 10:40 上午
 */
public class RuleEvaluationStep implements IEvaluationStep {

    @Override
    public void evaluate(IEvaluationStateManager stateManager, EvaluationContext context) {
        AlertRule alertRule = context.getAlertRule();
        AlertState prevState = context.getPrevState();

        if (!context.isExpressionEvaluatedAsTrue()) {
            context.log(RuleEvaluationStep.class,
                        "Rule [%s] evaluated as FALSE",
                        context.getAlertRule().getName());

            stateManager.resetMatchCount(context.getAlertRule().getId());

            if (prevState != null) {
                for (Map.Entry<Label, AlertState.SeriesState> item : prevState.getPayload().getSeries().entrySet()) {
                    context.getSeriesStatus().put(item.getKey(), AlertStatus.RESOLVED);
                }
            }
            return;
        }

        context.log(RuleEvaluationStep.class, "Rule [%s] evaluated as TRUE", alertRule.getName());

        long expectedMatchCount = alertRule.getExpectedMatchCount();

        Map<Label, Long> successiveCountList = stateManager.incrMatchCount(alertRule.getId(),
                                                                           context.getGroups(),
                                                                           alertRule.getEvery()
                                                                                    .getDuration()
                                                                                    // Add 30 seconds for margin
                                                                                    .plus(Duration.ofSeconds(30)));
        for (Map.Entry<Label, Long> entry : successiveCountList.entrySet()) {
            Label label = entry.getKey();
            long successiveCount = entry.getValue();

            AlertStatus newStatus;
            if (successiveCount >= expectedMatchCount) {
                stateManager.resetMatchCount(alertRule.getId());
                newStatus = AlertStatus.ALERTING;
            } else {
                context.log(RuleEvaluationStep.class,
                            "Rule [%s] evaluated as TRUE for [%d] times successively，NOT reach the expected threshold [%s] to fire alert",
                            alertRule.getName(),
                            successiveCount,
                            expectedMatchCount);

                newStatus = AlertStatus.PENDING;
            }

            context.getSeriesStatus().put(label, newStatus);
        }
    }
}
