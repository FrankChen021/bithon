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
import org.bithon.server.alerting.common.model.Alert;
import org.bithon.server.alerting.common.model.AlertExpression;
import org.bithon.server.alerting.common.model.IAlertExpressionVisitor;
import org.bithon.server.alerting.common.parser.InvalidExpressionException;
import org.bithon.server.alerting.manager.security.IUserProvider;
import org.bithon.server.storage.alerting.IAlertNotificationProviderStorage;
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
    final IAlertNotificationProviderStorage notificationProviderStorage;

    public AlertCommandService(final IAlertObjectStorage dao,
                               final IMetadataApi metadataApi,
                               ObjectMapper objectMapper,
                               IDataSourceApi dataSourceApi,
                               IUserProvider userProvider,
                               IAlertNotificationProviderStorage notificationProviderStorage) {
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

    private AlertStorageObject toAlertObject(Alert alert) throws BizException {
        try {
            alert.initialize();
        } catch (InvalidExpressionException e) {
            throw new BizException(e.getMessage());
        }

        alert.setExpression(new AlertExpressionSerializer().serialize(alert.getEvaluationExpression()));

        Map<String, ISchema> schemas = dataSourceApi.getSchemas();

        Set<String> ids = new HashSet<>();
        for (AlertExpression alertExpression : alert.getFlattenExpressions().values()) {
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

        for (String id : alert.getNotifications()) {
            if (!this.notificationProviderStorage.exists(id)) {
                throw new BizException("Notification [%s] does not exists.", id);
            }
        }

        AlertStorageObject alertObject = new AlertStorageObject();
        alertObject.setAlertId(alert.getId());
        alertObject.setAlertName(alert.getName());
        alertObject.setAppName(alert.getAppName());
        alertObject.setNamespace("");
        alertObject.setDisabled(!alert.isEnabled());
        try {
            alertObject.setPayload(this.objectMapper.writeValueAsString(alert));
        } catch (IOException e) {
            throw new BizException("Serialization failed", e);
        }
        return alertObject;
    }

    public String createAlert(Alert alert, CommandArgs args) throws BizException {
        if (args.isCheckApplicationExist() && !metadataApi.isApplicationExist(alert.getAppName())) {
            throw new BizException("Target application [%s] does not exist", alert.getAppName());
        }

        if (StringUtils.hasText(alert.getId()) && this.alertObjectStorage.existAlert(alert.getId())) {
            throw new BizException("Alert object [%s] already exists.", alert.getId());
        }

        AlertStorageObject alertObject = toAlertObject(alert);
        String alertId = this.alertObjectStorage.executeTransaction(() -> {
            final String operator = userProvider.getCurrentUser().getUserName();

            String id = this.alertObjectStorage.createAlert(alertObject, operator);

            this.alertObjectStorage.addChangelog(id, ObjectAction.CREATE, operator, "{}", alertObject.getPayload());

            return id;
        });

        alert.setId(alertId);
        alert.setAppName(alertObject.getAppName());
        sendOperationNotification("Alert Created", alert, alert.isEnabled(), userProvider.getCurrentUser().getUserName());
        return alertId;
    }

    public void updateAlert(Alert newAlert) throws BizException {
        AlertStorageObject oldObject = this.alertObjectStorage.getAlertById(newAlert.getId());
        if (oldObject == null) {
            throw new BizException("Alert object [%s] not exist.", newAlert.getName());
        }

        AlertStorageObject newObject = toAlertObject(newAlert);

        boolean successful = this.alertObjectStorage.executeTransaction(() -> {
            if (!this.alertObjectStorage.updateAlert(oldObject, newObject, userProvider.getCurrentUser().getUserName())) {
                return false;
            }

            this.alertObjectStorage.addChangelog(newAlert.getId(), ObjectAction.UPDATE, userProvider.getCurrentUser().getUserName(),
                                                 oldObject.getPayload(),
                                                 newObject.getPayload());
            return true;
        });
        if (!successful) {
            return;
        }

        newAlert.setAppName(newObject.getAppName());
        sendOperationNotification("Alert Updated", newAlert, newAlert.isEnabled(), userProvider.getCurrentUser().getUserName());
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
            Alert oldAlert = objectMapper.readValue(alertObject.getPayload(), Alert.class);
            oldAlert.setId(alertObject.getAlertId());
            oldAlert.setEnabled(!alertObject.getDisabled());
            oldAlert.setAppName(alertObject.getAppName());
            oldAlert.setName(alertObject.getAlertName());
            sendOperationNotification("Alert Enabled", oldAlert, true, userProvider.getCurrentUser().getUserName());
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
            Alert oldAlert = objectMapper.readValue(alertObject.getPayload(), Alert.class);
            oldAlert.setId(alertObject.getAlertId());
            oldAlert.setEnabled(!alertObject.getDisabled());
            oldAlert.setAppName(alertObject.getAppName());
            oldAlert.setName(alertObject.getAlertName());
            sendOperationNotification("Alert Disabled", oldAlert, false, userProvider.getCurrentUser().getUserName());
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
            Alert oldAlert = objectMapper.readValue(alertObject.getPayload(), Alert.class);
            oldAlert.setId(alertObject.getAlertId());
            oldAlert.setEnabled(!alertObject.getDisabled());
            oldAlert.setAppName(alertObject.getAppName());
            oldAlert.setName(alertObject.getAlertName());
            sendOperationNotification("Alert Deleted", oldAlert, oldAlert.isEnabled(), userProvider.getCurrentUser().getUserName());
        } catch (IOException ignored) {
        }
    }

    private void sendOperationNotification(String title,
                                           Alert alert,
                                           boolean enabled,
                                           String operator) {
    }
}
