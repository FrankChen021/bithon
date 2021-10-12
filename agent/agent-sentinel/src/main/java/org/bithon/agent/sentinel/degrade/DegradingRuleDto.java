/*
 *    Copyright 2020 bithon.cn
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

package org.bithon.agent.sentinel.degrade;

import org.bithon.agent.sentinel.expt.ParamInvalidException;
import org.bithon.agent.sentinel.expt.ParamNullException;
import shaded.com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;

import java.util.Objects;

/**
 * @author frankchen
 */
public class DegradingRuleDto {
    private String ruleId;
    private String uri;
    private int minRequestAmount = 5;

    /**
     * 降级时长(秒)
     */
    private Integer timeWindow;

    /**
     * 降级策略。0:慢响应比例; 1:异常比例; 2:异常请求数
     */
    private Integer grade;

    private Double threshold;

    /**
     * 最大响应时间。仅当 降级策略 为 慢响应比例 时，该字段有效
     */
    private Integer maxResponseTime;

    /**
     * 统计时长(毫秒)
     */
    private Integer statIntervalMs;
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

    public int getMinRequestAmount() {
        return minRequestAmount;
    }

    public void setMinRequestAmount(int minRequestAmount) {
        this.minRequestAmount = minRequestAmount;
    }

    public Integer getTimeWindow() {
        return timeWindow;
    }

    public void setTimeWindow(Integer timeWindow) {
        this.timeWindow = timeWindow;
    }

    public Integer getGrade() {
        return grade;
    }

    public void setGrade(Integer grade) {
        this.grade = grade;
    }

    public Double getThreshold() {
        return threshold;
    }

    public void setThreshold(Double threshold) {
        this.threshold = threshold;
    }

    public Integer getMaxResponseTime() {
        return maxResponseTime;
    }

    public void setMaxResponseTime(Integer maxResponseTime) {
        this.maxResponseTime = maxResponseTime;
    }

    public Integer getStatIntervalMs() {
        return statIntervalMs;
    }

    public void setStatIntervalMs(Integer statIntervalMs) {
        this.statIntervalMs = statIntervalMs;
    }

    public DegradeRule toDegradeRule() {
        DegradeRule degradeRule = new DegradeRule();
        degradeRule.setResource(this.getUri());
        degradeRule.setCount(this.getThreshold());
        degradeRule.setGrade(this.getGrade());
        degradeRule.setMinRequestAmount(this.getMinRequestAmount());
        if (this.getGrade() == 0) {
            degradeRule.setCount(this.getMaxResponseTime());
            degradeRule.setSlowRatioThreshold(this.getThreshold());
        } else {
            degradeRule.setCount(this.getThreshold());
        }
        degradeRule.setTimeWindow(this.getTimeWindow());
        degradeRule.setStatIntervalMs(this.getStatIntervalMs());
        return degradeRule;
    }

    public void valid() {
        ParamNullException.throwIf(ruleId == null, "ruleId");
        ParamNullException.throwIf(uri == null, "uri");

        ParamNullException.throwIf(timeWindow == null, "timeWindow");
        ParamInvalidException.throwIf(timeWindow < 0, "timeWindow", timeWindow);

        ParamNullException.throwIf(grade == null, "grade");
        ParamInvalidException.throwIf(grade < 0 || grade > 2, "grade", grade);

        ParamNullException.throwIf(threshold == null, "threshold");
        ParamInvalidException.throwIf(threshold <= 0, "threshold", threshold);

        ParamNullException.throwIf(grade == 0 && maxResponseTime == null, "maxResponseTime");
        ParamInvalidException.throwIf(grade == 0 && maxResponseTime <= 0,
                                      "maxResponseTime",
                                      maxResponseTime);

        ParamNullException.throwIf(statIntervalMs == null, "statIntervalMs");
        ParamInvalidException.throwIf(statIntervalMs <= 0, "statIntervalMs", statIntervalMs);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DegradingRuleDto that = (DegradingRuleDto) o;
        return minRequestAmount == that.minRequestAmount
               && Objects.equals(ruleId, that.ruleId)
               && Objects.equals(uri, that.uri)
               && Objects.equals(timeWindow, that.timeWindow)
               && Objects.equals(grade, that.grade)
               && Objects.equals(threshold, that.threshold)
               && Objects.equals(maxResponseTime, that.maxResponseTime)
               && Objects.equals(statIntervalMs, that.statIntervalMs)
               && Objects.equals(lastOperator, that.lastOperator)
               && Objects.equals(lastUpdatedAt, that.lastUpdatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ruleId,
                            uri,
                            minRequestAmount,
                            timeWindow,
                            grade,
                            threshold,
                            maxResponseTime,
                            statIntervalMs,
                            lastOperator,
                            lastUpdatedAt);
    }

    @Override
    public String toString() {
        return "DegradeRuleDto{" +
               "ruleId='" + ruleId + '\'' +
               ", uri='" + uri + '\'' +
               ", minRequestAmount=" + minRequestAmount +
               ", timeWindow=" + timeWindow +
               ", grade=" + grade +
               ", threshold=" + threshold +
               ", maxResponseTime=" + maxResponseTime +
               ", statIntervalMs=" + statIntervalMs +
               ", lastOperator='" + lastOperator + '\'' +
               ", lastUpdatedAt='" + lastUpdatedAt + '\'' +
               '}';
    }
}
