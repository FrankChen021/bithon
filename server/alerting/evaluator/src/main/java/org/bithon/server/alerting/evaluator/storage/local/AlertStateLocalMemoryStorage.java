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

package org.bithon.server.alerting.evaluator.storage.local;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import org.bithon.server.alerting.common.model.AlertRule;
import org.bithon.server.alerting.evaluator.repository.AlertRepository;
import org.bithon.server.alerting.evaluator.repository.IAlertChangeListener;
import org.bithon.server.storage.alerting.IAlertStateStorage;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Frank Chen
 * @date 12/11/21 4:29 pm
 */
@JsonTypeName("local")
public class AlertStateLocalMemoryStorage implements IAlertStateStorage {
    private final Map<String, AtomicInteger> matchCounters = new ConcurrentHashMap<>();
    private final Map<String, Long> silenced = new ConcurrentHashMap<>();
    private final Map<String, Long> evaluationTime = new ConcurrentHashMap<>();

    public AlertStateLocalMemoryStorage() {
    }

    @JsonCreator
    public AlertStateLocalMemoryStorage(@JacksonInject(useInput = OptBoolean.FALSE) AlertRepository alertRepository) {
        alertRepository.addListener(new IAlertChangeListener() {
            @Override
            public void onLoaded(AlertRule rule) {
                matchCounters.remove(rule.getId());
                silenced.remove(rule.getId());
                evaluationTime.remove(rule.getId());
            }

            @Override
            public void onUpdated(AlertRule original, AlertRule updated) {
                matchCounters.remove(original.getId());
                silenced.remove(original.getId());
                evaluationTime.remove(original.getId());
            }

            @Override
            public void onRemoved(AlertRule rule) {
                matchCounters.remove(rule.getId());
                silenced.remove(rule.getId());
                evaluationTime.remove(rule.getId());
            }
        });
    }

    @Override
    public void resetMatchCount(String ruleId) {
        matchCounters.remove(ruleId);
    }

    @Override
    public long incrMatchCount(String ruleId, Duration duration) {
        return matchCounters.computeIfAbsent(ruleId, k -> new AtomicInteger())
                            .incrementAndGet();
    }

    @Override
    public boolean tryEnterSilence(String ruleId, Duration silenceDuration) {
        long silenceExpiration = System.currentTimeMillis() + silenceDuration.toMillis();
        Long prevExpirationTimestamp = silenced.putIfAbsent(ruleId, silenceExpiration);
        if (prevExpirationTimestamp == null) {
            // No records, means that this is the first time to enter
            return false;
        }

        if (System.currentTimeMillis() <= prevExpirationTimestamp) {
            // Still in the previous silence period
            return true;
        } else {
            // The previous silence period expires, Set a new one
            silenced.put(ruleId, silenceExpiration);
            return false;
        }
    }

    @Override
    public Duration getSilenceRemainTime(String ruleId) {
        long silencedTill = silenced.getOrDefault(ruleId, 0L);
        if (silencedTill == 0) {
            return Duration.ZERO;
        }
        long duration = silencedTill - System.currentTimeMillis();
        return duration < 0 ? Duration.ZERO : Duration.ofMillis(duration);
    }

    @Override
    public void setEvaluationTime(String ruleId, long timestamp, Duration interval) {
        this.evaluationTime.put(ruleId, timestamp);
    }

    @Override
    public long getEvaluationTimestamp(String ruleId) {
        return this.evaluationTime.getOrDefault(ruleId, 0L);
    }
}
