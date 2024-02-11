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
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.expression.IExpressionVisitor;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.serialization.ExpressionSerializer;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.alerting.common.model.AlertRule;
import org.bithon.server.alerting.common.model.AlertExpression;
import org.bithon.server.alerting.common.model.IAlertExpressionVisitor;
import org.bithon.server.alerting.common.parser.InvalidExpressionException;
import org.bithon.server.alerting.manager.security.IUserProvider;
import org.bithon.server.storage.alerting.IAlertNotificationChannelStorage;
import org.bithon.server.storage.alerting.IAlertObjectStorage;
import org.bithon.server.storage.alerting.ObjectAction;
import org.bithon.server.storage.alerting.pojo.AlertStorageObject;
import org.bithon.server.storage.datasource.ISchema;
import org.bithon.server.storage.datasource.column.IColumn;
import org.bithon.server.web.service.datasource.api.IDataSourceApi;
import org.bithon.server.web.service.meta.api.IMetadataApi;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/31
 */
@Slf4j
@Service
public class AlertCommandService {

    final IAlertObjectStorage alertObjectStorage;
    final IMetadataApi metadataApi;
    final ObjectMapper objectMapper;
    final IDataSourceApi dataSourceApi;
    final IUserProvider userProvider;
    final IAlertNotificationChannelStorage notificationProviderStorage;

    public AlertCommandService(final IAlertObjectStorage dao,
                               final IMetadataApi metadataApi,
                               ObjectMapper objectMapper,
                               IDataSourceApi dataSourceApi,
                               IUserProvider userProvider,
                               IAlertNotificationChannelStorage notificationProviderStorage) {
        this.alertObjectStorage = dao;
        this.metadataApi = metadataApi;
        this.objectMapper = objectMapper;
        this.dataSourceApi = dataSourceApi;
        this.userProvider = userProvider;
        this.notificationProviderStorage = notificationProviderStorage;
    }

    static class AlertExpressionSerializer extends ExpressionSerializer implements IAlertExpressionVisitor {
        public AlertExpressionSerializer() {
            super(null);
        }

        @Override
        public void visit(AlertExpression expression) {
            sb.append(expression.serializeToText(true));
        }
    }

    private AlertStorageObject toAlertObject(AlertRule alertRule) throws BizException {
        try {
            alertRule.initialize();
        } catch (InvalidExpressionException e) {
            throw new BizException(e.getMessage());
        }

        alertRule.setExpr(new AlertExpressionSerializer().serialize(alertRule.getEvaluationExpression()));

        Map<String, ISchema> schemas = dataSourceApi.getSchemas();

        Set<String> ids = new HashSet<>();
        for (AlertExpression alertExpression : alertRule.getFlattenExpressions().values()) {
            if (!ids.add(alertExpression.getId())) {
                throw new BizException("There are multiple conditions with the same id[%s]", alertExpression.serializeToText());
            }

            if (!StringUtils.hasText(alertExpression.getFrom())) {
                throw new BizException("data-source is missed for expression [%s]", alertExpression.serializeToText());
            }
            ISchema schema = schemas.get(alertExpression.getFrom());
            if (schema == null) {
                throw new BizException("data-source [%s] does not exist for expression [%s]", alertExpression.getFrom(), alertExpression.serializeToText());
            }

            if (alertExpression.getWhereExpression() != null) {
                alertExpression.getWhereExpression().accept(new IExpressionVisitor() {
                    @Override
                    public boolean visit(IdentifierExpression expression) {
                        IColumn dimensionSpec = schema.getColumnByName(expression.getIdentifier());
                        if (dimensionSpec == null) {
                            throw new BizException("Dimension [%s] specified in expression [%s] does not exist",
                                                   expression.getIdentifier(),
                                                   alertExpression.serializeToText());
                        }
                        return false;
                    }
                });
            }

            String metric = alertExpression.getSelect().getName();
            if (schema.getColumnByName(metric) == null) {
                throw new BizException("Metric [%s] in expression [%s] does not exist in data-source [%s]",
                                       metric,
                                       alertExpression.serializeToText(),
                                       alertExpression.getFrom());
            }
        }

        for (String id : alertRule.getNotifications()) {
            if (!this.notificationProviderStorage.exists(id)) {
                throw new BizException("Notification [%s] does not exists.", id);
            }
        }

        AlertStorageObject alertObject = new AlertStorageObject();
        alertObject.setAlertId(alertRule.getId());
        alertObject.setAlertName(alertRule.getName());
        alertObject.setAppName(alertRule.getAppName());
        alertObject.setNamespace("");
        alertObject.setDisabled(!alertRule.isEnabled());
        try {
            alertObject.setPayload(this.objectMapper.writeValueAsString(alertRule));
        } catch (IOException e) {
            throw new BizException("Serialization failed", e);
        }
        return alertObject;
    }

    public String createAlert(AlertRule alertRule, CommandArgs args) throws BizException {
        if (args.isCheckApplicationExist() && !metadataApi.isApplicationExist(alertRule.getAppName())) {
            throw new BizException("Target application [%s] does not exist", alertRule.getAppName());
        }

        if (StringUtils.hasText(alertRule.getId()) && this.alertObjectStorage.existAlert(alertRule.getId())) {
            throw new BizException("Alert object [%s] already exists.", alertRule.getId());
        }

        AlertStorageObject alertObject = toAlertObject(alertRule);
        String alertId = this.alertObjectStorage.executeTransaction(() -> {
            final String operator = userProvider.getCurrentUser().getUserName();

            String id = this.alertObjectStorage.createAlert(alertObject, operator);

            this.alertObjectStorage.addChangelog(id, ObjectAction.CREATE, operator, "{}", alertObject.getPayload());

            return id;
        });

        alertRule.setId(alertId);
        alertRule.setAppName(alertObject.getAppName());
        sendOperationNotification("Alert Created", alertRule, alertRule.isEnabled(), userProvider.getCurrentUser().getUserName());
        return alertId;
    }

