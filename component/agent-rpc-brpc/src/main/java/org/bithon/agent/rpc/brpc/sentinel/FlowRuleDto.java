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

package org.bithon.agent.rpc.brpc.sentinel;

import org.bithon.component.commons.utils.Preconditions;

import java.util.Objects;

/**
 * @author frankchen
 */
public class FlowRuleDto {
    private String ruleId;
    private String uri;
    private Integer grade = 1;
    private Integer controlBehavior;
    private Integer maxTimeoutMs;
    private Double threshold;
    private String lastOperator;
    private String lastUpdatedAt;

    public String getLastOperator() {
        return lastOperator;
    }

    public void setLastOperator(String lastOperator) {
        this.lastOperator = lastOperator;
    }

    public String getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(String lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public Integer getGrade() {
        return grade;
    }

    public void setGrade(int grade) {
        this.grade = grade;
    }

    public Double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public Integer getControlBehavior() {
        return controlBehavior;
    }

    public void setControlBehavior(int controlBehavior) {
        this.controlBehavior = controlBehavior;
    }

    public Integer getMaxTimeoutMs() {
        return maxTimeoutMs;
    }

    public void setMaxTimeoutMs(int maxTimeoutMs) {
        this.maxTimeoutMs = maxTimeoutMs;
    }

    public void valid() {
        Preconditions.checkNotNull(ruleId, "ruleId");
        Preconditions.checkNotNull(uri, "uri");
        Preconditions.checkNotNull(grade, "grade");

        Preconditions.checkNotNull(controlBehavior == null, "controlBehavior");
        Preconditions.throwIf(controlBehavior < 0 || controlBehavior > 3, "controlBehavior", controlBehavior);

        Preconditions.checkNotNull(maxTimeoutMs, "maxTimeoutMs");
        Preconditions.throwIf(controlBehavior == 2 && maxTimeoutMs <= 0, "maxTimeoutMs", maxTimeoutMs);

        Preconditions.checkNotNull(threshold, "threshold");
        Preconditions.throwIf(threshold <= 0, "threshold", threshold);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FlowRuleDto that = (FlowRuleDto) o;
        return Objects.equals(ruleId, that.ruleId)
            && Objects.equals(uri, that.uri)
            && Objects.equals(grade, that.grade)
            && Objects.equals(controlBehavior, that.controlBehavior)
            && Objects.equals(maxTimeoutMs, that.maxTimeoutMs)
            && Objects.equals(threshold, that.threshold)
            && Objects.equals(lastOperator, that.lastOperator)
            && Objects.equals(lastUpdatedAt, that.lastUpdatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ruleId, uri, grade, controlBehavior, maxTimeoutMs, threshold, lastOperator, lastUpdatedAt);
    }

    @Override
    public String toString() {
        return "FlowRuleDto{" +
            "ruleId='" + ruleId + '\'' +
            ", uri='" + uri + '\'' +
            ", grade=" + grade +
            ", controlBehavior=" + controlBehavior +
            ", maxTimeoutMs=" + maxTimeoutMs +
            ", threshold=" + threshold +
            ", lastOperator='" + lastOperator + '\'' +
            ", lastUpdatedAt='" + lastUpdatedAt + '\'' +
            '}';
    }
}
