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

package org.bithon.server.alerting.common.evaluator.state;

import org.bithon.server.alerting.common.evaluator.result.EvaluationOutputs;
import org.bithon.server.storage.alerting.Label;
import org.bithon.server.storage.alerting.pojo.AlertState;
import org.bithon.server.storage.alerting.pojo.AlertStatus;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * states are managed in local memory
 *
 * @author Frank Chen
 * @date 12/11/21 4:29 pm
 */
public class LocalStateManager implements IEvaluationStateManager {
    private final AlertState alertState;

    public LocalStateManager(AlertState prevState) {
        if (prevState == null) {
            this.alertState = new AlertState();
            this.alertState.setStatus(AlertStatus.READY);
            this.alertState.setPayload(new AlertState.Payload());
        } else {
            this.alertState = prevState;

            // Expire old states
            Iterator<Map.Entry<Label, AlertState.SeriesState>> i = alertState.getPayload()
                                                                             .getSeries()
                                                                             .entrySet()
                                                                             .iterator();
            while (i.hasNext()) {
                AlertState.SeriesState state = i.next().getValue();

                long now = System.currentTimeMillis();
                if (state.getMatchExpiredAt() < now) {
                    // The match expired, remove it
                    i.remove();
                    continue;
                }

                if (state.getStatus() == AlertStatus.RESOLVED
                    // If it has been resolved for more than 5 minutes, remove it from the list
                    && (now - state.getResolvedAt()) > 5 * 60 * 1000) {
                    i.remove();
                }
            }
        }
    }

    @Override
    public Map<Label, Long> setMatches(Collection<Label> labels, Duration duration) {
        long now = System.currentTimeMillis();

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
    public boolean tryEnterSilence(Label label, Duration silenceDuration) {
        AlertState.SeriesState seriesState = this.alertState.getPayload().getSeries().get(label);
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
    public Duration getSilenceRemainTime(Label label) {
        AlertState.SeriesState seriesState = this.alertState.getPayload().getSeries().get(label);
        if (seriesState == null) {
            // SHOULD NOT HAPPEN because above incrMatchCount() should have created the label
            return Duration.ZERO;
        }

        long duration = seriesState.getSilenceExpiredAt() - System.currentTimeMillis();
        return duration < 0 ? Duration.ZERO : Duration.ofMillis(duration);
    }

    @Override
    public void setLastEvaluationTime(long timestamp, Duration interval) {
        alertState.setLastEvaluatedAt(new Timestamp(timestamp).toLocalDateTime());
    }

    @Override
    public long getLastEvaluationTimestamp() {
        return this.alertState.getLastEvaluatedAt() == null ? 0L : Timestamp.valueOf(this.alertState.getLastEvaluatedAt()).getTime();
    }

    @Override
    public AlertState updateState(AlertStatus status,
                                  Map<Label, EvaluationOutputs> newStates) {

        // Remove entries that aren't in the newStates map
        Iterator<Map.Entry<Label, AlertState.SeriesState>> i = alertState.getPayload()
                                                                         .getSeries()
                                                                         .entrySet()
                                                                         .iterator();
        while (i.hasNext()) {
            Map.Entry<Label, AlertState.SeriesState> entry = i.next();
            Label label = entry.getKey();
            AlertState.SeriesState oldState = entry.getValue();

            EvaluationOutputs newState = newStates.get(label);
            if (newState == null) {
                // no new state for this label, remove it
                i.remove();
            }
        }

        for (Map.Entry<Label, EvaluationOutputs> newState : newStates.entrySet()) {
            // Update status for each label
            AlertState.SeriesState oldState = alertState.getPayload()
                                                        .getSeries()
                                                        .get(newState.getKey());
            if (oldState == null) {
                // SHOULD be error because for this newState, incrMatchCount should have created the label
                continue;
            }

            if (newState.getValue().getStatus() == AlertStatus.RESOLVED && newState.getValue().getStatus() != oldState.getStatus()) {
                oldState.setResolvedAt(System.currentTimeMillis());
            }

            oldState.setStatus(newState.getValue().getStatus());
        }
        alertState.setStatus(status);

        return alertState;
    }

    @Override
    public LocalDateTime getLastAlertAt() {
        return alertState.getLastAlertAt();
    }

    @Override
    public AlertStatus getStatusByLabel(Label label) {
        return this.alertState.getStatusByLabel(label);
    }

    @Override
    public void setLastRecordId(String recordId) {
        this.alertState.setLastRecordId(recordId);
        this.alertState.setLastAlertAt(new Timestamp(System.currentTimeMillis()).toLocalDateTime());
    }

    @Override
    public String getLastRecordId() {
        return this.alertState.getLastRecordId();
    }

    @Override
    public Map<Label, AlertState.SeriesState> getSeriesState() {
        return alertState.getPayload().getSeries();
    }
}
