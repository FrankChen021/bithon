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

package org.bithon.server.alerting.manager.biz;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.expression.expt.InvalidExpressionException;
import org.bithon.component.commons.expression.serialization.ExpressionSerializer;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.alerting.common.model.AlertExpression;
import org.bithon.server.alerting.common.model.AlertRule;
import org.bithon.server.alerting.common.model.IAlertInDepthExpressionVisitor;
import org.bithon.server.alerting.evaluator.evaluator.AlertEvaluator;
import org.bithon.server.alerting.evaluator.evaluator.INotificationApiInvoker;
import org.bithon.server.alerting.evaluator.state.local.LocalStateManager;
import org.bithon.server.alerting.manager.ManagerModuleEnabler;
import org.bithon.server.alerting.manager.security.IUserProvider;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.alerting.IAlertNotificationChannelStorage;
import org.bithon.server.storage.alerting.IAlertObjectStorage;
import org.bithon.server.storage.alerting.IAlertRecordStorage;
import org.bithon.server.storage.alerting.IEvaluationLogReader;
import org.bithon.server.storage.alerting.IEvaluationLogStorage;
import org.bithon.server.storage.alerting.IEvaluationLogWriter;
import org.bithon.server.storage.alerting.Label;
import org.bithon.server.storage.alerting.ObjectAction;
import org.bithon.server.storage.alerting.pojo.AlertRecordObject;
import org.bithon.server.storage.alerting.pojo.AlertStateObject;
import org.bithon.server.storage.alerting.pojo.AlertStatus;
import org.bithon.server.storage.alerting.pojo.AlertStorageObject;
import org.bithon.server.storage.alerting.pojo.AlertStorageObjectPayload;
import org.bithon.server.storage.alerting.pojo.EvaluationLogEvent;
import org.bithon.server.storage.alerting.pojo.ListResult;
import org.bithon.server.storage.common.expiration.IExpirationRunnable;
import org.bithon.server.storage.datasource.ISchema;
import org.bithon.server.storage.datasource.query.Limit;
import org.bithon.server.storage.metrics.Interval;
import org.bithon.server.web.service.datasource.api.IDataSourceApi;
import org.bithon.server.web.service.meta.api.IMetadataApi;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/31
 */
@Slf4j
@Service
@Conditional(ManagerModuleEnabler.class)
public class AlertCommandService {

    @Getter
    @Setter
    static class ChangeLogPayload extends AlertStorageObjectPayload {
        private boolean enabled;

        public ChangeLogPayload(AlertStorageObject obj) {
            this.setEvery(obj.getPayload().getEvery());
            this.setExpr(obj.getPayload().getExpr());
            this.setSilence(obj.getPayload().getSilence());
            this.setNotifications(obj.getPayload().getNotifications());
            this.setForTimes(obj.getPayload().getForTimes());
            this.enabled = !obj.isDisabled();
        }
    }

    final IAlertObjectStorage alertObjectStorage;
    final IMetadataApi metadataApi;
    final ObjectMapper objectMapper;
    final IDataSourceApi dataSourceApi;
    final IUserProvider userProvider;
    final IAlertNotificationChannelStorage notificationChannelStorage;
    final ApplicationContext applicationContext;

    public AlertCommandService(final IAlertObjectStorage dao,
                               final IMetadataApi metadataApi,
                               ObjectMapper objectMapper,
                               IDataSourceApi dataSourceApi,
                               IUserProvider userProvider,
                               IAlertNotificationChannelStorage notificationChannelStorage,
                               ApplicationContext applicationContext) {
        this.alertObjectStorage = dao;
        this.metadataApi = metadataApi;
        this.objectMapper = objectMapper;
        this.dataSourceApi = dataSourceApi;
        this.userProvider = userProvider;
        this.notificationChannelStorage = notificationChannelStorage;
        this.applicationContext = applicationContext;
    }

    static class AlertExpressionSerializer extends ExpressionSerializer implements IAlertInDepthExpressionVisitor {
        public AlertExpressionSerializer() {
            super(null);
        }

        @Override
        public void visit(AlertExpression expression) {
            sb.append(expression.serializeToText(true));
        }
    }

