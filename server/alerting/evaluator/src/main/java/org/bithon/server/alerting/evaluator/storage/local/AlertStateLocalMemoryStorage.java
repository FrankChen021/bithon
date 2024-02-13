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

    @JsonCreator
    public AlertStateLocalMemoryStorage(@JacksonInject(useInput = OptBoolean.FALSE) AlertRepository alertRepository) {
        alertRepository.addListener(new IAlertChangeListener() {
            @Override
            public void onCreated(AlertRule alert) {
                matchCounters.remove(alert.getId());
                silenced.remove(alert.getId());
            }

            @Override
            public void onUpdated(AlertRule original, AlertRule updated) {
                matchCounters.remove(original.getId());
                silenced.remove(original.getId());
            }

            @Override
            public void onRemoved(AlertRule alert) {
                matchCounters.remove(alert.getId());
                silenced.remove(alert.getId());
            }
        });
    }

    @Override
    public void resetMatchCount(String alertId) {
        matchCounters.remove(alertId);
    }

    @Override
    public long incrMatchCount(String alertId, Duration duration) {
        return matchCounters.computeIfAbsent(alertId, k -> new AtomicInteger())
                            .incrementAndGet();
    }

    @Override
    public boolean tryEnterSilence(String alertId, Duration silenceDuration) {
        long silencedTill = System.currentTimeMillis() + silenceDuration.toMillis();
        if (silenced.putIfAbsent(alertId, silencedTill) == null) {
            return false;
        }
        silencedTill = silenced.get(alertId);
        if (System.currentTimeMillis() <= silencedTill) {
            return true;
        } else {
            silenced.put(alertId, silencedTill);
            return false;
        }
    }

    @Override
    public Duration getSilenceRemainTime(String alertId) {
        long silencedTill = silenced.get(alertId);
        return Duration.ofMillis(silencedTill - System.currentTimeMillis());
    }
}
