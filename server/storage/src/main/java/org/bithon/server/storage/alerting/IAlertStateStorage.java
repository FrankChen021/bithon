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

package org.bithon.server.storage.alerting;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Duration;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/14 2:51 下午
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface IAlertStateStorage {

    void resetMatchCount(String alertId);

    /**
     * @param duration The duration of an alert rule
     */
    long incrMatchCount(String alertId, Duration duration);

    /**
     * Check if there's an alert sending out in the past {@param silencePeriod} minutes.
     */
    boolean tryEnterSilence(String alertId, Duration silenceDuration);

    Duration getSilenceRemainTime(String alertId);

    /**
     * Set the last evaluation time of an alert.
     * @param timestamp the timestamp when the alert is evaluated
     * @param interval the interval of two consecutive evaluations
     */
    void setEvaluationTime(String alertId, long timestamp, Duration interval);

    /**
     * Get the timestamp when the alert is evaluated last time in milliseconds
     */
    long getEvaluationTimestamp(String alertId);
}