    private AlertStorageObject toAlertStorageObject(AlertRule alertRule) throws BizException {
        try {
            alertRule.initialize();
        } catch (InvalidExpressionException e) {
            throw new BizException(e.getMessage());
        }

        Map<String, ISchema> schemas = dataSourceApi.getSchemas();

        for (AlertExpression alertExpression : alertRule.getFlattenExpressions().values()) {
            alertExpression.getMetricExpression().validate(schemas);
        }

        for (String channel : alertRule.getNotificationProps().getChannels()) {
            if (!this.notificationChannelStorage.exists(channel)) {
                throw new BizException("Notification channel [%s] does not exist", channel);
            }
        }

        AlertStorageObject alertStorageObject = new AlertStorageObject();
        alertStorageObject.setId(alertRule.getId());
        alertStorageObject.setName(alertRule.getName());
        alertStorageObject.setAppName(alertRule.getAppName());
        alertStorageObject.setNamespace("");
        alertStorageObject.setDisabled(!alertRule.isEnabled());
        alertStorageObject.setPayload(AlertStorageObjectPayload.builder()
                                                               .every(alertRule.getEvery())
                                                               .expr(alertRule.getExpr())
                                                               .forTimes(alertRule.getForTimes())
                                                               .notificationProps(alertRule.getNotificationProps())
                                                               .build());
        return alertStorageObject;
    }

    public String createRule(AlertRule alertRule, CommandArgs args) throws BizException {
        if (args.isCheckApplicationExist() && !metadataApi.isApplicationExist(alertRule.getAppName())) {
            throw new BizException("Target application [%s] does not exist", alertRule.getAppName());
        }

        if (StringUtils.hasText(alertRule.getId()) && this.alertObjectStorage.existAlertById(alertRule.getId())) {
            throw new BizException("Alert rule with the same id [%s] already exists.", alertRule.getId());
        }

        if (this.alertObjectStorage.existAlertByName(alertRule.getName())) {
            throw new BizException("Alert rule with the name [%s] already exists.", alertRule.getName());
        }

        AlertStorageObject alertStorageObject = toAlertStorageObject(alertRule);
        if (!StringUtils.hasText(alertStorageObject.getId())) {
            alertStorageObject.setId(UUID.randomUUID().toString().replace("-", ""));
        }

        return this.alertObjectStorage.executeTransaction(() -> {
            final String operator = userProvider.getCurrentUser().getUserName();

            this.alertObjectStorage.createAlert(alertStorageObject, operator);

            this.alertObjectStorage.addChangelog(alertStorageObject.getId(),
                                                 ObjectAction.CREATE,
                                                 operator, "{}",
                                                 objectMapper.writeValueAsString(alertStorageObject.getPayload()));

            return alertStorageObject.getId();
        });
    }

    public void updateRule(AlertRule newAlertRule) throws BizException {
        AlertStorageObject oldRule = this.alertObjectStorage.getAlertById(newAlertRule.getId());
        if (oldRule == null) {
            throw new BizException("Alert rule [%s] not exist.", newAlertRule.getName());
        }

        AlertStorageObject newRule = toAlertStorageObject(newAlertRule);

        this.alertObjectStorage.executeTransaction(() -> {
            if (!this.alertObjectStorage.updateAlert(oldRule, newRule, userProvider.getCurrentUser().getUserName())) {
                return false;
            }

            this.alertObjectStorage.addChangelog(newAlertRule.getId(),
                                                 ObjectAction.UPDATE,
                                                 userProvider.getCurrentUser().getUserName(),
                                                 objectMapper.writeValueAsString(new ChangeLogPayload(oldRule)),
                                                 objectMapper.writeValueAsString(new ChangeLogPayload(newRule)));
            return true;
        });
    }

    public void enableRule(String ruleId) throws BizException {
        AlertStorageObject rule = this.alertObjectStorage.getAlertById(ruleId);
        if (rule == null) {
            throw new BizException("Alert rule [%s] not exist.", ruleId);
        }

        this.alertObjectStorage.executeTransaction(() -> {
            if (!this.alertObjectStorage.enableAlert(ruleId, userProvider.getCurrentUser().getUserName())) {
                return false;
            }
            this.alertObjectStorage.addChangelog(ruleId,
                                                 ObjectAction.ENABLE,
                                                 userProvider.getCurrentUser().getUserName(),
                                                 "{}",
                                                 "{\"enabled\": true}");
            return true;
        });
    }

