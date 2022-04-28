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

package org.bithon.server.alerting.processor.evaluation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.alerting.common.AlertingModule;
import org.bithon.server.alerting.common.evaluator.EvaluatorContext;
import org.bithon.server.alerting.common.evaluator.result.EvaluationResult;
import org.bithon.server.alerting.common.evaluator.result.IEvaluationOutput;
import org.bithon.server.alerting.common.model.Alert;
import org.bithon.server.alerting.common.model.AlertCompositeConditions;
import org.bithon.server.alerting.common.model.AlertCondition;
import org.bithon.server.alerting.common.notification.message.ConditionEvaluationResult;
import org.bithon.server.alerting.common.notification.message.NotificationMessage;
import org.bithon.server.alerting.common.notification.message.OutputMessage;
import org.bithon.server.alerting.common.notification.message.RuleMessage;
import org.bithon.server.alerting.common.notification.provider.INotificationProvider;
import org.bithon.server.alerting.processor.notification.NotificationConfig;
import org.bithon.server.alerting.processor.service.AlertImageRenderService;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.alerting.IAlertRecordStorage;
import org.bithon.server.storage.alerting.IAlertStateStorage;
import org.bithon.server.storage.alerting.IEvaluationLogStorage;
import org.bithon.server.storage.alerting.pojo.AlertRecordObject;
import org.bithon.server.web.service.api.IDataSourceApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/11 10:40 上午
 */
@Slf4j
@Service
@ConditionalOnBean(AlertingModule.class)
public class AlertEvaluator {

    private final IAlertStateStorage stateStorage;
    private final IEvaluationLogStorage evaluationLoggerFactory;
    private final NotificationConfig notificationConfig;
    private final IAlertRecordStorage alertRecordStorage;
    private final ObjectMapper objectMapper;
    private final AlertImageRenderService imageRenderService;
    private final IDataSourceApi dataSourceApi;

