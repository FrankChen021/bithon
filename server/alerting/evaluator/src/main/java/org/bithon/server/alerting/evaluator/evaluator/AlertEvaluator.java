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
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.HumanReadableDuration;
import org.bithon.component.commons.utils.NetworkUtils;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.alerting.common.evaluator.EvaluationContext;
import org.bithon.server.alerting.common.evaluator.result.IEvaluationOutput;
import org.bithon.server.alerting.common.model.AlertExpression;
import org.bithon.server.alerting.common.model.AlertRule;
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
import org.bithon.server.web.service.datasource.api.IDataSourceApi;
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
@Slf4j
public class AlertEvaluator {

    private final IAlertStateStorage stateStorage;
    private final IEvaluationLogWriter evaluationLogWriter;
    private final IAlertRecordStorage alertRecordStorage;
    private final ObjectMapper objectMapper;
    private final IDataSourceApi dataSourceApi;
    private final INotificationApi notificationApi;

    public AlertEvaluator(IAlertStateStorage stateStorage,
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
    }

    public void evaluate(TimeSpan now, AlertRule alertRule, AlertStateObject prevStatus) {
        EvaluationContext context = new EvaluationContext(now,
                                                          evaluationLogWriter,
                                                          alertRule,
                                                          dataSourceApi,
                                                          prevStatus);

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

            if (evaluate(context)) {
                fireAlert(alertRule, context);
            } else {
                resolveAlert(alertRule, context);
            }
        } catch (Exception e) {
            log.error(StringUtils.format("ERROR to evaluate alert %s", alertRule.getName()), e);
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

    private boolean evaluate(EvaluationContext context) {
        AlertRule alertRule = context.getAlertRule();
        context.log(AlertEvaluator.class, "Evaluating alert [%s] %s ", alertRule.getName(), alertRule.getExpr());

        try {
            AlertExpressionEvaluator expressionEvaluator = new AlertExpressionEvaluator((AlertExpression) alertRule.getAlertExpression());
            if (expressionEvaluator.evaluate(context)) {
                context.log(AlertEvaluator.class, "alert [%s] tested successfully.", alertRule.getName());

                long expectedMatchCount = alertRule.getExpectedMatchCount();
                long successiveCount = stateStorage.incrMatchCount(alertRule.getId(),
                                                                   alertRule.getEvery()
                                                                            .getDuration()
                                                                            // Add 30 seconds for margin
                                                                            .plus(Duration.ofSeconds(30)));
                if (successiveCount >= expectedMatchCount) {
                    stateStorage.resetMatchCount(alertRule.getId());

                    context.log(AlertEvaluator.class,
                                "Rule tested %d times successively，and reaches the expected count：%d",
                                successiveCount,
                                expectedMatchCount);
                    return true;
                } else {
                    context.log(AlertEvaluator.class,
                                "Rule tested %d times successively，expected times：%d",
                                successiveCount,
                                expectedMatchCount);
                    return false;
                }
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

    private void resolveAlert(AlertRule alertRule, EvaluationContext context) {
        if (context.getPrevState() == null || context.getPrevState().getStatus() != AlertStatus.FIRING) {
            return;
        }

        context.log(AlertEvaluator.class, "Alert is resolved.");
        this.alertRecordStorage.updateAlertStatus(alertRule.getId(), context.getPrevState(), AlertStatus.RESOLVED);
    }

    private void fireAlert(AlertRule alertRule, EvaluationContext context) {
        long now = System.currentTimeMillis();

        HumanReadableDuration silenceDuration = context.getAlertRule().getSilence();
        if (stateStorage.tryEnterSilence(alertRule.getId(), silenceDuration.getDuration())) {
            Duration silenceRemainTime = stateStorage.getSilenceRemainTime(alertRule.getId());
            context.log(AlertEvaluator.class, "Alerting，but is under notification silence period(%s). Last alert at: %s",
                        silenceDuration,
                        TimeSpan.of(now - (silenceDuration.getDuration().toMillis() - silenceRemainTime.toMillis())).format("HH:mm:ss"));
            return;
        }

        //
        // Prepare notification
        //
        NotificationMessage notification = new NotificationMessage();
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
            // Save alerting records
            context.log(AlertExpression.class, "Saving alert record");
            String id = saveAlertRecord(context, alertAt, notification);

            // notification
            context.log(AlertExpression.class, "Sending notification");
            notification.setLastAlertAt(alertAt.getTime());
            notification.setAlertRecordId(id);
            for (String name : alertRule.getNotifications()) {
                try {
                    notificationApi.notify(name, notification);
                } catch (Exception e) {
                    log.error("Exception when notifying " + name, e);
                }
            }
        } catch (Exception e) {
            context.logException(AlertExpression.class, e, "Exception when sending notification.");
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

        long start = context.getEvaluatedExpressions()
                            .values()
                            .stream()
                            .map((output) -> output.getStart().getMilliseconds())
                            .min(Comparator.comparingLong((v) -> v))
                            .get();
        alertRecord.setPayload(objectMapper.writeValueAsString(AlertRecordPayload.builder()
                                                                                 .start(start)
                                                                                 .end(context.getIntervalEnd().getMilliseconds())
                                                                                 .expressions(context.getAlertExpressions().values())
                                                                                 .conditionEvaluation(notification.getConditionEvaluation())
                                                                                 .build()));
        alertRecord.setNotificationStatus(IAlertRecordStorage.STATUS_CODE_UNCHECKED);
        alertRecordStorage.addAlertRecord(alertRecord);
        return alertRecord.getRecordId();
    }
}
