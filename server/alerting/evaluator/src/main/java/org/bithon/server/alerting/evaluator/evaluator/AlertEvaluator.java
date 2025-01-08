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
import com.google.common.annotations.VisibleForTesting;
import lombok.Getter;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.HumanReadableDuration;
import org.bithon.component.commons.utils.NetworkUtils;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.alerting.common.evaluator.EvaluationContext;
import org.bithon.server.alerting.common.evaluator.result.EvaluationOutputs;
import org.bithon.server.alerting.common.model.AlertExpression;
import org.bithon.server.alerting.common.model.AlertRule;
import org.bithon.server.alerting.evaluator.repository.AlertRepository;
import org.bithon.server.alerting.evaluator.repository.IAlertChangeListener;
import org.bithon.server.alerting.evaluator.state.IEvaluationStateManager;
import org.bithon.server.alerting.notification.message.ExpressionEvaluationResult;
import org.bithon.server.alerting.notification.message.NotificationMessage;
import org.bithon.server.alerting.notification.message.OutputMessage;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.alerting.IAlertRecordStorage;
import org.bithon.server.storage.alerting.IEvaluationLogWriter;
import org.bithon.server.storage.alerting.Label;
import org.bithon.server.storage.alerting.pojo.AlertRecordObject;
import org.bithon.server.storage.alerting.pojo.AlertStateObject;
import org.bithon.server.storage.alerting.pojo.AlertStatus;
import org.bithon.server.storage.alerting.pojo.EvaluationLogEvent;
import org.bithon.server.web.service.datasource.api.IDataSourceApi;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.web.ServerProperties;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/11 10:40 上午
 */
public class AlertEvaluator implements DisposableBean {

    @Getter
    private final IEvaluationStateManager stateManager;
    private final IEvaluationLogWriter evaluationLogWriter;
    private final IAlertRecordStorage alertRecordStorage;
    private final ObjectMapper objectMapper;
    private final IDataSourceApi dataSourceApi;
    private final INotificationApiInvoker notificationApiInvoker;

    public AlertEvaluator(AlertRepository repository,
                          IEvaluationStateManager stateManager,
                          IEvaluationLogWriter evaluationLogWriter,
                          IAlertRecordStorage recordStorage,
                          IDataSourceApi dataSourceApi,
                          ServerProperties serverProperties,
                          INotificationApiInvoker notificationApiInvoker,
                          ObjectMapper objectMapper) {
        this.stateManager = stateManager;
        this.alertRecordStorage = recordStorage;
        this.dataSourceApi = dataSourceApi;
        this.evaluationLogWriter = evaluationLogWriter;
        this.evaluationLogWriter.setInstance(NetworkUtils.getIpAddress().getHostAddress() + ":" + serverProperties.getPort());

        // Use Indent output for better debugging
        // It's a copy of existing ObjectMapper
        // because the injected ObjectMapper has extra serialization/deserialization configurations
        this.objectMapper = objectMapper.copy().enable(SerializationFeature.INDENT_OUTPUT);
        this.notificationApiInvoker = notificationApiInvoker;

        if (repository != null) {
            repository.addListener(new IAlertChangeListener() {
                @Override
                public void onLoaded(AlertRule rule) {
                    evaluationLogWriter.write(EvaluationLogEvent.builder()
                                                                .timestamp(new Timestamp(System.currentTimeMillis()))
                                                                .alertId(rule.getId())
                                                                .clazz(AlertEvaluator.class.getName())
                                                                .level("INFO")
                                                                .message(StringUtils.format("Loaded rule: [%s], Enabled: %s, Expr: %s",
                                                                                            rule.getName(),
                                                                                            Boolean.toString(rule.isEnabled()),
                                                                                            rule.getExpr()))
                                                                .build());
                }

                @Override
                public void onUpdated(AlertRule original, AlertRule updated) {
                    evaluationLogWriter.write(EvaluationLogEvent.builder()
                                                                .timestamp(new Timestamp(System.currentTimeMillis()))
                                                                .alertId(updated.getId())
                                                                .clazz(AlertEvaluator.class.getName())
                                                                .level("INFO")
                                                                .message(StringUtils.format("Updated rule [%s], Enabled: %s, Expr: %s",
                                                                                            updated.getName(),
                                                                                            Boolean.toString(updated.isEnabled()),
                                                                                            updated.getExpr()))
                                                                .build());
                }

                @Override
                public void onRemoved(AlertRule rule) {
                    evaluationLogWriter.write(EvaluationLogEvent.builder()
                                                                .timestamp(new Timestamp(System.currentTimeMillis()))
                                                                .alertId(rule.getId())
                                                                .clazz(AlertEvaluator.class.getName())
                                                                .level("INFO")
                                                                .message(StringUtils.format("Deleted rule [%s], Expr: %s", rule.getName(), rule.getExpr()))
                                                                .build());
                }
            });
        }
    }

