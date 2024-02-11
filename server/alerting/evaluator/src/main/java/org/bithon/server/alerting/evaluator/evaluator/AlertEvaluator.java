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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.HumanReadableDuration;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.alerting.common.evaluator.EvaluationContext;
import org.bithon.server.alerting.common.evaluator.result.EvaluationResult;
import org.bithon.server.alerting.common.evaluator.result.IEvaluationOutput;
import org.bithon.server.alerting.common.model.AlertExpression;
import org.bithon.server.alerting.common.model.AlertRule;
import org.bithon.server.alerting.evaluator.EvaluatorModuleEnabler;
import org.bithon.server.alerting.notification.api.INotificationApi;
import org.bithon.server.alerting.notification.image.RenderingConfig;
import org.bithon.server.alerting.notification.message.ConditionEvaluationResult;
import org.bithon.server.alerting.notification.message.NotificationMessage;
import org.bithon.server.alerting.notification.message.OutputMessage;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.alerting.IAlertRecordStorage;
import org.bithon.server.storage.alerting.IAlertStateStorage;
import org.bithon.server.storage.alerting.IEvaluationLogStorage;
import org.bithon.server.storage.alerting.pojo.AlertRecordObject;
import org.bithon.server.web.service.datasource.api.IDataSourceApi;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Duration;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/11 10:40 上午
 */
@Slf4j
@Service
@Conditional(EvaluatorModuleEnabler.class)
public class AlertEvaluator {

    private final IAlertStateStorage stateStorage;
    private final IEvaluationLogStorage evaluationLoggerFactory;
    private final RenderingConfig renderingConfig;
    private final IAlertRecordStorage alertRecordStorage;
    private final ObjectMapper objectMapper;
    private final IDataSourceApi dataSourceApi;
    private final INotificationApi notificationApi;

    public AlertEvaluator(IAlertStateStorage stateStorage,
                          IEvaluationLogStorage evaluationLoggerFactory,
                          RenderingConfig notificationConfig,
                          IAlertRecordStorage alertRecordDao,
                          IDataSourceApi dataSourceApi,
                          INotificationApi notificationApi) {
        this.stateStorage = stateStorage;
        this.evaluationLoggerFactory = evaluationLoggerFactory;
        this.renderingConfig = notificationConfig;
        this.alertRecordStorage = alertRecordDao;
        this.dataSourceApi = dataSourceApi;
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        this.notificationApi = notificationApi;
    }

    public void evaluate(TimeSpan now, AlertRule alertRule) {
        EvaluationContext context = new EvaluationContext(now,
                                                          evaluationLoggerFactory.createWriter(),
                                                          alertRule,
                                                          dataSourceApi);
        try {
            if (!alertRule.isEnabled()) {
                context.log(AlertEvaluator.class, "Alert is disabled. Evaluation is skipped.");
                return;
            }
            if (evaluate(context)) {
                notify(alertRule, context);
            }
        } catch (Exception e) {
            log.error(StringUtils.format("ERROR to evaluate alert %s", alertRule.getName()), e);
        } finally {
            try {
                context.getEvaluatorLogger().flush();
            } catch (Exception e) {
                log.error("Flush log for alert {} failed: {}", alertRule.getId(), e.toString());
            }
        }
    }

    private boolean evaluate(EvaluationContext context) {
        long waitMinute = context.getIntervalEnd().getMilliseconds() % (context.getAlertRule().getEvaluationInterval() * 60L);
        if (waitMinute != 0) {
            context.log(AlertEvaluator.class,
                        "Alert not evaluated because it's not the right time. Will evaluate this alert at [%s]",
                        context.getIntervalEnd().after(waitMinute, TimeUnit.MINUTES).format("HH:mm"));
            return false;
        }

        AlertRule alertRule = context.getAlertRule();
        context.log(AlertEvaluator.class, "Evaluating alert [%s] %s ", alertRule.getName(), alertRule.getExpr());

        try {
            if ((boolean) (alertRule.getEvaluationExpression().evaluate(context))) {
                context.log(AlertEvaluator.class, "alert [%s] tested successfully.", alertRule.getName());

                long successiveCount = stateStorage.incrMatchCount(alertRule.getId(), alertRule.getForDuration().getDuration());
                if (successiveCount >= alertRule.getExpectedMatchCount()) {
                    stateStorage.resetMatchCount(alertRule.getId());

                    context.log(AlertEvaluator.class,
                                "Rule tested %d times successively，and reaches the expected count：%d",
                                successiveCount,
                                alertRule.getForDuration());

                } else {
                    context.log(AlertEvaluator.class, "Rule tested %d times successively，expected times：%d", successiveCount, alertRule.getForDuration());
                }
                return true;
            } else {
                stateStorage.resetMatchCount(alertRule.getId());
                return false;
            }
        } catch (Exception e) {
            context.logException(AlertEvaluator.class,
                                 e,
                                 "Exception during evaluation of alert [%s]: %s",
                                 alertRule.getName(),
                                 e.getMessage());
            return true;
        }
    }

