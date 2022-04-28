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

package org.bithon.server.alerting.admin.biz;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.alerting.common.AlertingModule;
import org.bithon.server.alerting.common.evaluator.rule.builder.InvalidRuleExpressionException;
import org.bithon.server.alerting.common.model.Alert;
import org.bithon.server.alerting.common.model.AlertCondition;
import org.bithon.server.alerting.common.model.IMetricCondition;
import org.bithon.server.commons.matcher.StringEqualMatcher;
import org.bithon.server.storage.alerting.IAlertObjectStorage;
import org.bithon.server.storage.alerting.ObjectAction;
import org.bithon.server.storage.alerting.pojo.AlertStorageObject;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.aggregator.spec.IMetricSpec;
import org.bithon.server.storage.datasource.dimension.IDimensionSpec;
import org.bithon.server.storage.metrics.DimensionFilter;
import org.bithon.server.storage.metrics.IFilter;
import org.bithon.server.web.service.api.IDataSourceApi;
import org.bithon.server.web.service.meta.api.IMetadataApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/31
 */
@Slf4j
@Service
@ConditionalOnBean(AlertingModule.class)
public class AlertCommandService {

    final IAlertObjectStorage alertObjectStorage;
    final IMetadataApi metadataApi;
    final ObjectMapper objectMapper;
    final IDataSourceApi dataSourceApi;

    public AlertCommandService(final IAlertObjectStorage dao,
                               final IMetadataApi metadataApi,
                               ObjectMapper objectMapper,
                               IDataSourceApi dataSourceApi) {
        this.alertObjectStorage = dao;
        this.metadataApi = metadataApi;
        this.objectMapper = objectMapper;
        this.dataSourceApi = dataSourceApi;
    }

    private AlertStorageObject toAlertObject(Alert alert) throws BizException {
        //
        // ensure appName for each AlertCondition
        //
        IFilter appCondition = new DimensionFilter("appName", new StringEqualMatcher(alert.getAppName()));
        alert.getConditions().forEach((alertCondition) -> {
            if (alertCondition.getDimensions() == null) {
                alertCondition.setDimensions(Collections.singletonList(appCondition));
            }
            Optional<IFilter> optionalAppCondition = alertCondition.getDimensions()
                                                                   .stream()
                                                                   .filter(dimension -> "appName".equals(dimension.getName()))
                                                                   .findFirst();
            if (!optionalAppCondition.isPresent()) {
                alertCondition.getDimensions().add(0, appCondition);
            }
        });

        try {
            alert.initialize();
        } catch (InvalidRuleExpressionException e) {
            throw new BizException(e.getMessage());
        }

        Map<String, DataSourceSchema> schemas = dataSourceApi.getSchemas();

        Set<String> ids = new HashSet<>();
        for (AlertCondition alertCondition : alert.getConditions()) {
            if (!ids.add(alertCondition.getId())) {
                throw new BizException("There are multiple conditions with the same id[%s]", alertCondition.getId());
            }

            if (!StringUtils.hasText(alertCondition.getDataSource())) {
                throw new BizException("Data source is missed for condition [%s]", alertCondition.getId());
            }
            DataSourceSchema schema = schemas.get(alertCondition.getDataSource());
            if (schema == null) {
                throw new BizException("Data source [%s] does not exist for condition [%s]", alertCondition.getDataSource(), alertCondition.getId());
            }

            if (!CollectionUtils.isEmpty(alertCondition.getDimensions())) {
                for (IFilter dimensionCondition : alertCondition.getDimensions()) {
                    IDimensionSpec dimensionSpec = schema.getDimensionSpecByName(dimensionCondition.getName());
                    if (dimensionSpec == null) {
                        throw new BizException("Dimension [%s] specified in condition [%s] does not exist",
                                               dimensionCondition.getName(),
                                               alertCondition.getId());
                    }
                }
            }

            IMetricCondition metricCondition = alertCondition.getMetric();
            if (metricCondition == null) {
                throw new BizException("Metrics comparators are missed for condition [%s]", alertCondition.getId());
            }
            IMetricSpec metricSpec = schema.getMetricSpecByName(metricCondition.getName());
            if (metricSpec == null) {
                throw new BizException("Metric [%s] specified in condition [%s] does not exist", metricCondition.getName(), alertCondition.getId());
            }
            //metricSpec.validate(metricCondition.)
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
            final String operator = UserHolder.getCurrentUser().getUserName();

            String id = this.alertObjectStorage.createAlert(alertObject, operator);

            this.alertObjectStorage.addChangelog(id, ObjectAction.CREATE, operator, "{}", alertObject.getPayload());

            return id;
        });

        alert.setId(alertId);
        alert.setAppName(alertObject.getAppName());
        sendNotification("Alert Created", alert, alert.isEnabled(), UserHolder.getCurrentUser().getUserName());
        return alertId;
    }

    public void updateAlert(Alert newAlert) throws BizException {
        AlertStorageObject oldObject = this.alertObjectStorage.getAlertById(newAlert.getId());
        if (oldObject == null) {
            throw new BizException("Alert object [%s] not exist.", newAlert.getName());
        }

        AlertStorageObject newObject = toAlertObject(newAlert);

        boolean successful = this.alertObjectStorage.executeTransaction(() -> {
            if (!this.alertObjectStorage.updateAlert(oldObject, newObject, UserHolder.getCurrentUser().getUserName())) {
                return false;
            }

            this.alertObjectStorage.addChangelog(newAlert.getId(), ObjectAction.UPDATE, UserHolder.getCurrentUser().getUserName(),
                                                 oldObject.getPayload(),
                                                 newObject.getPayload());
            return true;
        });
        if (!successful) {
            return;
        }

        newAlert.setAppName(newObject.getAppName());
        sendNotification("Alert Updated", newAlert, newAlert.isEnabled(), UserHolder.getCurrentUser().getUserName());
    }

    public void enableAlert(String alertId) throws BizException {
        AlertStorageObject alertObject = this.alertObjectStorage.getAlertById(alertId);
        if (alertObject == null) {
            throw new BizException("Alert object [%s] not exist.", alertId);
        }

        boolean successful = this.alertObjectStorage.executeTransaction(() -> {
            if (!this.alertObjectStorage.enableAlert(alertId, UserHolder.getCurrentUser().getUserName())) {
                return false;
            }
            this.alertObjectStorage.addChangelog(alertId,
                                                 ObjectAction.ENABLE,
                                                 UserHolder.getCurrentUser().getUserName(),
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
            sendNotification("Alert Enabled", oldAlert, true, UserHolder.getCurrentUser().getUserName());
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
            if (!this.alertObjectStorage.disableAlert(alertId, UserHolder.getCurrentUser().getUserName())) {
                return false;
            }
            this.alertObjectStorage.addChangelog(alertId,
                                                 ObjectAction.DISABLE,
                                                 UserHolder.getCurrentUser().getUserName(),
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
            sendNotification("Alert Disabled", oldAlert, false, UserHolder.getCurrentUser().getUserName());
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
            if (!this.alertObjectStorage.deleteAlert(alertId, UserHolder.getCurrentUser().getUserName())) {
                return false;
            }
            this.alertObjectStorage.addChangelog(alertId,
                                                 ObjectAction.DELETE,
                                                 UserHolder.getCurrentUser().getUserName(),
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
            sendNotification("Alert Deleted", oldAlert, oldAlert.isEnabled(), UserHolder.getCurrentUser().getUserName());
        } catch (IOException ignored) {
        }
    }

    private void sendNotification(String title,
                                  Alert alert,
                                  boolean enabled,
                                  String operator) {
    }
}