    public void evaluate(TimeSpan now, AlertRule rule) {
        Map<String, AlertStateObject> state = this.getStateManager().exportAlertStates();
        this.evaluate(now, rule, state == null ? null : state.get(rule.getId()), false);
    }

    /**
     * @param prevState can be null
     */
    @VisibleForTesting
    void evaluate(TimeSpan now, AlertRule alertRule, AlertStateObject prevState) {
        evaluate(now, alertRule, prevState, false);
    }

    @VisibleForTesting
    void evaluate(TimeSpan now, AlertRule alertRule, AlertStateObject prevState, boolean skipPrecheck) {
        EvaluationContext context = new EvaluationContext(now,
                                                          evaluationLogWriter,
                                                          alertRule,
                                                          dataSourceApi,
                                                          prevState);

        Duration interval = alertRule.getEvery().getDuration();
        try {
            if (!skipPrecheck) {
                if (!alertRule.isEnabled()) {
                    context.log(AlertEvaluator.class, "Alert is disabled. Evaluation is skipped.");
                    return;
                }

                TimeSpan lastEvaluationAt = TimeSpan.of(this.stateManager.getEvaluationTimestamp(alertRule.getId()));
                if (now.diff(lastEvaluationAt) < interval.toMillis()) {
                    context.log(AlertEvaluator.class,
                                "Evaluation skipped, it's expected to be evaluated at %s",
                                lastEvaluationAt.after(interval).format("HH:mm"));
                    return;
                }
            }

            // Evaluate the alert rule
            Map<Label, AlertStatus> allNewStatus = evaluate(context, context.getPrevState());

            // TODO: Find notifications to send
            Map<Label, AlertStatus> notificationStatus = new HashMap<>();
            for (Map.Entry<Label, AlertStatus> entry : allNewStatus.entrySet()) {
                Label label = entry.getKey();
                AlertStatus newStatus = entry.getValue();
                AlertStatus prevStatus = context.getPrevState() == null ? AlertStatus.READY : context.getPrevState().getStatusByLabel(label);

                if (prevStatus.canTransitTo(newStatus)) {
                    context.log(AlertEvaluator.class, "Update alert status: [%s] ---> [%s]", prevStatus, newStatus);

                    if (newStatus == AlertStatus.ALERTING) {
                        notificationStatus.put(label, newStatus);
                        allNewStatus.put(label, newStatus);
                    } else if ((prevStatus == AlertStatus.ALERTING || prevStatus == AlertStatus.SUPPRESSING) && newStatus == AlertStatus.RESOLVED) {
                        notificationStatus.put(label, newStatus);
                        allNewStatus.put(label, newStatus);
                    } else {
                        // Update alert status only
                        //this.alertRecordStorage.updateAlertStatus(alertRule.getId(), context.getPrevState(), newStatus);
                        allNewStatus.put(label, newStatus);
                    }
                } else {
                    allNewStatus.put(label, prevStatus);
                    context.log(AlertEvaluator.class, "Stay in alert status: [%s]", prevStatus);
                }
            }

            // Update state storage
            //this.stateStorage.setAlertStates(alertRule.getId(), allNewStatus);
            this.alertRecordStorage.updateAlertStatus(alertRule.getId(),
                                                      context.getPrevState(),
                                                      getStatus(allNewStatus),
                                                      allNewStatus);

            // Group by alert status
            Map<AlertStatus, Map<Label, AlertStatus>> groupedStatus = notificationStatus.entrySet()
                                                                                        .stream()
                                                                                        .collect(HashMap::new,
                                                                                                 (map, entry) -> map.computeIfAbsent(entry.getValue(), (k) -> new HashMap<>())
                                                                                                                    .put(entry.getKey(), entry.getValue()),
                                                                                                 HashMap::putAll);
            fireAlert(alertRule, groupedStatus.get(AlertStatus.ALERTING), context);
            resolveAlert(alertRule, groupedStatus.get(AlertStatus.RESOLVED), context);

        } catch (Exception e) {
            context.logException(AlertEvaluator.class, e, "ERROR to evaluate alert %s", alertRule.getName());
        }

        this.stateManager.setEvaluationTime(alertRule.getId(), now.getMilliseconds(), interval);
    }

