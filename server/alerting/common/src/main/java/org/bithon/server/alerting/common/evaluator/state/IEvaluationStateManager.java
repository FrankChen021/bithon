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

import org.bithon.server.storage.alerting.Label;
import org.bithon.server.storage.alerting.pojo.AlertState;
import org.bithon.server.storage.alerting.pojo.AlertStatus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/14 2:51 下午
 */
public interface IEvaluationStateManager {

    /**
     * @param duration The duration of an alert rule
     * @return The successive match count of each Label
     */
    Map<Label, Long> setMatches(Collection<Label> series, Duration duration);

    /**
     * Check if there's an alert sending out in the past {@param silencePeriod} minutes.
     */
    boolean tryEnterSilence(Label label, Duration silenceDuration);

    Duration getSilenceRemainTime(Label label);

    /**
     * Set the last evaluation time of an alert.
     *
     * @param timestamp the timestamp when the alert is evaluated
     * @param interval  the interval of two consecutive evaluations
     */
    void setLastEvaluationTime(long timestamp, Duration interval);

    /**
     * Get the timestamp when the alert is evaluated last time in milliseconds
     */
    long getLastEvaluationTimestamp();

    AlertState updateState(AlertStatus status,
                           Map<Label, AlertStatus> seriesStatus);

    /**
     * @return the timestamp or NULL
     */
    LocalDateTime getLastAlertAt();

    /**
     * @return If the label is not found, it returns {@link AlertStatus#READY}
     */
    AlertStatus getStatusByLabel(Label label);

    void setLastRecordId(String recordId);

    /**
     * @return Nullable
     */
    String getLastRecordId();

    Map<Label, AlertState.SeriesState> getSeriesState();
}
