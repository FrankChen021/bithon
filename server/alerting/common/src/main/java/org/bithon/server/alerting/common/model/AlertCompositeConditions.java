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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.bithon.server.alerting.common.evaluator.rule.IAlertRuleEvaluator;
import org.bithon.server.alerting.common.evaluator.rule.builder.RuleEvaluatorBuildResult;
import org.bithon.server.alerting.common.evaluator.rule.builder.RuleEvaluatorBuilder;

import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/11/30 6:51 下午
 */
@Getter
public class AlertCompositeConditions {
    @NotEmpty
    private final String expression;

    @NotNull
    private final AlertSeverity severity;

    private final boolean enabled;

    @JsonIgnore
    private IAlertRuleEvaluator evaluator;

    /**
     * runtime property that contains id of all conditions in the rule expression
     */
    @JsonIgnore
    private List<String> conditions;

    @JsonCreator
    public AlertCompositeConditions(@JsonProperty("expression") String expression,
                                    @JsonProperty("severity") AlertSeverity severity,
                                    @JsonProperty("enabled") @Nullable Boolean enabled) {
        this.expression = expression;
        this.severity = severity;
        this.enabled = enabled == null || enabled;
    }

    public void initialize(Alert alert, Map<String, AlertCondition> conditions) {
        RuleEvaluatorBuildResult result = RuleEvaluatorBuilder.build(alert, conditions, this.expression);
        this.evaluator = result.getEvaluator();
        this.conditions = result.getConditions();
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression, severity, enabled);
    }

    @Override
    public boolean equals(Object that) {
        if (that instanceof AlertCompositeConditions) {
            AlertCompositeConditions rhs = (AlertCompositeConditions) that;
            return this.expression.equals(rhs.expression)
                   && this.severity.equals(rhs.severity)
                   && this.enabled == rhs.enabled;
        } else {
            return false;
        }
    }
}