    private AlertStatus getStatus(Map<Label, AlertStatus> status) {
        if (status.isEmpty()) {
            return AlertStatus.READY;
        }

        int isResolved = 0;
        boolean hasSuppressing = false;
        boolean hasPending = false;
        for (AlertStatus alertStatus : status.values()) {
            if (alertStatus == AlertStatus.ALERTING) {
                return AlertStatus.ALERTING;
            }
            if (alertStatus == AlertStatus.RESOLVED) {
                isResolved++;
            }
            if (alertStatus == AlertStatus.SUPPRESSING) {
                hasSuppressing = true;
            }
            if (alertStatus == AlertStatus.PENDING) {
                hasPending = true;
            }
        }
        if (hasSuppressing) {
            return AlertStatus.SUPPRESSING;
        }
        if (hasPending) {
            return AlertStatus.PENDING;
        }
        if (isResolved == status.size()) {
            return AlertStatus.RESOLVED;
        }
        return AlertStatus.READY;
    }

    private Map<Label, AlertStatus> evaluate(EvaluationContext context, AlertStateObject prevState) {
        AlertRule alertRule = context.getAlertRule();
        context.log(AlertEvaluator.class, "Evaluating rule [%s]: %s ", alertRule.getName(), alertRule.getExpr());

        AlertExpressionEvaluator expressionEvaluator = new AlertExpressionEvaluator(alertRule.getAlertExpression());
        if (!expressionEvaluator.evaluate(context)) {
            context.log(AlertEvaluator.class,
                        "Rule [%s] evaluated as FALSE",
                        alertRule.getName());

            stateManager.resetMatchCount(alertRule.getId());

            Map<Label, AlertStatus> newStatus = new HashMap<>();
            if (prevState != null) {
                for (Map.Entry<Label, AlertStateObject.StatePerLabel> item : prevState.getPayload().getStates().entrySet()) {
                    newStatus.put(item.getKey(), AlertStatus.RESOLVED);
                }
            }
            return newStatus;
        }

        context.log(AlertEvaluator.class, "Rule [%s] evaluated as TRUE", alertRule.getName());

        long expectedMatchCount = alertRule.getExpectedMatchCount();

        Map<Label, AlertStatus> alertStatus = new HashMap<>();
        Map<Label, Long> successiveCountList = stateManager.incrMatchCount(alertRule.getId(),
                                                                           context.getGroups(),
                                                                           alertRule.getEvery()
                                                                                    .getDuration()
                                                                                    // Add 30 seconds for margin
                                                                                    .plus(Duration.ofSeconds(30)));
        for (Map.Entry<Label, Long> entry : successiveCountList.entrySet()) {
            Label label = entry.getKey();
            long successiveCount = entry.getValue();

            AlertStatus newStatus = getAlertStatus(context, label, prevState, successiveCount, expectedMatchCount);

            alertStatus.put(label, newStatus);
        }

        return alertStatus;
    }

