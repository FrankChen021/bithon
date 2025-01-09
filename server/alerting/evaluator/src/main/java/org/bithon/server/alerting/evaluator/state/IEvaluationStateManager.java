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

package org.bithon.server.alerting.evaluator.state;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.bithon.server.storage.alerting.Label;
import org.bithon.server.storage.alerting.pojo.AlertStateObject;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/14 2:51 下午
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface IEvaluationStateManager {

    void resetMatchCount(String alertId);

    /**
     * @param duration The duration of an alert rule
     */
    Map<Label, Long> incrMatchCount(String alertId, List<Label> series, Duration duration);

    /**
     * Check if there's an alert sending out in the past {@param silencePeriod} minutes.
     */
    boolean tryEnterSilence(String alertId, Label label, Duration silenceDuration);

    Duration getSilenceRemainTime(String alertId, Label label);

    /**
     * Set the last evaluation time of an alert.
     *
     * @param timestamp the timestamp when the alert is evaluated
     * @param interval  the interval of two consecutive evaluations
     */
    void setEvaluationTime(String alertId, long timestamp, Duration interval);

    /**
     * Get the timestamp when the alert is evaluated last time in milliseconds
     */
    long getEvaluationTimestamp(String alertId);

    /**
     * Two interfaces that export and import alert states from an external system
     */
    Map<String, AlertStateObject> exportAlertStates();

    void restoreAlertStates(Map<String, AlertStateObject> alertStates);
}
