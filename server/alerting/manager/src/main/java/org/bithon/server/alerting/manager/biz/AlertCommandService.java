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
import org.bithon.server.alerting.common.model.AlertExpression;
import org.bithon.server.alerting.common.model.AlertRule;
import org.bithon.server.alerting.common.model.IAlertExpressionVisitor;
import org.bithon.server.alerting.common.parser.InvalidExpressionException;
import org.bithon.server.alerting.manager.security.IUserProvider;
import org.bithon.server.storage.alerting.IAlertNotificationChannelStorage;
import org.bithon.server.storage.alerting.IAlertObjectStorage;
import org.bithon.server.storage.alerting.ObjectAction;
import org.bithon.server.storage.alerting.pojo.AlertStorageObject;
import org.bithon.server.storage.alerting.pojo.AlertStorageObjectPayload;
import org.bithon.server.storage.datasource.ISchema;
import org.bithon.server.storage.datasource.column.IColumn;
import org.bithon.server.web.service.datasource.api.IDataSourceApi;
import org.bithon.server.web.service.meta.api.IMetadataApi;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
                throw new BizException("Notification channel [%s] does not exist", id);
            }
        }

        AlertStorageObject alertObject = new AlertStorageObject();
        alertObject.setId(alertRule.getId());
        alertObject.setName(alertRule.getName());
        alertObject.setAppName(alertRule.getAppName());
        alertObject.setNamespace("");
        alertObject.setDisabled(!alertRule.isEnabled());
        alertObject.setPayload(AlertStorageObjectPayload.builder()
                                                        .evaluationInterval(alertRule.getEvaluationInterval())
                                                        .expr(alertRule.getExpr())
                                                        .forDuration(alertRule.getForDuration())
                                                        .notifications(alertRule.getNotifications())
                                                        .silence(alertRule.getSilence())
                                                        .build());
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
        if (!StringUtils.hasText(alertObject.getId())) {
            alertObject.setId(UUID.randomUUID().toString().replace("-", ""));
        }

        return this.alertObjectStorage.executeTransaction(() -> {
            final String operator = userProvider.getCurrentUser().getUserName();

            this.alertObjectStorage.createAlert(alertObject, operator);

            this.alertObjectStorage.addChangelog(alertObject.getId(),
                                                 ObjectAction.CREATE,
                                                 operator, "{}",
                                                 objectMapper.writeValueAsString(alertObject.getPayload()));

            return alertObject.getId();
        });
    }

    public void updateAlert(AlertRule newAlertRule) throws BizException {
        AlertStorageObject oldObject = this.alertObjectStorage.getAlertById(newAlertRule.getId());
        if (oldObject == null) {
            throw new BizException("Alert object [%s] not exist.", newAlertRule.getName());
        }

        AlertStorageObject newObject = toAlertObject(newAlertRule);

        this.alertObjectStorage.executeTransaction(() -> {
            if (!this.alertObjectStorage.updateAlert(oldObject, newObject, userProvider.getCurrentUser().getUserName())) {
                return false;
            }

            this.alertObjectStorage.addChangelog(newAlertRule.getId(),
                                                 ObjectAction.UPDATE,
                                                 userProvider.getCurrentUser().getUserName(),
                                                 objectMapper.writeValueAsString(oldObject.getPayload()),
                                                 objectMapper.writeValueAsString(newObject.getPayload()));
            return true;
        });
    }

    public void enableAlert(String alertId) throws BizException {
        AlertStorageObject alertObject = this.alertObjectStorage.getAlertById(alertId);
        if (alertObject == null) {
            throw new BizException("Alert object [%s] not exist.", alertId);
        }

        this.alertObjectStorage.executeTransaction(() -> {
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
    }

    public void disableAlert(String alertId) throws BizException {
        AlertStorageObject alertObject = this.alertObjectStorage.getAlertById(alertId);
        if (alertObject == null) {
            throw new BizException("Alert object [%s] not exist.", alertId);
        }
        this.alertObjectStorage.executeTransaction(() -> {
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
    }

    public void deleteAlert(String alertId) throws BizException {
        AlertStorageObject alertObject = this.alertObjectStorage.getAlertById(alertId);
        if (alertObject == null) {
            throw new BizException("Alert object [%s] not exist.", alertId);
        }

        this.alertObjectStorage.executeTransaction(() -> {
            if (!this.alertObjectStorage.deleteAlert(alertId, userProvider.getCurrentUser().getUserName())) {
                return false;
            }
            this.alertObjectStorage.addChangelog(alertId,
                                                 ObjectAction.DELETE,
                                                 userProvider.getCurrentUser().getUserName(),
                                                 objectMapper.writeValueAsString(alertObject.getPayload()),
                                                 "{}");
            return true;
        });
    }
}