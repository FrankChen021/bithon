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

package org.bithon.server.alerting.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bithon.server.alerting.common.evaluator.rule.builder.InvalidRuleExpressionException;
import org.bithon.server.alerting.common.notification.provider.INotificationProvider;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author frankchen
 * @date 2020-08-21 16:22:12
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alert {
    /**
     * 32 bytes UUID. Can be null. If it's null, the server generates a new one
     */
    @JsonIgnore
    @Size(max = 32)
    private String id;

    @JsonIgnore
    @NotBlank
    private String appName;

    @NotBlank
    @JsonIgnore
    private String name;

    /**
     * in minute
     */
    @JsonProperty
    private int evaluationInterval = 1;

    @JsonProperty
    private int matchTimes = 3;

    @JsonProperty
    private int alertEveryNMinutes = 3;

    @JsonProperty
    private List<AlertCondition> conditions;

    @JsonProperty
    private List<AlertCompositeConditions> rules;

    @JsonProperty
    private INotificationProvider[] notifications;

    @JsonProperty
    private boolean enabled = true;

    @JsonIgnore
    private boolean initialized = false;

    public Alert initialize() throws InvalidRuleExpressionException {
        if (initialized) {
            return this;
        }

        final Map<String, AlertCondition> conditionMap = conditions.stream().collect(Collectors.toMap(AlertCondition::getId, cond -> cond));
        for (AlertCompositeConditions rule : rules) {
            rule.initialize(this, conditionMap);
        }
        initialized = true;
        return this;
    }

    /**
     * @param conditionId must start from 'A'
     */
    public AlertCondition getAlertConditionById(String conditionId) {
        int index = conditionId.charAt(0) - 'A';
        return conditions.get(index);
    }
}
