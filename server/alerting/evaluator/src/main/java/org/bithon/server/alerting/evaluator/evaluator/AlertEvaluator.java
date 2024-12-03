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
import feign.Contract;
import feign.Feign;
import feign.codec.Decoder;
import feign.codec.Encoder;
import org.bithon.component.commons.utils.HumanReadableDuration;
import org.bithon.component.commons.utils.NetworkUtils;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.alerting.common.evaluator.EvaluationContext;
import org.bithon.server.alerting.common.evaluator.result.IEvaluationOutput;
import org.bithon.server.alerting.common.model.AlertExpression;
import org.bithon.server.alerting.common.model.AlertRule;
import org.bithon.server.alerting.evaluator.repository.AlertRepository;
import org.bithon.server.alerting.evaluator.repository.IAlertChangeListener;
import org.bithon.server.alerting.notification.api.INotificationApi;
import org.bithon.server.alerting.notification.message.ExpressionEvaluationResult;
import org.bithon.server.alerting.notification.message.NotificationMessage;
import org.bithon.server.alerting.notification.message.OutputMessage;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.discovery.client.DiscoveredServiceInvoker;
import org.bithon.server.storage.alerting.IAlertRecordStorage;
import org.bithon.server.storage.alerting.IAlertStateStorage;
import org.bithon.server.storage.alerting.IEvaluationLogWriter;
import org.bithon.server.storage.alerting.pojo.AlertRecordObject;
import org.bithon.server.storage.alerting.pojo.AlertStateObject;
import org.bithon.server.storage.alerting.pojo.AlertStatus;
import org.bithon.server.storage.alerting.pojo.EvaluationLogEvent;
import org.bithon.server.web.service.datasource.api.IDataSourceApi;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.UUID;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/11 10:40 上午
 */
public class AlertEvaluator implements DisposableBean {

    private final IAlertStateStorage stateStorage;
    private final IEvaluationLogWriter evaluationLogWriter;
    private final IAlertRecordStorage alertRecordStorage;
    private final ObjectMapper objectMapper;
    private final IDataSourceApi dataSourceApi;
    private final INotificationApi notificationApi;

