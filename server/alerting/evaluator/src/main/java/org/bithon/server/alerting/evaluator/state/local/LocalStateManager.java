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
import org.bithon.server.storage.alerting.pojo.AlertState;
import org.bithon.server.storage.alerting.pojo.AlertStatus;

import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
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
    private Map<String, AlertState> alertStates = new ConcurrentHashMap<>();

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

        AlertState alertState = alertStates.computeIfAbsent(ruleId, k -> {
            AlertState state = new AlertState();
            state.setPayload(new AlertState.Payload());
            return state;
        });
        AlertState.Payload payload = alertState.getPayload();

        Map<Label, Long> result = new HashMap<>();

        // Find state for each label, if not found, create a new one
        // If prev state is not in current label list, remove it since it does NOT match current evaluation condition, it needs to re-calculate
        for (Label label : labels) {
            AlertState.SeriesState seriesState = payload.getSeries()
                                                        .computeIfAbsent(label, (k) -> new AlertState.SeriesState());

            // Update match count
            if (now < seriesState.getMatchExpiredAt()) {
                // the status is not expired
                seriesState.setMatchCount(seriesState.getMatchCount() + 1);
            } else {
                seriesState.setMatchCount(1);
            }

            // Update expiration
            seriesState.setMatchExpiredAt(now + duration.toMillis());

            result.put(label, seriesState.getMatchCount());
        }

        return result;
    }

    @Override
    public boolean tryEnterSilence(String ruleId, Label label, Duration silenceDuration) {
        AlertState alertState = alertStates.computeIfAbsent(ruleId, k -> {
            AlertState state = new AlertState();
            state.setPayload(new AlertState.Payload());
            return state;
        });

        AlertState.SeriesState seriesState = alertState.getPayload().getSeries().get(label);
        if (seriesState == null) {
            // SHOULD NOT HAPPEN because above incrMatchCount() should have created the label
            return false;
        }

        if (System.currentTimeMillis() <= seriesState.getSilenceExpiredAt()) {
            // Still in the previous silence period
            return true;
        } else {
            // The previous silence period expires, Set a new one
            long silenceExpiration = System.currentTimeMillis() + silenceDuration.toMillis();
            seriesState.setSilenceExpiredAt(silenceExpiration);
            return false;
        }

        // TODO: remove un-accessed and expired records
    }

    @Override
    public Duration getSilenceRemainTime(String ruleId, Label label) {
        AlertState alertState = alertStates.get(ruleId);
        if (alertState == null) {
            // SHOULD NOT HAPPEN because above incrMatchCount() should have created the label
            return Duration.ZERO;
        }
        AlertState.SeriesState seriesState = alertState.getPayload().getSeries().get(label);
        if (seriesState == null) {
            // SHOULD NOT HAPPEN because above incrMatchCount() should have created the label
            return Duration.ZERO;
        }

        long duration = seriesState.getSilenceExpiredAt() - System.currentTimeMillis();
        return duration < 0 ? Duration.ZERO : Duration.ofMillis(duration);
    }

    @Override
    public void setLastEvaluationTime(String ruleId, long timestamp, Duration interval) {
        AlertState alertState = alertStates.computeIfAbsent(ruleId, k -> {
            AlertState state = new AlertState();
            state.setPayload(new AlertState.Payload());
            return state;
        });

        alertState.getPayload().setLastEvaluationTimestamp(timestamp);
    }

    @Override
    public long getLastEvaluationTimestamp(String ruleId) {
        AlertState alertState = alertStates.get(ruleId);
        if (alertState == null) {
            return 0;
        }
        return alertState.getPayload().getLastEvaluationTimestamp();
    }

    @Override
    public void restoreAlertStates() {
        this.alertStates = new ConcurrentHashMap<>(this.stateStorage.getAlertStates());
    }

    @Override
    public AlertState getAlertState(String alertId) {
        return this.alertStates.computeIfAbsent(alertId, k -> {
            AlertState state = new AlertState();
            state.setPayload(new AlertState.Payload());
            return state;
        });
    }

    @Override
    public void setState(String alertId, AlertStatus status, Map<Label, AlertStatus> seriesStatus) {
        AlertState alertState = alertStates.computeIfAbsent(alertId, k -> {
            AlertState state = new AlertState();
            state.setPayload(new AlertState.Payload());
            return state;
        });

        // Remove entries that aren't in the seriesStatus map
        Iterator<Map.Entry<Label, AlertState.SeriesState>> i = alertState.getPayload()
                                                                         .getSeries()
                                                                         .entrySet()
                                                                         .iterator();
        while (i.hasNext()) {
            Map.Entry<Label, AlertState.SeriesState> entry = i.next();
            Label label = entry.getKey();
            if (!seriesStatus.containsKey(label)
                || entry.getValue().getMatchExpiredAt() < System.currentTimeMillis()
            ) {
                i.remove();
            }
        }

        for (Map.Entry<Label, AlertStatus> series : seriesStatus.entrySet()) {
            // Update status for each label
            AlertState.SeriesState seriesState = alertState.getPayload()
                                                           .getSeries()
                                                           .get(series.getKey());
            if (seriesState == null) {
                // SHOULD be error because for this series, incrMatchCount should have created the label
                continue;
            }
            seriesState.setStatus(series.getValue());
        }
        alertState.setStatus(status);

        // TODO: change declaration to pass array
        this.stateStorage.saveAlertStates(Map.of(alertId, alertState));
    }
}