    private AlertStatus getAlertStatus(EvaluationContext context,
                                       Label label,
                                       AlertStateObject prevState,
                                       long successiveCount,
                                       long expectedMatchCount) {
        AlertRule alertRule = context.getAlertRule();

        if (successiveCount >= expectedMatchCount) {
            stateManager.resetMatchCount(alertRule.getId());

            context.log(AlertEvaluator.class,
                        "Rule [%s] evaluated as TRUE for [%d] times successively，and reaches the expected threshold [%d] to fire alert",
                        alertRule.getName(),
                        successiveCount,
                        expectedMatchCount);

            HumanReadableDuration silenceDuration = context.getAlertRule().getNotificationProps().getSilence();

            String lastAlertingAt = prevState == null ? "N/A" : TimeSpan.of(Timestamp.valueOf(prevState.getLastAlertAt()).getTime()).format("HH:mm:ss");

            // Calc the silence period by adding some margin
            // Let's say current timestamp is 10:01:02.123, and the silence period is 1 minute,
            // The silence period is [now(), 10:02:00.000(assuming the evaluation execution finishes within 1 minute) + 1 minute]
            TimeSpan now = TimeSpan.now();
            TimeSpan endOfThisMinute = now.ceil(Duration.ofMinutes(1));
            Duration silencePeriod = silenceDuration.getDuration().plus(Duration.ofMillis(endOfThisMinute.diff(now)));

            if (silenceDuration.getDuration().getSeconds() > 0 && stateManager.tryEnterSilence(alertRule.getId(), label, silencePeriod)) {
                Duration silenceRemainTime = stateManager.getSilenceRemainTime(alertRule.getId(), label);
                context.log(AlertEvaluator.class, "Alerting，but is under notification silence duration (%s) from last alerting timestamp %s to %s.",
                            silenceDuration,
                            lastAlertingAt,
                            TimeSpan.of(System.currentTimeMillis() + silenceRemainTime.toMillis()).format("HH:mm:ss"));
                return AlertStatus.SUPPRESSING;
            } else {
                context.log(AlertEvaluator.class,
                            "Alerting，silence period(%s) is over. Last alert at: %s",
                            silenceDuration,
                            lastAlertingAt);
            }

            return AlertStatus.ALERTING;
        } else {
            context.log(AlertEvaluator.class,
                        "Rule [%s] evaluated as TRUE for [%d] times successively，NOT reach the expected threshold [%s] to fire alert",
                        alertRule.getName(),
                        successiveCount,
                        expectedMatchCount);

            return AlertStatus.PENDING;
        }
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
        notification.setExpressions(alertRule.getFlattenExpressions().values());
        notification.setConditionEvaluation(new HashMap<>());
        context.getEvaluationStatus().forEach((expressionId, result) -> {
            AlertExpression expression = context.getAlertExpressions().get(expressionId);

            EvaluationOutputs outputs = context.getRuleEvaluationOutputs(expressionId);
            notification.getConditionEvaluation()
                        .put(expression.getId(),
                             new ExpressionEvaluationResult(result,
                                                            outputs == null ? null : outputs.stream().map((output) -> OutputMessage.builder()
                                                                                                                                   .current(output.getCurrentText())
                                                                                                                                   .delta(output.getDeltaText())
                                                                                                                                   .threshold(output.getThresholdText())
                                                                                                                                   .build()).toList()));
        });

        Timestamp alertAt = new Timestamp(System.currentTimeMillis());
        try {
            // Save alerting records
            context.log(AlertEvaluator.class, "Saving alert record");
            String id = saveAlertRecord(context, alertAt, notification);

            // notification
            notification.setLastAlertAt(alertAt.getTime());
            notification.setAlertRecordId(id);
            for (String channelName : alertRule.getNotificationProps().getChannels()) {
                context.log(AlertEvaluator.class, "Sending alerting notification to channel [%s]", channelName);

                try {
                    notificationApiInvoker.notify(channelName, notification);
                } catch (Exception e) {
                    context.logException(AlertEvaluator.class, e, "Exception when sending notification to channel [%s]", channelName);
                }
            }
        } catch (Exception e) {
            context.logException(AlertEvaluator.class, e, "Exception when sending notification");
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
        notification.setExpressions(alertRule.getFlattenExpressions().values());
        notification.setConditionEvaluation(new HashMap<>());
        context.getEvaluationStatus().forEach((expressionId, result) -> {
            AlertExpression expression = context.getAlertExpressions().get(expressionId);

            EvaluationOutputs outputs = context.getRuleEvaluationOutputs(expressionId);
            notification.getConditionEvaluation()
                        .put(expression.getId(),
                             new ExpressionEvaluationResult(result,
                                                            outputs == null ? null : outputs.stream()
                                                                                            .map((output) -> OutputMessage.builder()
                                                                                                                          .current(output.getCurrentText())
                                                                                                                          .delta(output.getDeltaText())
                                                                                                                          .threshold(output.getThresholdText())
                                                                                                                          .build())
                                                                                            .toList()));
        });

        Timestamp alertAt = new Timestamp(System.currentTimeMillis());
        try {
            // notification
            notification.setLastAlertAt(alertAt.getTime());
            notification.setAlertRecordId(context.getPrevState().getLastRecordId());
            for (String channelName : alertRule.getNotificationProps().getChannels()) {
                context.log(AlertEvaluator.class, "Sending RESOLVED notification to channel [%s]", channelName);

                try {
                    notificationApiInvoker.notify(channelName, notification);
                } catch (Exception e) {
                    context.logException(AlertEvaluator.class, e, "Exception when sending notification to channel [%s]", channelName);
                }
            }
        } catch (Exception e) {
            context.logException(AlertEvaluator.class, e, "Exception when sending RESOLVED notification");
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

        long startOfThisEvaluation = context.getEvaluationOutputs()
                                            .values()
                                            .stream()
                                            .flatMap(Collection::stream)
                                            .map((output) -> output.getStart().getMilliseconds())
                                            .min(Comparator.comparingLong((v) -> v))
                                            .get();

        // Calculate the first time the rule is tested as TRUE
        // Since the current interval is TRUE, there were (n - 1) intervals before this interval
        long startInclusive = startOfThisEvaluation - (context.getAlertRule().getForTimes() - 1) * context.getAlertRule().getEvery().getDuration().toMillis();
        long endInclusive = context.getIntervalEnd().getMilliseconds() - context.getAlertRule().getEvery().getDuration().toMillis();

        alertRecord.setPayload(objectMapper.writeValueAsString(AlertRecordPayload.builder()
                                                                                 .start(startInclusive)
                                                                                 .end(endInclusive)
                                                                                 .expressions(context.getAlertExpressions().values())
                                                                                 .conditionEvaluation(notification.getConditionEvaluation())
                                                                                 .build()));
        alertRecord.setNotificationStatus(IAlertRecordStorage.STATUS_CODE_UNCHECKED);
        alertRecordStorage.addAlertRecord(alertRecord);
        return alertRecord.getRecordId();
    }

    @Override
    public void destroy() {
        this.evaluationLogWriter.close();
    }
}
