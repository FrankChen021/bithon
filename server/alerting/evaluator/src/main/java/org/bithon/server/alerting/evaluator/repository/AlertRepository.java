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

package org.bithon.server.alerting.evaluator.repository;

import lombok.extern.slf4j.Slf4j;
import org.bithon.server.alerting.common.model.AlertRule;
import org.bithon.server.alerting.common.parser.InvalidExpressionException;
import org.bithon.server.alerting.common.utils.Validator;
import org.bithon.server.alerting.evaluator.EvaluatorModuleEnabler;
import org.bithon.server.storage.alerting.IAlertObjectStorage;
import org.bithon.server.storage.alerting.pojo.AlertStorageObject;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/8 5:27 下午
 */
@Slf4j
@Service
@Conditional(EvaluatorModuleEnabler.class)
public class AlertRepository {

    private final IAlertObjectStorage alertObjectStorage;
    private final Map<String, AlertRule> loadedAlerts = new ConcurrentHashMap<>();
    private final List<IAlertChangeListener> changeListeners = Collections.synchronizedList(new ArrayList<>());
    private Timestamp lastLoadedAt = new Timestamp(0);

    public AlertRepository(IAlertObjectStorage alertObjectDao) {
        this.alertObjectStorage = alertObjectDao;

        loadChanges();
    }

    public Map<String, AlertRule> getLoadedAlerts() {
        return this.loadedAlerts;
    }

    public void loadChanges() {
        Timestamp lastTimestamp = lastLoadedAt;
        Timestamp now = new Timestamp(System.currentTimeMillis());

        log.info("Loading Alerts from [{}, {}]", lastTimestamp, now);

        List<AlertStorageObject> alertObjects = alertObjectStorage.getAlertListByTime(lastTimestamp, now);

        alertObjects.stream()
                    .filter(AlertStorageObject::getDeleted)
                    .forEach((alertObject) -> {
                        AlertRule original = this.loadedAlerts.remove(alertObject.getId());
                        if (original != null) {
                            log.info("Remove Alerts [{}]{}", original.getId(), original.getName());
                            this.onRemoved(original);
                        }
                    });

        //
        // 从Payload获取告警配置
        //
        alertObjects.stream()
                    .filter(alert -> !alert.getDeleted())
                    .map(this::toAlert)
                    .filter(Objects::nonNull)
                    .forEach((alert) -> {
                        AlertRule oldRule = this.loadedAlerts.put(alert.getId(), alert);
                        if (oldRule == null) {
                            this.onCreated(alert);
                        } else {
                            this.onUpdated(oldRule, alert);
                        }
                    });

        this.lastLoadedAt = now;
    }

    private AlertRule toAlert(AlertStorageObject alertObject) {
        AlertRule alertRule = AlertRule.from(alertObject);
        Validator.validate(alertRule);
        try {
            return alertRule.initialize();
        } catch (InvalidExpressionException e) {
            log.error("Unable to build alert[{}] due to {}", alertRule.getName(), e.getMessage());
            return null;
        }
    }

    public void addListener(IAlertChangeListener listener) {
        this.changeListeners.add(listener);
    }

    private void onCreated(AlertRule alertRule) {
        IAlertChangeListener[] listeners = this.changeListeners.toArray(new IAlertChangeListener[0]);
        for (IAlertChangeListener listener : listeners) {
            listener.onCreated(alertRule);
        }
    }

    private void onUpdated(AlertRule oldRule, AlertRule newRule) {
        IAlertChangeListener[] listeners = this.changeListeners.toArray(new IAlertChangeListener[0]);
        for (IAlertChangeListener listener : listeners) {
            listener.onUpdated(oldRule, newRule);
        }
    }

    private void onRemoved(AlertRule alertRule) {
        IAlertChangeListener[] listeners = this.changeListeners.toArray(new IAlertChangeListener[0]);
        for (IAlertChangeListener listener : listeners) {
            listener.onRemoved(alertRule);
        }
    }
}
