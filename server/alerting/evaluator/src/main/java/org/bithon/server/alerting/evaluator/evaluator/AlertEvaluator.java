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
import org.bithon.component.commons.concurrency.NamedThreadFactory;
import org.bithon.component.commons.utils.HumanReadableDuration;
import org.bithon.component.commons.utils.NetworkUtils;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.alerting.common.evaluator.EvaluationContext;
import org.bithon.server.alerting.common.evaluator.EvaluationLogger;
import org.bithon.server.alerting.common.evaluator.result.EvaluationOutputs;
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
import org.bithon.server.storage.alerting.Labels;
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
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
    private final INotificationApi notificationAsyncApi;

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
        this.notificationAsyncApi = createNotificationAsyncApi(applicationContext);

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

            // Evaluate the alert rule
            Map<Labels, AlertStatus> allNewStatus = evaluate(context, context.getPrevState());

            // TODO: Find notifications to send
            Map<Labels, AlertStatus> notificationStatus = new HashMap<>();
            for (Map.Entry<Labels, AlertStatus> entry : allNewStatus.entrySet()) {
                Labels labels = entry.getKey();
                AlertStatus newStatus = entry.getValue();
                AlertStatus prevStatus = context.getPrevState() == null ? AlertStatus.READY : context.getPrevState().getStatusByLabel(labels);

                if (prevStatus.canTransitTo(newStatus)) {
                    context.log(AlertEvaluator.class, "Update alert status: [%s] ---> [%s]", prevStatus, newStatus);

                    if (newStatus == AlertStatus.ALERTING) {
                        notificationStatus.put(labels, newStatus);
                        allNewStatus.put(labels, newStatus);
                    } else if ((prevStatus == AlertStatus.ALERTING || prevStatus == AlertStatus.SUPPRESSING) && newStatus == AlertStatus.RESOLVED) {
                        notificationStatus.put(labels, newStatus);
                        allNewStatus.put(labels, newStatus);
                    } else {
                        // Update alert status only
                        //this.alertRecordStorage.updateAlertStatus(alertRule.getId(), context.getPrevState(), newStatus);
                        allNewStatus.put(labels, newStatus);
                    }
                } else {
                    allNewStatus.put(labels, prevStatus);
                    context.log(AlertEvaluator.class, "Stay in alert status: [%s]", prevStatus);
                }
            }

            // Update state storage
            this.alertRecordStorage.updateAlertStatus(alertRule.getId(), context.getPrevState(), getStatus(allNewStatus), allNewStatus);

            // Group by alert status
            Map<AlertStatus, Map<Labels, AlertStatus>> groupedStatus = notificationStatus.entrySet()
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

        this.stateStorage.setEvaluationTime(alertRule.getId(), now.getMilliseconds(), interval);
    }

    private AlertStatus getStatus(Map<Labels, AlertStatus> status) {
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

    private Map<Labels, AlertStatus> evaluate(EvaluationContext context, AlertStateObject prevState) {
        AlertRule alertRule = context.getAlertRule();
        context.log(AlertEvaluator.class, "Evaluating rule [%s]: %s ", alertRule.getName(), alertRule.getExpr());

        AlertExpressionEvaluator expressionEvaluator = new AlertExpressionEvaluator(alertRule.getAlertExpression());
        if (!expressionEvaluator.evaluate(context)) {
            context.log(AlertEvaluator.class,
                        "Rule [%s] evaluated as FALSE",
                        alertRule.getName());

            stateStorage.resetMatchCount(alertRule.getId());

            Map<Labels, AlertStatus> newStatus = new HashMap<>();
            for (Map.Entry<Labels, AlertStatus> item : prevState.getPayload().getStatus().entrySet()) {
                newStatus.put(item.getKey(), AlertStatus.RESOLVED);
            }
            return newStatus;
        }

        context.log(AlertEvaluator.class, "Rule [%s] evaluated as TRUE", alertRule.getName());

        long expectedMatchCount = alertRule.getExpectedMatchCount();

        Map<Labels, AlertStatus> alertStatus = new HashMap<>();
        Map<Labels, Long> successiveCountList = stateStorage.incrMatchCount(alertRule.getId(),
                                                                            context.getGroups(),
                                                                            alertRule.getEvery()
                                                                                     .getDuration()
                                                                                     // Add 30 seconds for margin
                                                                                     .plus(Duration.ofSeconds(30)));
        for (Map.Entry<Labels, Long> entry : successiveCountList.entrySet()) {
            Labels labels = entry.getKey();
            long successiveCount = entry.getValue();

            AlertStatus newStatus = getAlertStatus(context, prevState, successiveCount, expectedMatchCount);

            alertStatus.put(labels, newStatus);
        }

        return alertStatus;
    }

    private AlertStatus getAlertStatus(EvaluationContext context,
                                       AlertStateObject prevState,
                                       long successiveCount,
                                       long expectedMatchCount) {
        AlertRule alertRule = context.getAlertRule();

        if (successiveCount >= expectedMatchCount) {
            stateStorage.resetMatchCount(alertRule.getId());

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
    }

    /**
     * Fire alert and update its status
     */
    private void fireAlert(AlertRule alertRule, Map<Labels, AlertStatus> labels, EvaluationContext context) {
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
                    notificationAsyncApi.notify(channelName, notification);
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
    private void resolveAlert(AlertRule alertRule, Map<Labels, AlertStatus> labels, EvaluationContext context) {
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
                    notificationAsyncApi.notify(channelName, notification);
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
    public void destroy() throws Exception {
        this.evaluationLogWriter.close();
    }

    private INotificationApi createNotificationAsyncApi(ApplicationContext context) {
        INotificationApi impl;

        // The notification service is configured by auto-discovery
        String service = context.getBean(Environment.class).getProperty("bithon.alerting.evaluator.notification-service", "discovery");
        if ("discovery".equalsIgnoreCase(service)) {
            // Even the notification module is deployed with the evaluator module together,
            // we still call the notification module via HTTP instead of direct API method calls in process
            // So that it simulates the 'remote call' via discovered service
            DiscoveredServiceInvoker invoker = context.getBean(DiscoveredServiceInvoker.class);
            impl = invoker.createUnicastApi(INotificationApi.class);
        } else if (service.startsWith("http:") || service.startsWith("https:")) {
            // The service is configured as a remote service at fixed address
            // Create a feign client to call it
            impl = Feign.builder()
                        .contract(context.getBean(Contract.class))
                        .encoder(context.getBean(Encoder.class))
                        .decoder(context.getBean(Decoder.class))
                        .target(INotificationApi.class, service);
        } else {
            throw new RuntimeException(StringUtils.format("Invalid notification property configured. Only 'discovery' or URL is allowed, but got [%s]", service));
        }

        return new INotificationApi() {
            // Cached thread pool
            private final ThreadPoolExecutor notificationThreadPool = new ThreadPoolExecutor(1,
                                                                                             10,
                                                                                             3,
                                                                                             TimeUnit.MINUTES,
                                                                                             new SynchronousQueue<>(),
                                                                                             NamedThreadFactory.nonDaemonThreadFactory("notification"),
                                                                                             new ThreadPoolExecutor.CallerRunsPolicy());


            @Override
            public void notify(String name, NotificationMessage message) {
                notificationThreadPool.execute(() -> {
                    try {
                        impl.notify(name, message);
                    } catch (Exception e) {
                        new EvaluationLogger(evaluationLogWriter).error(message.getAlertRule().getId(),
                                                                        message.getAlertRule().getName(),
                                                                        AlertEvaluator.class,
                                                                        e,
                                                                        "Failed to send notification to channel [%s]",
                                                                        name);
                    }
                });
            }
        };
    }
}