    private void notify(AlertRule alertRule, EvaluationContext context) throws Exception {
        long now = System.currentTimeMillis();

        HumanReadableDuration silenceDuration = context.getAlertRule().getSilence();
        if (stateStorage.tryEnterSilence(alertRule.getId(), silenceDuration.getDuration())) {
            Duration silenceRemainTime = stateStorage.getSilenceRemainTime(alertRule.getId());
            context.log(AlertEvaluator.class, "Alerting，but is under silence period(%s)。Last alert at:%s",
                        silenceDuration,
                        TimeSpan.of(now - silenceDuration.getDuration().toMillis() - silenceRemainTime.toMillis()).toISO8601());
            return;
        }

        context.log(AlertEvaluator.class, "Sending alert");

        NotificationMessage notification = new NotificationMessage();
        notification.setAlertRule(alertRule);
        notification.setStart(context.getIntervalEnd().before(alertRule.getForDuration()).getMilliseconds());
        notification.setEnd(context.getIntervalEnd().getMilliseconds());
        notification.setDuration(alertRule.getForDuration());

        notification.setConditionEvaluation(new HashMap<>());
        if (this.renderingConfig.isEnabled()) {
            notification.setImages(new HashMap<>());
        }
        context.getEvaluationResults().forEach((expressionId, result) -> {
            AlertExpression condition = context.getAlertExpressions().get(expressionId);

            IEvaluationOutput outputs = context.getRuleEvaluationOutput(expressionId);
            notification.getConditionEvaluation()
                        .put(condition.getId(),
                             new ConditionEvaluationResult(result,
                                                           outputs == null ? null : OutputMessage.builder()
                                                                                                 .current(outputs.getCurrentText())
                                                                                                 .delta(outputs.getDeltaText())
                                                                                                 .threshold(outputs.getThresholdText())
                                                                                                 .build()));

            if (result == EvaluationResult.MATCHED && this.renderingConfig.isEnabled()) {
                /*
                notification.getImages().put(ruleId,
                                             this.imageRenderService.renderAndSaveAsync(alert.getNotifications().getImageMode(),
                                                                                        alert.getName(),
                                                                                        condition,
                                                                                        alert.getMatchTimes(),
                                                                                        context.getIntervalEnd()
                                                                                               .before(condition.getMetric().getWindow(), TimeUnit.MINUTES),
                                                                                        context.getIntervalEnd()));
                 */
            }
        });

        //
        // save
        //
        Timestamp lastAlertAt = alertRecordStorage.getLastAlert(alertRule.getId());
        AlertRecordObject alertRecord = new AlertRecordObject();
        alertRecord.setRecordId(UUID.randomUUID().toString().replace("-", ""));
        alertRecord.setAlertId(alertRule.getId());
        alertRecord.setAlertName(alertRule.getName());
        alertRecord.setAppName(alertRule.getAppName());
        alertRecord.setNamespace("");
        alertRecord.setPayload(objectMapper.writeValueAsString(AlertRecordPayload.builder()
                                                                                 .start(notification.getStart())
                                                                                 .end(notification.getEnd())
                                                                                 .expressions(context.getAlertExpressions().values())
                                                                                 .conditionEvaluation(notification.getConditionEvaluation())
                                                                                 .duration(notification.getDuration())
                                                                                 .build()));
        alertRecord.setNotificationStatus(IAlertRecordStorage.STATUS_CODE_UNCHECKED);
        alertRecordStorage.addAlertRecord(alertRecord);

        //
        // notification
        //
        notification.setLastAlertAt(lastAlertAt == null ? null : lastAlertAt.getTime());
        notification.setAlertRecordId(alertRecord.getRecordId());
        for (String name : alertRule.getNotifications()) {
            try {
                notificationApi.notify(name, notification);
            } catch (Exception e) {
                log.error("Exception when notifying " + name, e);
            }
        }
    }
}
