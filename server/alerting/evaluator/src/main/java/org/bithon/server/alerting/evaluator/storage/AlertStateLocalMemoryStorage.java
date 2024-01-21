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

package org.bithon.server.alerting.evaluator.storage;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.bithon.server.storage.alerting.IAlertStateStorage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Frank Chen
 * @date 12/11/21 4:29 pm
 */
@JsonTypeName("local")
public class AlertStateLocalMemoryStorage implements IAlertStateStorage {
    private final Map<String, AtomicInteger> triggerCounters = new ConcurrentHashMap<>();
    private final Map<String, Long> silenced = new ConcurrentHashMap<>();

    @Override
    public void resetTriggerMatchCount(String alertId, String trigger) {
        triggerCounters.remove(trigger + "@" + alertId);
    }

    @Override
    public long incrTriggerMatchCount(String alertId, String trigger) {
        String id = trigger + "@" + alertId;
        return triggerCounters.computeIfAbsent(id, k -> new AtomicInteger())
                              .incrementAndGet();
    }

    @Override
    public boolean tryEnterSilence(String alertId, int silencePeriod) {
        long silencedTill = System.currentTimeMillis() + silencePeriod * 60_000L;
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
    public long getSilenceRemainTime(String alertId) {
        long silencedTill = silenced.get(alertId);
        return silencedTill - System.currentTimeMillis();
    }
}