    public void updateAlert(AlertRule newAlertRule) throws BizException {
        AlertStorageObject oldObject = this.alertObjectStorage.getAlertById(newAlertRule.getId());
        if (oldObject == null) {
            throw new BizException("Alert object [%s] not exist.", newAlertRule.getName());
        }

        AlertStorageObject newObject = toAlertObject(newAlertRule);

        boolean successful = this.alertObjectStorage.executeTransaction(() -> {
            if (!this.alertObjectStorage.updateAlert(oldObject, newObject, userProvider.getCurrentUser().getUserName())) {
                return false;
            }

            this.alertObjectStorage.addChangelog(newAlertRule.getId(), ObjectAction.UPDATE, userProvider.getCurrentUser().getUserName(),
                                                 oldObject.getPayload(),
                                                 newObject.getPayload());
            return true;
        });
        if (!successful) {
            return;
        }

        newAlertRule.setAppName(newObject.getAppName());
        sendOperationNotification("Alert Updated", newAlertRule, newAlertRule.isEnabled(), userProvider.getCurrentUser().getUserName());
    }

    public void enableAlert(String alertId) throws BizException {
        AlertStorageObject alertObject = this.alertObjectStorage.getAlertById(alertId);
        if (alertObject == null) {
            throw new BizException("Alert object [%s] not exist.", alertId);
        }

        boolean successful = this.alertObjectStorage.executeTransaction(() -> {
            if (!this.alertObjectStorage.enableAlert(alertId, userProvider.getCurrentUser().getUserName())) {
                return false;
            }
            this.alertObjectStorage.addChangelog(alertId,
                                                 ObjectAction.ENABLE,
                                                 userProvider.getCurrentUser().getUserName(),
                                                 "{}",
                                                 "{\"enabled\": true}");
            return true;
        });
        if (!successful) {
            return;
        }

        try {
            AlertRule oldAlertRule = objectMapper.readValue(alertObject.getPayload(), AlertRule.class);
            oldAlertRule.setId(alertObject.getAlertId());
            oldAlertRule.setEnabled(!alertObject.getDisabled());
            oldAlertRule.setAppName(alertObject.getAppName());
            oldAlertRule.setName(alertObject.getAlertName());
            sendOperationNotification("Alert Enabled", oldAlertRule, true, userProvider.getCurrentUser().getUserName());
        } catch (IOException e) {
            log.error("send notification", e);
        }
    }

    public void disableAlert(String alertId) throws BizException {
        AlertStorageObject alertObject = this.alertObjectStorage.getAlertById(alertId);
        if (alertObject == null) {
            throw new BizException("Alert object [%s] not exist.", alertId);
        }
        boolean successful = this.alertObjectStorage.executeTransaction(() -> {
            if (!this.alertObjectStorage.disableAlert(alertId, userProvider.getCurrentUser().getUserName())) {
                return false;
            }
            this.alertObjectStorage.addChangelog(alertId,
                                                 ObjectAction.DISABLE,
                                                 userProvider.getCurrentUser().getUserName(),
                                                 "{}",
                                                 "{\"enabled\": false}");
            return true;
        });
        if (!successful) {
            return;
        }

        try {
            AlertRule oldAlertRule = objectMapper.readValue(alertObject.getPayload(), AlertRule.class);
            oldAlertRule.setId(alertObject.getAlertId());
            oldAlertRule.setEnabled(!alertObject.getDisabled());
            oldAlertRule.setAppName(alertObject.getAppName());
            oldAlertRule.setName(alertObject.getAlertName());
            sendOperationNotification("Alert Disabled", oldAlertRule, false, userProvider.getCurrentUser().getUserName());
        } catch (IOException e) {
            log.error("send notification", e);
        }
    }

    public void deleteAlert(String alertId) throws BizException {
        AlertStorageObject alertObject = this.alertObjectStorage.getAlertById(alertId);
        if (alertObject == null) {
            throw new BizException("Alert object [%s] not exist.", alertId);
        }

        boolean successful = this.alertObjectStorage.executeTransaction(() -> {
            if (!this.alertObjectStorage.deleteAlert(alertId, userProvider.getCurrentUser().getUserName())) {
                return false;
            }
            this.alertObjectStorage.addChangelog(alertId,
                                                 ObjectAction.DELETE,
                                                 userProvider.getCurrentUser().getUserName(),
                                                 alertObject.getPayload(),
                                                 "{}");
            return true;
        });
        if (!successful) {
            return;
        }

        try {
            AlertRule oldAlertRule = objectMapper.readValue(alertObject.getPayload(), AlertRule.class);
            oldAlertRule.setId(alertObject.getAlertId());
            oldAlertRule.setEnabled(!alertObject.getDisabled());
            oldAlertRule.setAppName(alertObject.getAppName());
            oldAlertRule.setName(alertObject.getAlertName());
            sendOperationNotification("Alert Deleted", oldAlertRule, oldAlertRule.isEnabled(), userProvider.getCurrentUser().getUserName());
        } catch (IOException ignored) {
        }
    }

    private void sendOperationNotification(String title,
                                           AlertRule alertRule,
                                           boolean enabled,
                                           String operator) {
    }
}
