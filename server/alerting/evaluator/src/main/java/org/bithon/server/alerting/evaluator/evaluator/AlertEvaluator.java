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
import org.bithon.component.commons.utils.NetworkUtils;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.alerting.common.evaluator.EvaluationContext;
import org.bithon.server.alerting.common.model.AlertRule;
import org.bithon.server.alerting.evaluator.evaluator.pipeline.ExpressionEvaluationStep;
import org.bithon.server.alerting.evaluator.evaluator.pipeline.IPipelineStep;
import org.bithon.server.alerting.evaluator.evaluator.pipeline.InhibitionStep;
import org.bithon.server.alerting.evaluator.evaluator.pipeline.NotificationStep;
import org.bithon.server.alerting.evaluator.evaluator.pipeline.Pipeline;
import org.bithon.server.alerting.evaluator.evaluator.pipeline.RuleEvaluationStep;
import org.bithon.server.alerting.evaluator.repository.AlertRepository;
import org.bithon.server.alerting.evaluator.repository.IAlertChangeListener;
import org.bithon.server.alerting.evaluator.state.IEvaluationStateManager;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.alerting.IAlertRecordStorage;
import org.bithon.server.storage.alerting.IEvaluationLogWriter;
import org.bithon.server.storage.alerting.Label;
import org.bithon.server.storage.alerting.pojo.AlertState;
import org.bithon.server.storage.alerting.pojo.AlertStatus;
import org.bithon.server.storage.alerting.pojo.EvaluationLogEvent;
import org.bithon.server.web.service.datasource.api.IDataSourceApi;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.web.ServerProperties;

import java.sql.Timestamp;
import java.time.Duration;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/11 10:40 上午
 */
public class AlertEvaluator implements DisposableBean {

    @Getter
    private final IEvaluationStateManager stateManager;
    private final IEvaluationLogWriter evaluationLogWriter;
    private final IDataSourceApi dataSourceApi;
    private final ObjectMapper objectMapper;
    private final IAlertRecordStorage recordStorage;
    private final INotificationApiInvoker notificationApiInvoker;

    public AlertEvaluator(AlertRepository repository,
                          IEvaluationStateManager stateManager,
                          IEvaluationLogWriter evaluationLogWriter,
                          IAlertRecordStorage recordStorage,
                          IDataSourceApi dataSourceApi,
                          ServerProperties serverProperties,
                          INotificationApiInvoker notificationApiInvoker,
                          ObjectMapper objectMapper) {

        // Use Indent output for better debugging
        // It's a copy of existing ObjectMapper
        // because the injected ObjectMapper has extra serialization/deserialization configurations
        this.objectMapper = objectMapper.copy().enable(SerializationFeature.INDENT_OUTPUT);
        this.recordStorage = recordStorage;
        this.notificationApiInvoker = notificationApiInvoker;
        this.stateManager = stateManager;
        this.dataSourceApi = dataSourceApi;
        this.evaluationLogWriter = evaluationLogWriter;
        this.evaluationLogWriter.setInstance(NetworkUtils.getIpAddress().getHostAddress() + ":" + serverProperties.getPort());

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
        AlertState stateObject = this.getStateManager().getAlertState(rule.getId());
        this.evaluate(now, rule, stateObject, false);
    }

    /**
     * @param prevState can be null
     */
    @VisibleForTesting
    void evaluate(TimeSpan now, AlertRule alertRule, AlertState prevState) {
        this.evaluate(now, alertRule, prevState, false);
    }

    @VisibleForTesting
    void evaluate(TimeSpan now, AlertRule alertRule, AlertState prevState, boolean skipPrecheck) {
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

                TimeSpan lastEvaluationTimestamp = TimeSpan.of(this.stateManager.getLastEvaluationTimestamp(alertRule.getId()));
                if (now.diff(lastEvaluationTimestamp) < interval.toMillis()) {
                    context.log(AlertEvaluator.class,
                                "Evaluation skipped, it's expected to be evaluated at %s",
                                lastEvaluationTimestamp.after(interval).format("HH:mm"));
                    return;
                }
            }

            Pipeline pipeline = new Pipeline();
            pipeline.addStep(new ExpressionEvaluationStep());
            pipeline.addStep(new RuleEvaluationStep());
            pipeline.addStep(new InhibitionStep());
            pipeline.addStep(new NotificationStep(recordStorage, notificationApiInvoker, this.objectMapper));
            pipeline.addStep(new IPipelineStep() {
                @Override
                public void evaluate(IEvaluationStateManager stateManager, EvaluationContext context) {
                    String ruleId = context.getAlertRule().getId();
                    stateManager.setLastEvaluationTime(ruleId, System.currentTimeMillis(), context.getAlertRule().getEvery().getDuration());
                    stateManager.setState(ruleId, mergeStatus(context.getSeriesStatus()), context.getSeriesStatus());
                }
            });
            pipeline.evaluate(this.stateManager, context);
        } catch (Exception e) {
            context.logException(AlertEvaluator.class, e, "ERROR to evaluate alert %s", alertRule.getName());
        }
    }

    /**
     * Merge status per label into a single status at the rule level
     */
    private AlertStatus mergeStatus(Map<Label, AlertStatus> status) {
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

    @Override
    public void destroy() {
        this.evaluationLogWriter.close();
    }

}
