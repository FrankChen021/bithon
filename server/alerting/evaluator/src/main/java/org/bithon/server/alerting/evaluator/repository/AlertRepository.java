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
import org.bithon.component.commons.expression.expt.InvalidExpressionException;
import org.bithon.server.alerting.common.model.AlertRule;
import org.bithon.server.alerting.common.utils.Validator;
import org.bithon.server.alerting.evaluator.EvaluatorModuleEnabler;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.alerting.IAlertObjectStorage;
import org.bithon.server.storage.alerting.pojo.AlertStorageObject;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

    public AlertRepository(IAlertObjectStorage alertObjectStorage) {
        this.alertObjectStorage = alertObjectStorage;
    }

    public Map<String, AlertRule> getLoadedAlerts() {
        return this.loadedAlerts;
    }

    public void loadChanges() {
        Timestamp lastTimestamp = lastLoadedAt;
        Timestamp now = TimeSpan.now().ceil(Duration.ofSeconds(1)).toTimestamp();

        log.info("Loading Alerts from [{}, {}]", lastTimestamp, now);

        List<AlertStorageObject> alertObjects = alertObjectStorage.getAlertListByTime(lastTimestamp, now);

        alertObjects.forEach((alertObject) -> {
            if (alertObject.isDeleted()) {
                AlertRule original = this.loadedAlerts.remove(alertObject.getId());
                if (original != null) {
                    log.info("Remove Alerts [{}]{}", original.getId(), original.getName());
                    this.onRemoved(original);
                }
            } else {
                AlertRule newRule = toAlert(alertObject);
                if (newRule != null) {
                    AlertRule oldRule = this.loadedAlerts.put(newRule.getId(), newRule);
                    if (oldRule == null) {
                        this.onLoaded(newRule);
                    } else {
                        this.onUpdated(oldRule, newRule);
                    }
                }
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

    private void onLoaded(AlertRule alertRule) {
        IAlertChangeListener[] listeners = this.changeListeners.toArray(new IAlertChangeListener[0]);
        for (IAlertChangeListener listener : listeners) {
            try {
                listener.onLoaded(alertRule);
            } catch (Exception e) {
                log.info("Exception when notify onCreated", e);
            }
        }
    }

    private void onUpdated(AlertRule oldRule, AlertRule newRule) {
        IAlertChangeListener[] listeners = this.changeListeners.toArray(new IAlertChangeListener[0]);
        for (IAlertChangeListener listener : listeners) {
            try {
                listener.onUpdated(oldRule, newRule);
            } catch (Exception e) {
                log.info("Exception when notify onUpdated", e);
            }
        }
    }

    private void onRemoved(AlertRule alertRule) {
        IAlertChangeListener[] listeners = this.changeListeners.toArray(new IAlertChangeListener[0]);
        for (IAlertChangeListener listener : listeners) {
            try {
                listener.onRemoved(alertRule);
            } catch (Exception e) {
                log.info("Exception when notify onRemoved", e);
            }
        }
    }
}
