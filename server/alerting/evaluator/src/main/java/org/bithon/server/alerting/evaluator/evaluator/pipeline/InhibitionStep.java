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


import org.bithon.component.commons.utils.HumanReadableDuration;
import org.bithon.server.alerting.common.evaluator.EvaluationContext;
import org.bithon.server.alerting.common.evaluator.result.EvaluationOutputs;
import org.bithon.server.alerting.common.evaluator.state.IEvaluationStateManager;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.alerting.Label;
import org.bithon.server.storage.alerting.pojo.AlertStatus;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 17/3/25 11:02 pm
 */
public class InhibitionStep implements IPipelineStep {

    @Override
    public void evaluate(EvaluationContext context) {
        for (Map.Entry<Label, EvaluationOutputs> entry : context.getOutputs().entrySet()) {
            Label label = entry.getKey();
            EvaluationOutputs state = entry.getValue();

            if (state.getStatus() == AlertStatus.ALERTING) {
                AlertStatus newStatus = inhibit(context, label);
                state.setStatus(newStatus);
            }
        }
    }

    private AlertStatus inhibit(EvaluationContext context, Label label) {
        String lastAlertingAt = "N/A";
        LocalDateTime timestamp = context.getStateManager().getLastAlertAt();
        if (timestamp != null) {
            lastAlertingAt = TimeSpan.of(Timestamp.valueOf(timestamp).getTime()).format("HH:mm:ss");
        }

        HumanReadableDuration silenceDuration = context.getAlertRule().getNotificationProps().getSilence();
        // Calc the silence period by adding some margin
        // Let's say current timestamp is 10:01:02.123, and the silence period is 1 minute,
        // The silence period is [now(), 10:02:00.000(assuming the evaluation execution finishes within 1 minute) + 1 minute]
        TimeSpan now = TimeSpan.now();
        TimeSpan endOfThisMinute = now.ceil(Duration.ofMinutes(1));
        Duration silencePeriod = silenceDuration.getDuration().plus(Duration.ofMillis(endOfThisMinute.diff(now)));

        IEvaluationStateManager stateManager = context.getStateManager();
        if (silenceDuration.getDuration().getSeconds() > 0
            && stateManager.tryEnterSilence(label, silencePeriod)) {
            Duration silenceRemainTime = stateManager.getSilenceRemainTime(label);
            context.log(InhibitionStep.class,
                        "Alerting%s，but is under notification silence duration (%s) from last alerting timestamp %s to %s.",
                        label.formatIfNotEmpty(" for series {%s}"),
                        silenceDuration,
                        lastAlertingAt,
                        TimeSpan.of(System.currentTimeMillis() + silenceRemainTime.toMillis()).format("HH:mm:ss"));
            return AlertStatus.SUPPRESSING;
        } else {
            context.log(InhibitionStep.class, "Alerting%s，silence period(%s) is over. Last alert at: %s",
                        label.formatIfNotEmpty(" for series {%s}"),
                        silenceDuration,
                        lastAlertingAt);
        }

        return AlertStatus.ALERTING;
    }
}