    public AlertEvaluator(AlertRepository repository,
                          IAlertStateStorage stateStorage,
                          IEvaluationLogWriter evaluationLogWriter,
                          IAlertRecordStorage recordStorage,
                          IDataSourceApi dataSourceApi,
                          ServerProperties serverProperties,
                          ApplicationContext applicationContext,
                          ObjectMapper objectMapper) {
        this.stateStorage = stateStorage;
        this.alertRecordStorage = recordStorage;
        this.dataSourceApi = dataSourceApi;
        this.evaluationLogWriter = evaluationLogWriter;
        this.evaluationLogWriter.setInstance(NetworkUtils.getIpAddress().getHostAddress() + ":" + serverProperties.getPort());

        // Use Indent output for better debugging
        // It's a copy of existing ObjectMapper
        // because the injected ObjectMapper has extra serialization/deserialization configurations
        this.objectMapper = objectMapper.copy().enable(SerializationFeature.INDENT_OUTPUT);
        this.notificationApi = createNotificationApi(applicationContext);

        if (repository != null) {
            repository.addListener(new IAlertChangeListener() {
                @Override
                public void onLoaded(AlertRule rule) {
                    evaluationLogWriter.write(EvaluationLogEvent.builder()
                                                                .timestamp(new Timestamp(System.currentTimeMillis()))
                                                                .alertId(rule.getId())
                                                                .clazz(AlertEvaluator.class.getName())
                                                                .level("INFO")
                                                                .message(StringUtils.format("Loaded rule [%s]: [Enabled: %s] [Expr: %s]",
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
                                                                .message(StringUtils.format("Updated rule [%s]: [Enabled: %s] [Expr: %s]",
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
                                                                .message(StringUtils.format("Deleted rule [%s]: [%s]", rule.getName(), rule.getExpr()))
                                                                .build());
                }
            });
        }
    }

    /**
     * @param prevState can be null
     */
    public void evaluate(TimeSpan now, AlertRule alertRule, AlertStateObject prevState) {
        EvaluationContext context = new EvaluationContext(now,
                                                          evaluationLogWriter,
                                                          alertRule,
                                                          dataSourceApi,
                                                          prevState);

        Duration interval = alertRule.getEvery().getDuration();
        try {
            if (!alertRule.isEnabled()) {
                context.log(AlertEvaluator.class, "Alert is disabled. Evaluation is skipped.");
                return;
            }

            TimeSpan lastEvaluationAt = TimeSpan.of(this.stateStorage.getEvaluationTimestamp(alertRule.getId()));
            if (now.diff(lastEvaluationAt) < interval.toMillis()) {
                context.log(AlertEvaluator.class,
                            "Evaluation skipped, it's expected to be evaluated at %s",
                            lastEvaluationAt.after(interval).format("HH:mm"));
                return;
            }

            AlertStatus prevStatus = context.getPrevState() == null ? AlertStatus.READY : context.getPrevState().getStatus();

            // Evaluate the alert rule
            AlertStatus newStatus = evaluate(context, context.getPrevState());

            if (prevStatus.canTransitTo(newStatus)) {
                context.log(AlertEvaluator.class, "Update alert status: [%s] ---> [%s]", prevStatus, newStatus);

                if (newStatus == AlertStatus.ALERTING) {
                    // Fire and save alert record
                    fireAlert(alertRule, context);
                } else if ((prevStatus == AlertStatus.ALERTING || prevStatus == AlertStatus.SUPPRESSING) && newStatus == AlertStatus.RESOLVED) {
                    // Update alert status and resolve alert
                    this.alertRecordStorage.updateAlertStatus(alertRule.getId(), context.getPrevState(), newStatus);

                    this.resolveAlert(alertRule, context);
                } else {
                    // Update alert status only
                    this.alertRecordStorage.updateAlertStatus(alertRule.getId(), context.getPrevState(), newStatus);
                }
            } else {
                context.log(AlertEvaluator.class, "Stay in alert status: [%s]", prevStatus);
            }
        } catch (Exception e) {
            context.logException(AlertEvaluator.class, e, "ERROR to evaluate alert %s", alertRule.getName());
        }

        this.stateStorage.setEvaluationTime(alertRule.getId(), now.getMilliseconds(), interval);
    }

    private INotificationApi createNotificationApi(ApplicationContext context) {
        // The notification service is configured by auto-discovery
        String service = context.getBean(Environment.class).getProperty("bithon.alerting.evaluator.notification-service", "discovery");
        if ("discovery".equalsIgnoreCase(service)) {
            // Even the notification module is deployed with the evaluator module together,
            // we still call the notification module via HTTP instead of direct API method calls in process
            // So that it simulates the 'remote call' via discovered service
            DiscoveredServiceInvoker invoker = context.getBean(DiscoveredServiceInvoker.class);
            return invoker.createUnicastApi(INotificationApi.class);
        }

        // The service is configured as a remote service at fixed address
        // Create a feign client to call it
        if (service.startsWith("http:") || service.startsWith("https:")) {
            return Feign.builder()
                        .contract(context.getBean(Contract.class))
                        .encoder(context.getBean(Encoder.class))
                        .decoder(context.getBean(Decoder.class))
                        .target(INotificationApi.class, service);
        }

        throw new RuntimeException(StringUtils.format("Invalid notification property configured. Only 'discovery' or URL is allowed, but got [%s]", service));
    }

    private AlertStatus evaluate(EvaluationContext context, AlertStateObject prevState) {
        AlertRule alertRule = context.getAlertRule();
        context.log(AlertEvaluator.class, "Evaluating rule [%s]: %s ", alertRule.getName(), alertRule.getExpr());

        AlertExpressionEvaluator expressionEvaluator = new AlertExpressionEvaluator(alertRule.getAlertExpression());
        if (expressionEvaluator.evaluate(context)) {
            context.log(AlertEvaluator.class, "Rule [%s] evaluated as TRUE", alertRule.getName());

            long expectedMatchCount = alertRule.getExpectedMatchCount();
            long successiveCount = stateStorage.incrMatchCount(alertRule.getId(),
                                                               alertRule.getEvery()
                                                                        .getDuration()
                                                                        // Add 30 seconds for margin
                                                                        .plus(Duration.ofSeconds(30)));
            if (successiveCount >= expectedMatchCount) {
                stateStorage.resetMatchCount(alertRule.getId());

                context.log(AlertEvaluator.class,
                            "Rule [%s] evaluated as TRUE for [%d] times successively，and reaches the expected threshold [%d] to fire alert",
                            alertRule.getName(),
                            successiveCount,
                            expectedMatchCount);

                HumanReadableDuration silenceDuration = context.getAlertRule().getSilence();

                String lastAlertingAt = prevState == null ? "N/A" : TimeSpan.of(Timestamp.valueOf(prevState.getLastAlertAt()).getTime()).format("HH:mm:ss");

                // Calc the silence period by adding some margin
                // Let's say current timestamp is 10:01:02.123, and the silence period is 1 minute,
                // The silence period is [now(), 10:02:00.000(assuming the evaluation execution finishes within 1 minute) + 1 minute]
                TimeSpan now = TimeSpan.now();
                TimeSpan endOfThisMinute = now.ceil(Duration.ofMinutes(1));
                Duration silencePeriod = silenceDuration.getDuration().plus(Duration.ofMillis(endOfThisMinute.diff(now)));
                if (stateStorage.tryEnterSilence(alertRule.getId(), silencePeriod)) {
                    Duration silenceRemainTime = stateStorage.getSilenceRemainTime(alertRule.getId());
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
        } else {
            context.log(AlertEvaluator.class,
                        "Rule [%s] evaluated as FALSE",
                        alertRule.getName());

            stateStorage.resetMatchCount(alertRule.getId());
            return AlertStatus.RESOLVED;
        }
    }

    /**
     * Fire alert and update its status
     */
    private void fireAlert(AlertRule alertRule, EvaluationContext context) {
        // Prepare notification
        NotificationMessage notification = new NotificationMessage();
        notification.setAlertRule(alertRule);
        notification.setStatus(AlertStatus.ALERTING);
        notification.setExpressions(alertRule.getFlattenExpressions().values());
        notification.setConditionEvaluation(new HashMap<>());
        context.getEvaluationResults().forEach((expressionId, result) -> {
            AlertExpression expression = context.getAlertExpressions().get(expressionId);

            IEvaluationOutput outputs = context.getRuleEvaluationOutput(expressionId);
            notification.getConditionEvaluation()
                        .put(expression.getId(),
                             new ExpressionEvaluationResult(result,
                                                            outputs == null ? null : OutputMessage.builder()
                                                                                                  .current(outputs.getCurrentText())
                                                                                                  .delta(outputs.getDeltaText())
                                                                                                  .threshold(outputs.getThresholdText())
                                                                                                  .build()));
        });

        Timestamp alertAt = new Timestamp(System.currentTimeMillis());
        try {
            // Save alerting records
            context.log(AlertEvaluator.class, "Saving alert record");
            String id = saveAlertRecord(context, alertAt, notification);

            // notification
            notification.setLastAlertAt(alertAt.getTime());
            notification.setAlertRecordId(id);
            for (String channelName : alertRule.getNotifications()) {
                context.log(AlertEvaluator.class, "Sending alerting notification to channel [%s]", channelName);

                try {
                    notificationApi.notify(channelName, notification);
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
    private void resolveAlert(AlertRule alertRule, EvaluationContext context) {
        // Prepare notification
        NotificationMessage notification = new NotificationMessage();
        notification.setStatus(AlertStatus.RESOLVED);
        notification.setAlertRule(alertRule);
        notification.setExpressions(alertRule.getFlattenExpressions().values());
        notification.setConditionEvaluation(new HashMap<>());
        context.getEvaluationResults().forEach((expressionId, result) -> {
            AlertExpression expression = context.getAlertExpressions().get(expressionId);

            IEvaluationOutput outputs = context.getRuleEvaluationOutput(expressionId);
            notification.getConditionEvaluation()
                        .put(expression.getId(),
                             new ExpressionEvaluationResult(result,
                                                            outputs == null ? null : OutputMessage.builder()
                                                                                                  .current(outputs.getCurrentText())
                                                                                                  .delta(outputs.getDeltaText())
                                                                                                  .threshold(outputs.getThresholdText())
                                                                                                  .build()));
        });

        Timestamp alertAt = new Timestamp(System.currentTimeMillis());
        try {
            // notification
            notification.setLastAlertAt(alertAt.getTime());
            notification.setAlertRecordId(context.getPrevState().getLastRecordId());
            for (String channelName : alertRule.getNotifications()) {
                context.log(AlertEvaluator.class, "Sending RESOLVED notification to channel [%s]", channelName);

                try {
                    notificationApi.notify(channelName, notification);
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

        long startOfThisEvaluation = context.getEvaluatedExpressions()
                                            .values()
                                            .stream()
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
    public void destroy() throws Exception {
        this.evaluationLogWriter.close();
    }
}