    public void disableRule(String ruleId) throws BizException {
        AlertStorageObject rule = this.alertObjectStorage.getAlertById(ruleId);
        if (rule == null) {
            throw new BizException("Alert rule [%s] not exist.", ruleId);
        }
        this.alertObjectStorage.executeTransaction(() -> {
            if (!this.alertObjectStorage.disableAlert(ruleId, userProvider.getCurrentUser().getUserName())) {
                return false;
            }
            this.alertObjectStorage.addChangelog(ruleId,
                                                 ObjectAction.DISABLE,
                                                 userProvider.getCurrentUser().getUserName(),
                                                 "{}",
                                                 "{\"enabled\": false}");
            return true;
        });
    }

    public void deleteRule(String alertId) throws BizException {
        AlertStorageObject rule = this.alertObjectStorage.getAlertById(alertId);
        if (rule == null) {
            throw new BizException("Alert rule[%s] not exist.", alertId);
        }

        this.alertObjectStorage.executeTransaction(() -> {
            if (!this.alertObjectStorage.deleteAlert(alertId, userProvider.getCurrentUser().getUserName())) {
                return false;
            }
            this.alertObjectStorage.addChangelog(alertId,
                                                 ObjectAction.DELETE,
                                                 userProvider.getCurrentUser().getUserName(),
                                                 objectMapper.writeValueAsString(new ChangeLogPayload(rule)),
                                                 "{}");
            return true;
        });
    }

    public List<EvaluationLogEvent> testRule(AlertRule rule) {
        EvaluationLogLocalStorage logStorage = new EvaluationLogLocalStorage();

        IAlertRecordStorage recordStorage = new IAlertRecordStorage() {
            @Override
            public Timestamp getLastAlert(String alertId) {
                return null;
            }

            @Override
            public void addAlertRecord(AlertRecordObject record) {

            }

            @Override
            public ListResult<AlertRecordObject> getAlertRecords(String alertId, Interval interval, Limit limit) {
                return null;
            }

            @Override
            public AlertRecordObject getAlertRecord(String id) {
                return null;
            }

            @Override
            public List<AlertRecordObject> getRecordsByNotificationStatus(int statusCode) {
                return List.of();
            }

            @Override
            public void setNotificationResult(String id, int statusCode, String status) {
            }

            @Override
            public void initialize() {
            }

            @Override
            public void updateAlertStatus(String id, AlertStateObject prevState, AlertStatus newStatus, Map<Label, AlertStatus> statusPerLabel) {
            }

            @Override
            public String getName() {
                return "";
            }

            @Override
            public IExpirationRunnable getExpirationRunnable() {
                return null;
            }
        };

        AlertEvaluator evaluator = new AlertEvaluator(null,
                                                      new LocalStateManager(),
                                                      logStorage.createWriter(),
                                                      recordStorage,
                                                      this.dataSourceApi,
                                                      applicationContext.getBean(ServerProperties.class),
                                                      applicationContext.getBean(INotificationApiInvoker.class),
                                                      this.objectMapper);

        evaluator.evaluate(TimeSpan.now().floor(Duration.ofMinutes(1)),
                           rule);

        return logStorage.getLogs();
    }

    private static class EvaluationLogLocalStorage implements IEvaluationLogStorage {
        @Getter
        private final List<EvaluationLogEvent> logs = new ArrayList<>();

        @Override
        public void initialize() {
        }

        @Override
        public IEvaluationLogWriter createWriter() {
            return new IEvaluationLogWriter() {
                private String instance;

                @Override
                public void setInstance(String instance) {
                    this.instance = instance;
                }

                @Override
                public void write(EvaluationLogEvent logEvent) {
                    logEvent.setInstance(instance);
                    logs.add(logEvent);
                }

                @Override
                public void write(List<EvaluationLogEvent> logs) {
                    logs.forEach(this::write);
                }

                @Override
                public void close() {
                }
            };
        }

        @Override
        public IEvaluationLogReader createReader() {
            return null;
        }

        @Override
        public String getName() {
            return "";
        }

        @Override
        public IExpirationRunnable getExpirationRunnable() {
            return null;
        }
    }
}
