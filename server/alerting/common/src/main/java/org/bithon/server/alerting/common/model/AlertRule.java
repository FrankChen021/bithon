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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bithon.component.commons.expression.ComparisonExpression;
import org.bithon.component.commons.expression.ConditionalExpression;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IExpressionInDepthVisitor;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.expt.InvalidExpressionException;
import org.bithon.component.commons.utils.HumanReadableDuration;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.alerting.common.parser.AlertExpressionASTParser;
import org.bithon.server.storage.alerting.pojo.AlertStorageObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The object during the evaluation
 *
 * @author frankchen
 * @date 2020-08-21 16:22:12
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRule {
    /**
     * 32 bytes UUID. Can be null. If it's null, the server generates a new one
     */
    @Size(max = 32)
    private String id;

    private String appName;

    @NotBlank
    private String name;


    @JsonProperty
    private String expr;

    /**
     * in minutes
     */
    @JsonProperty
    private HumanReadableDuration every = HumanReadableDuration.DURATION_1_MINUTE;

    /**
     * How many consecutive times the evaluation should be true before notifying the alert
     */
    @JsonProperty("for")
    private int forTimes = 3;

    /**
     * silence period in minute
     */
    @JsonProperty
    private HumanReadableDuration silence = HumanReadableDuration.DURATION_3_MINUTE;

    @JsonProperty
    private List<String> notifications;

    @JsonIgnore
    private boolean enabled = true;

    @JsonIgnore
    private IExpression evaluationExpression;

    @JsonIgnore
    private Map<String, AlertExpression> flattenExpressions;

    @JsonIgnore
    public int getExpectedMatchCount() {
        return this.forTimes;
    }

    public AlertRule initialize() throws InvalidExpressionException {
        if (evaluationExpression != null) {
            return this;
        }

        Preconditions.checkIfTrue(!StringUtils.isEmpty(expr), "There must be at least one expression in the alert [%s]", this.name);

        this.evaluationExpression = AlertExpressionASTParser.parse(this.expr);
        if (StringUtils.isBlank(this.appName)) {
            this.appName = new ApplicationNameExtractor().extract(this.evaluationExpression);
        }

        // Use LinkedHashMap to keep order
        this.flattenExpressions = new LinkedHashMap<>();

        this.evaluationExpression.accept((IAlertInDepthExpressionVisitor) expression -> {
            // Save to the flattened list
            flattenExpressions.put(expression.getId(), expression);
        });

        return this;
    }

    public static AlertRule from(AlertStorageObject alertObject) {
        AlertRule rule = new AlertRule();
        rule.setId(alertObject.getId());
        rule.setEnabled(!alertObject.isDisabled());
        rule.setAppName(alertObject.getAppName());
        rule.setName(alertObject.getName());
        rule.setEvery(alertObject.getPayload().getEvery());
        rule.setExpr(alertObject.getPayload().getExpr());
        rule.setSilence(alertObject.getPayload().getSilence());
        rule.setForTimes(alertObject.getPayload().getForTimes());
        rule.setNotifications(alertObject.getPayload().getNotifications());
        return rule;
    }

    static class ApplicationNameExtractor {
        private String applicationName = "";

        public String extract(IExpression astExpression) {
            astExpression.accept(((IAlertInDepthExpressionVisitor) expression -> {
                IExpression whereExpression = expression.getMetricExpression().getLabelSelectorExpression();
                if (whereExpression == null) {
                    return;
                }
                whereExpression.accept(new IExpressionInDepthVisitor() {
                    @Override
                    public boolean visit(ConditionalExpression expression) {
                        if (expression instanceof ComparisonExpression.EQ
                            && (expression.getLeft() instanceof IdentifierExpression)
                            && expression.getRight() instanceof LiteralExpression
                            && ((IdentifierExpression) expression.getLeft()).getIdentifier().equals("appName")) {
                            applicationName = ((LiteralExpression) expression.getRight()).asString();
                            return false;
                        }
                        return true;
                    }
                });
            }));

            return applicationName;
        }
    }
}