    public AlertEvaluator(IAlertStateStorage stateStorage,
                          IEvaluationLogStorage evaluationLoggerFactory,
                          NotificationConfig notificationConfig,
                          IAlertRecordStorage alertRecordDao,
                          AlertImageRenderService imageRenderService,
                          IDataSourceApi dataSourceApi) {
        this.stateStorage = stateStorage;
        this.evaluationLoggerFactory = evaluationLoggerFactory;
        this.notificationConfig = notificationConfig;
        this.alertRecordStorage = alertRecordDao;
        this.imageRenderService = imageRenderService;
        this.dataSourceApi = dataSourceApi;
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    public void evaluate(TimeSpan now, Alert alert) {
        EvaluatorContext context = new EvaluatorContext(now,
                                                        evaluationLoggerFactory.createWriter(),
                                                        alert,
                                                        dataSourceApi);
        try {
            if (!alert.isEnabled()) {
                context.log(AlertEvaluator.class, "Alert is disabled. Evaluation is skipped.");
                return;
            }
            if (evaluate(context)) {
                notify(alert, context);
            }
        } catch (Exception e) {
            log.error(StringUtils.format("ERROR to evaluate alert %s", alert.getName()), e);
        } finally {
            try {
                context.getEvaluatorLogger().flush();
            } catch (Exception e) {
                log.error("Flush log for alert {} failed: {}", alert.getId(), e.toString());
            }
        }
    }

    private boolean evaluate(EvaluatorContext context) {
        long waitMinute = context.getIntervalEnd().getMilliseconds() % (context.getAlert().getEvaluationInterval() * 60L);
        if (waitMinute != 0) {
            context.log(AlertEvaluator.class,
                        "Alert not evaluated because it's not the right time. Will evaluate this alert at [%s]",
                        context.getIntervalEnd().after(waitMinute, TimeUnit.MINUTES).toString("HH:mm"));
            return false;
        }

        context.log(AlertEvaluator.class, "Evaluating alert");
        Alert alert = context.getAlert();
        for (AlertCompositeConditions rules : alert.getRules()) {
            context.log(AlertEvaluator.class,
                        "Evaluating rule [%s]", rules.getExpression());

            try {
                if (rules.getEvaluator().test(context)) {
                    context.log(AlertEvaluator.class, "Rule [%s] tested successfully.", rules.getExpression());

                    long successiveCount = stateStorage.incrTriggerMatchCount(alert.getId(), rules.getExpression());
                    if (successiveCount >= alert.getMatchTimes()) {
                        stateStorage.resetTriggerMatchCount(alert.getId(), rules.getExpression());

                        context.log(AlertEvaluator.class,
                                    "Rule tested %d times successively，and reaches the expected count：%d",
                                    successiveCount,
                                    alert.getMatchTimes());

                        // merge triggered alerts together for one app
                        context.addMatchedTrigger(rules);
                    } else {
                        context.log(AlertEvaluator.class, "Rule tested %d times successively，expected times：%d", successiveCount, alert.getMatchTimes());
                    }
                } else {
                    stateStorage.resetTriggerMatchCount(alert.getId(), rules.getExpression());
                }
            } catch (Exception e) {
                context.logException(AlertEvaluator.class,
                                     e,
                                     "Evaluate rule [%s] exception: %s",
                                     rules.getExpression(),
                                     e.getMessage());
            }
        }

        return !context.getMatchedRules().isEmpty();
    }

    private void notify(Alert alert, EvaluatorContext context) throws Exception {
        long now = System.currentTimeMillis();
        int silencePeriod = context.getAlert().getAlertEveryNMinutes();
        if (stateStorage.tryEnterSilence(alert.getId(), silencePeriod)) {
            long silenceRemainTime = stateStorage.getSilenceRemainTime(alert.getId());
            context.log(AlertEvaluator.class, "Alerting，but is under silence period(%d minutes)。Last alert at:%s",
                        silencePeriod,
                        TimeSpan.of(now - (TimeUnit.MILLISECONDS.convert(silencePeriod, TimeUnit.MINUTES) - silenceRemainTime)).toISO8601());
            return;
        }

        context.log(AlertEvaluator.class, "Sending alert");

        NotificationMessage notification = new NotificationMessage();
        notification.setAlert(alert);
        notification.setStart(context.getIntervalEnd().before(alert.getMatchTimes(), TimeUnit.MINUTES).getMilliseconds());
        notification.setEnd(context.getIntervalEnd().getMilliseconds());
        notification.setDetectionLength(alert.getMatchTimes());
        notification.setConditions(alert.getConditions());
        notification.setRules(alert.getRules().stream().map(trigger -> {
            RuleMessage message = new RuleMessage();
            message.setExpression(trigger.getExpression());
            message.setTriggered(context.getMatchedRules().contains(trigger));
            message.setSeverity(trigger.getSeverity());
            message.setConditions(trigger.getConditions());
            return message;
        }).collect(Collectors.toList()));

        notification.setConditionEvaluation(new HashMap<>(alert.getConditions().size()));

        if (!this.notificationConfig.getRenderConfig().getDisabled()) {
            notification.setImages(new HashMap<>(alert.getConditions().size()));
        }
        context.getEvaluationResults().forEach((conditionId, result) -> {
            AlertCondition condition = alert.getAlertConditionById(conditionId);

            IEvaluationOutput outputs = context.getConditionEvaluationOutput(conditionId);
            notification.getConditionEvaluation()
                        .put(condition.getId(),
                             new ConditionEvaluationResult(result,
                                                           outputs == null ? null : OutputMessage.builder()
                                                                                                 .current(outputs.getCurrentText())
                                                                                                 .delta(outputs.getDeltaText())
                                                                                                 .threshold(outputs.getThresholdText())
                                                                                                 .build()));

            if (result == EvaluationResult.MATCHED && !this.notificationConfig.getRenderConfig().getDisabled()) {
                /*
                notification.getImages().put(conditionId,
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
        Timestamp lastAlertAt = alertRecordStorage.getLastAlert(alert.getId());
        AlertRecordObject alertRecord = new AlertRecordObject();
        alertRecord.setRecordId(UUID.randomUUID().toString().replace("-", ""));
        alertRecord.setAlertId(alert.getId());
        alertRecord.setAlertName(alert.getName());
        alertRecord.setAppName(alert.getAppName());
        alertRecord.setNamespace("");
        alertRecord.setPayload(objectMapper.writeValueAsString(AlertRecordPayload.builder()
                                                                                 .start(notification.getStart())
                                                                                 .end(notification.getEnd())
                                                                                 .conditions(alert.getConditions())
                                                                                 .conditionEvaluation(notification.getConditionEvaluation())
                                                                                 .detectionLength(notification.getDetectionLength())
                                                                                 .rules(notification.getRules())
                                                                                 .build()));
        alertRecord.setNotificationStatus(IAlertRecordStorage.STATUS_CODE_UNCHECKED);
        alertRecordStorage.addAlertRecord(alertRecord);

        //
        // notification
        //
        notification.setLastAlertAt(lastAlertAt == null ? null : lastAlertAt.getTime());
        notification.setAlertRecordId(alertRecord.getRecordId());
        for (INotificationProvider notificationProvider : alert.getNotifications()) {
            try {
                notificationProvider.notify(context.getEvaluatorLogger(), notification);
            } catch (Exception e) {
                log.error("Exception when notifying " + notificationProvider, e);
            }
        }
    }
}
