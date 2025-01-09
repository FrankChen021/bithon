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

package org.bithon.server.alerting.evaluator.state.redis;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.alerting.common.model.AlertRule;
import org.bithon.server.alerting.evaluator.repository.AlertRepository;
import org.bithon.server.alerting.evaluator.repository.IAlertChangeListener;
import org.bithon.server.alerting.evaluator.state.IEvaluationStateManager;
import org.bithon.server.storage.alerting.Label;
import org.bithon.server.storage.alerting.pojo.AlertStateObject;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * States are managed in redis
 *
 * @author frank.chen021@outlook.com
 * @date 2020/12/14 2:51 下午
 */
@JsonTypeName("redis")
public class RedisStateManager implements IEvaluationStateManager {

    private final RedisClient redisClient;

    @JsonCreator
    public RedisStateManager(@JsonProperty("props") RedisConfig props,
                             @JacksonInject(useInput = OptBoolean.FALSE) AlertRepository alertRepository) {

        this.redisClient = RedisClientFactory.create(props);

        if (alertRepository != null) {
            alertRepository.addListener(new IAlertChangeListener() {
                @Override
                public void onLoaded(AlertRule rule) {
                }

                @Override
                public void onUpdated(AlertRule original, AlertRule updated) {
                    if (original.getNotificationProps().getSilence().equals(updated.getNotificationProps().getSilence())) {
                        // The silence period has been changed
                        try {
                            redisClient.delete(getAlertKey(original.getId(), "silence"));
                        } catch (Exception ignored) {
                        }
                    }
                    try {
                        redisClient.delete(getAlertKey(original.getId(), "*"));
                    } catch (Exception ignored) {
                    }
                }

                @Override
                public void onRemoved(AlertRule rule) {
                    try {
                        redisClient.delete(getAlertKey(rule.getId(), "silence"));
                    } catch (Exception ignored) {
                    }
                    try {
                        redisClient.delete(getAlertKey(rule.getId(), "*"));
                    } catch (Exception ignored) {
                    }
                }
            });
        }
    }

    @Override
    public void resetMatchCount(String alertId) {
        redisClient.delete(getAlertKey(alertId));
    }

    @Override
    public Map<Label, Long> incrMatchCount(String alertId, List<Label> series, Duration duration) {
        Map<Label, Long> ret = new HashMap<>();

        for (Label label : series) {
            String key = getAlertKey(alertId, label.getId());
            long v = redisClient.increment(key);
            redisClient.expire(key, duration);

            ret.put(label, v);
        }

        return ret;
    }

    @Override
    public boolean tryEnterSilence(String alertId, Label label, Duration silencePeriod) {
        return redisClient.setIfAbsent(getAlertKey(alertId, "silence"), "silence", silencePeriod);
    }

    @Override
    public Duration getSilenceRemainTime(String alertId, Label label) {
        return redisClient.getExpire(getAlertKey(alertId, "silence"));
    }

    @Override
    public void setEvaluationTime(String alertId, long timestamp, Duration interval) {
        redisClient.set(getAlertKey(alertId, "evaluation"), String.valueOf(timestamp), interval);
    }

    @Override
    public long getEvaluationTimestamp(String alertId) {
        String val = redisClient.get(getAlertKey(alertId, "evaluation"));
        if (val != null) {
            try {
                return Long.parseLong(val);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    @Override
    public Map<String, AlertStateObject> exportAlertStates() {
        return Map.of();
    }

    @Override
    public void restoreAlertStates(Map<String, AlertStateObject> alertStates) {
    }

    private String getAlertKey(String alertId) {
        return StringUtils.format("bithon-alerting:%s", alertId);
    }

    private String getAlertKey(String alertId, String subKey) {
        return StringUtils.format("bithon-alerting:%s:%s", alertId, subKey);
    }
}
