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


import com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.server.alerting.common.evaluator.EvaluationContext;
import org.bithon.server.alerting.common.evaluator.result.IEvaluationOutput;
import org.bithon.server.alerting.common.model.AlertRule;
import org.bithon.server.alerting.evaluator.evaluator.AlertRecordPayload;
import org.bithon.server.alerting.evaluator.evaluator.INotificationApiInvoker;
import org.bithon.server.alerting.evaluator.state.IEvaluationStateManager;
import org.bithon.server.alerting.notification.message.NotificationMessage;
import org.bithon.server.storage.alerting.IAlertRecordStorage;
import org.bithon.server.storage.alerting.Label;
import org.bithon.server.storage.alerting.pojo.AlertRecordObject;
import org.bithon.server.storage.alerting.pojo.AlertStatus;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author frank.chen021@outlook.com
 * @date 18/3/25 9:47 am
 */
public class NotificationStep implements IPipelineStep {
    private final IAlertRecordStorage alertRecordStorage;
    private final INotificationApiInvoker notificationApiInvoker;
    private final ObjectMapper objectMapper;

    public NotificationStep(IAlertRecordStorage alertRecordStorage,
                            INotificationApiInvoker notificationApiInvoker,
                            ObjectMapper objectMapper) {
        this.alertRecordStorage = alertRecordStorage;
        this.notificationApiInvoker = notificationApiInvoker;
        this.objectMapper = objectMapper;
    }

    @Override
    public void evaluate(IEvaluationStateManager stateManager, EvaluationContext context) {
        // TODO: Find notifications to send
        Map<Label, AlertStatus> notificationStatus = new HashMap<>();
        for (Map.Entry<Label, AlertStatus> entry : context.getSeriesStatus().entrySet()) {
            Label label = entry.getKey();
            AlertStatus newStatus = entry.getValue();
            AlertStatus prevStatus = context.getPrevState() == null ? AlertStatus.READY : context.getPrevState().getStatusByLabel(label);

            if (prevStatus.canTransitTo(newStatus)) {
                context.log(NotificationStep.class, "Update alert status%s: [%s] ---> [%s]",
                            label.formatIfNotEmpty(" for series {%s}"),
                            prevStatus,
                            newStatus);

                if (newStatus == AlertStatus.ALERTING) {
                    notificationStatus.put(label, newStatus);
                    context.getSeriesStatus().put(label, newStatus);
                } else if ((prevStatus == AlertStatus.ALERTING || prevStatus == AlertStatus.SUPPRESSING) && newStatus == AlertStatus.RESOLVED) {
                    notificationStatus.put(label, newStatus);
                    context.getSeriesStatus().put(label, newStatus);
                } else {
                    context.getSeriesStatus().put(label, newStatus);
                }
            } else {
                context.getSeriesStatus().put(label, prevStatus);
                context.log(NotificationStep.class, "%sstay in alert status: [%s]", label.formatIfNotEmpty("Series {%s} "), prevStatus);
            }
        }

        // Group series with different labels by alert status
        Map<AlertStatus, Map<Label, AlertStatus>> groupedStatus = notificationStatus.entrySet()
                                                                                    .stream()
                                                                                    .collect(HashMap::new,
                                                                                             (map, entry) -> map.computeIfAbsent(entry.getValue(), (k) -> new HashMap<>())
                                                                                                                .put(entry.getKey(), entry.getValue()),
                                                                                             HashMap::putAll);
        fireAlert(context.getAlertRule(), groupedStatus.get(AlertStatus.ALERTING), context);
        resolveAlert(context.getAlertRule(), groupedStatus.get(AlertStatus.RESOLVED), context);
    }

