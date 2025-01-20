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

package org.bithon.server.alerting.evaluator.state.local;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import org.bithon.server.alerting.evaluator.state.IEvaluationStateManager;
import org.bithon.server.storage.alerting.IAlertStateStorage;
import org.bithon.server.storage.alerting.Label;
import org.bithon.server.storage.alerting.pojo.AlertStateObject;
import org.bithon.server.storage.alerting.pojo.AlertStatus;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * states are managed in local memory
 *
 * @author Frank Chen
 * @date 12/11/21 4:29 pm
 */
@JsonTypeName("local")
public class LocalStateManager implements IEvaluationStateManager {

    private final IAlertStateStorage stateStorage;
    private Map<String, AlertStateObject> alertStates = new ConcurrentHashMap<>();

    public LocalStateManager(@JacksonInject(useInput = OptBoolean.FALSE) IAlertStateStorage stateStorage) {
        this.stateStorage = stateStorage;
    }

    @Override
    public void resetMatchCount(String alertId) {
        alertStates.remove(alertId);
    }

    @Override
    public Map<Label, Long> incrMatchCount(String ruleId, List<Label> labels, Duration duration) {
        long now = System.currentTimeMillis();

        AlertStateObject.Payload payload = alertStates.computeIfAbsent(ruleId, k -> {
            AlertStateObject state = new AlertStateObject();
            state.setPayload(new AlertStateObject.Payload());
            return state;
        }).getPayload();

        Map<Label, Long> result = new HashMap<>();
        for (Label label : labels) {
            AlertStateObject.StatePerLabel statusPerLabel = payload.getStates()
                                                                   .computeIfAbsent(label, (k) -> new AlertStateObject.StatePerLabel());

            // Update match count
            if (statusPerLabel.getMatchExpiredAt() < now) {
                statusPerLabel.setMatchCount(statusPerLabel.getMatchCount() + 1);
            } else {
                statusPerLabel.setMatchCount(1);
            }

            // Update expiration
            statusPerLabel.setMatchExpiredAt(now + duration.toMillis());

            result.put(label, statusPerLabel.getMatchCount());
        }

        return result;
    }

    @Override
    public boolean tryEnterSilence(String ruleId, Label label, Duration silenceDuration) {
        AlertStateObject.Payload payload = alertStates.computeIfAbsent(ruleId, k -> {
            AlertStateObject state = new AlertStateObject();
            state.setPayload(new AlertStateObject.Payload());
            return state;
        }).getPayload();

        AlertStateObject.StatePerLabel statePerLabel = payload.getStates().get(label);
        if (statePerLabel == null) {
            // SHOULD NOT HAPPEN because above incrMatchCount() should have created the label
            return false;
        }

        if (System.currentTimeMillis() <= statePerLabel.getSilenceExpiredAt()) {
            // Still in the previous silence period
            return true;
        } else {
            // The previous silence period expires, Set a new one
            long silenceExpiration = System.currentTimeMillis() + silenceDuration.toMillis();
            statePerLabel.setSilenceExpiredAt(silenceExpiration);
            return false;
        }

        // TODO: remove un-accessed and expired records
    }

    @Override
    public Duration getSilenceRemainTime(String ruleId, Label label) {
        AlertStateObject stateObject = alertStates.get(ruleId);
        if (stateObject == null) {
            // SHOULD NOT HAPPEN because above incrMatchCount() should have created the label
            return Duration.ZERO;
        }
        AlertStateObject.StatePerLabel statePerLabel = stateObject.getPayload().getStates().get(label);
        if (statePerLabel == null) {
            // SHOULD NOT HAPPEN because above incrMatchCount() should have created the label
            return Duration.ZERO;
        }

        long duration = statePerLabel.getSilenceExpiredAt() - System.currentTimeMillis();
        return duration < 0 ? Duration.ZERO : Duration.ofMillis(duration);
    }

    @Override
    public void setEvaluationTime(String ruleId, long timestamp, Duration interval) {
        AlertStateObject.Payload payload = alertStates.computeIfAbsent(ruleId, k -> {
            AlertStateObject state = new AlertStateObject();
            state.setPayload(new AlertStateObject.Payload());
            return state;
        }).getPayload();

        payload.setEvaluationTimestamp(timestamp);
    }

    @Override
    public long getEvaluationTimestamp(String ruleId) {
        AlertStateObject stateObject = alertStates.get(ruleId);
        if (stateObject == null) {
            return 0;
        }
        return stateObject.getPayload().getEvaluationTimestamp();
    }

    @Override
    public void restoreAlertStates() {
        this.alertStates = new ConcurrentHashMap<>(this.stateStorage.getAlertStates());
    }

    @Override
    public AlertStateObject getAlertState(String alertId) {
        return this.alertStates.computeIfAbsent(alertId, k -> {
            AlertStateObject state = new AlertStateObject();
            state.setPayload(new AlertStateObject.Payload());
            return state;
        });
    }

    @Override
    public void setState(String alertId, AlertStatus status, Map<Label, AlertStatus> allNewStatus) {
        AlertStateObject stateObject = alertStates.computeIfAbsent(alertId, k -> {
            AlertStateObject state = new AlertStateObject();
            state.setPayload(new AlertStateObject.Payload());
            return state;
        });

        for (Map.Entry<Label, AlertStatus> entry : allNewStatus.entrySet()) {
            // Remove expired states for each label
            boolean removed = stateObject.getPayload()
                                         .getStates()
                                         .entrySet()
                                         .removeIf(e -> e.getValue().getMatchExpiredAt() < System.currentTimeMillis());

            if (!removed) {
                AlertStateObject.StatePerLabel statePerLabel = stateObject.getPayload()
                                                                          .getStates()
                                                                          .computeIfAbsent(entry.getKey(), k -> new AlertStateObject.StatePerLabel());
                statePerLabel.setStatus(entry.getValue());
            }
        }
        stateObject.setStatus(status);

        // TODO: apply batch mechanism
        this.stateStorage.saveAlertStates(Map.of(alertId, stateObject));
    }
}
