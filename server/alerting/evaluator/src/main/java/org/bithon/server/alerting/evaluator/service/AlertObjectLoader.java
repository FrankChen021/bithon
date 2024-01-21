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

package org.bithon.server.alerting.evaluator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bithon.server.alerting.common.model.Alert;
import org.bithon.server.alerting.common.parser.InvalidExpressionException;
import org.bithon.server.alerting.common.utils.Validator;
import org.bithon.server.alerting.evaluator.EvaluatorModuleEnabler;
import org.bithon.server.storage.alerting.IAlertObjectStorage;
import org.bithon.server.storage.alerting.pojo.AlertStorageObject;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.Timestamp;
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
public class AlertObjectLoader {

    private final IAlertObjectStorage alertObjectStorage;
    private final ObjectMapper objectMapper;
    private final Map<String, Alert> loadedAlerts = new ConcurrentHashMap<>();
    private Timestamp lastLoadedAt = new Timestamp(0);

    public AlertObjectLoader(IAlertObjectStorage alertObjectDao,
                             ObjectMapper objectMapper) {
        this.alertObjectStorage = alertObjectDao;
        this.objectMapper = objectMapper;

        loadChanges();
    }

    public Map<String, Alert> getLoadedAlerts() {
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
                        Alert original = this.loadedAlerts.remove(alertObject.getAlertId());
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
                    .map(this::toAlert).filter(Objects::nonNull)
                    .forEach((alert) -> {
                        Alert original = this.loadedAlerts.put(alert.getId(), alert);
                        if (original == null) {
                            this.onCreated(alert);
                        } else {
                            this.onUpdated(original, alert);
                        }
                    });

        this.lastLoadedAt = now;
    }

    private Alert toAlert(AlertStorageObject alarmObject) {
        try {
            Alert alert = objectMapper.readValue(alarmObject.getPayload(), Alert.class);
            alert.setId(alarmObject.getAlertId());
            alert.setName(alarmObject.getAlertName());
            alert.setEnabled(!alarmObject.getDisabled());
            alert.setAppName(alarmObject.getAppName());
            Validator.validate(alert);
            try {
                return alert.initialize();
            } catch (InvalidExpressionException e) {
                log.error("Unable to build alert[{}] due to {}", alert.getName(), e.getMessage());
                return null;
            }
        } catch (IOException e) {
            log.error("Unable to deserialize alarm object Exception: {}\n. Alarm Object: {}\n Stack Trace: {}", e.getMessage(), alarmObject, e);
            return null;
        }
    }

    private void onCreated(Alert alert) {
    }

    private void onUpdated(Alert original, Alert alert) {
    }

    private void onRemoved(Alert alert) {
    }
}