    /**
     * Fire alert and update its status
     */
    private void fireAlert(AlertRule alertRule, Map<Label, AlertStatus> labels, EvaluationContext context) {
        if (CollectionUtils.isEmpty(labels)) {
            return;
        }

        // Prepare notification
        NotificationMessage notification = new NotificationMessage();
        notification.setEndTimestamp(context.getIntervalEnd().getMilliseconds());
        notification.setAlertRule(alertRule);
        notification.setStatus(AlertStatus.ALERTING);
        notification.setExpressions(alertRule.getFlattenExpressions());
        notification.setEvaluationResult(context.getEvaluationResult());

        Timestamp alertAt = new Timestamp(System.currentTimeMillis());
        try {
            // Save alerting records
            context.log(NotificationStep.class, "Saving alert record");
            String id = saveAlertRecord(context, alertAt, notification);

            // notification
            notification.setLastAlertAt(alertAt.getTime());
            notification.setAlertRecordId(id);
            for (String channelName : alertRule.getNotificationProps().getChannels()) {
                context.log(NotificationStep.class, "Sending alerting notification to channel [%s]", channelName);

                try {
                    notificationApiInvoker.notify(channelName, notification);
                } catch (Exception e) {
                    context.logException(NotificationStep.class, e, "Exception when sending notification to channel [%s]", channelName);
                }
            }
        } catch (Exception e) {
            context.logException(NotificationStep.class, e, "Exception when sending notification");
        }
    }

    /**
     * Fire alert and update its status
     */
    private void resolveAlert(AlertRule alertRule, Map<Label, AlertStatus> labels, EvaluationContext context) {
        if (CollectionUtils.isEmpty(labels)) {
            return;
        }

        // Prepare notification
        NotificationMessage notification = new NotificationMessage();
        notification.setStatus(AlertStatus.RESOLVED);
        notification.setAlertRule(alertRule);
        notification.setExpressions(alertRule.getFlattenExpressions());
        notification.setEvaluationResult(context.getEvaluationResult());

        Timestamp alertAt = new Timestamp(System.currentTimeMillis());
        try {
            // notification
            notification.setLastAlertAt(alertAt.getTime());
            notification.setAlertRecordId(context.getPrevState().getLastRecordId());
            for (String channelName : alertRule.getNotificationProps().getChannels()) {
                context.log(NotificationStep.class, "Sending RESOLVED notification to channel [%s]", channelName);

                try {
                    notificationApiInvoker.notify(channelName, notification);
                } catch (Exception e) {
                    context.logException(NotificationStep.class, e, "Exception when sending notification to channel [%s]", channelName);
                }
            }
        } catch (Exception e) {
            context.logException(NotificationStep.class, e, "Exception when sending RESOLVED notification");
        }
    }

    private String saveAlertRecord(EvaluationContext context, Timestamp lastAlertAt, NotificationMessage notification) throws IOException {
        AlertRecordObject alertRecord = new AlertRecordObject();
        alertRecord.setRecordId(UUID.randomUUID().toString().replace("-", ""));
        alertRecord.setAlertId(context.getAlertRule().getId());
        alertRecord.setAlertName(context.getAlertRule().getName());
        alertRecord.setAppName(context.getAlertRule().getAppName());
        alertRecord.setNamespace("");
        alertRecord.setDataSource("{}");
        alertRecord.setCreatedAt(lastAlertAt);

        long startOfThisEvaluation = context.getOutputs()
                                            .stream()
                                            .map(IEvaluationOutput::getStart)
                                            .min(Comparator.comparingLong((v) -> v))
                                            .get();

        // Calculate the first time the rule is tested as TRUE
        // Since the current interval is TRUE, there were (n - 1) intervals before this interval
        long startInclusive = startOfThisEvaluation - (context.getAlertRule().getForTimes() - 1) * context.getAlertRule().getEvery().getDuration().toMillis();
        long endInclusive = context.getIntervalEnd().getMilliseconds() - context.getAlertRule().getEvery().getDuration().toMillis();

        alertRecord.setPayload(objectMapper.writeValueAsString(AlertRecordPayload.builder()
                                                                                 .start(startInclusive)
                                                                                 .end(endInclusive)
                                                                                 .expressions(context.getAlertRule().getFlattenExpressions().values())
                                                                                 .conditionEvaluation(notification.getEvaluationResult())
                                                                                 .build()));
        alertRecord.setNotificationStatus(IAlertRecordStorage.STATUS_CODE_UNCHECKED);
        alertRecordStorage.addAlertRecord(alertRecord);
        return alertRecord.getRecordId();
    }
}
