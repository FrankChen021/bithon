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
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.expression.IEvaluationContext;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IExpressionVisitor;
import org.bithon.component.commons.expression.IExpressionVisitor2;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.component.commons.expression.serialization.ExpressionSerializer;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.HumanReadableDuration;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.alerting.common.evaluator.AlertExpressionEvaluator;
import org.bithon.server.alerting.common.evaluator.EvaluationContext;
import org.bithon.server.alerting.common.evaluator.metric.IMetricEvaluator;
import org.bithon.server.web.service.datasource.api.QueryField;

import javax.annotation.Nullable;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.function.Function;

/**
 * Absolute comparison
 * avg by (a) (data-source.metric{dim1 = 'x'}) > 0.5
 * <p>
 * Relative comparison with an absolute value
 * avg by (a) (data-source.metric{dim1 = 'z', dim2=''}[1m|h]) > 5[-60m]
 * <p>
 * Relative comparison with a percentage
 * avg by (a) (data-source.metric{dim1 = 'a', dim2=''}[1m|h]) > 5%[-1d]
 * avg by (a) (data-source.metric{dim1 = 'b', dim2=''}[1m|h]) > 5%['2023-01-01']
 *
 * @author frankchen
 * @date 2020-08-21 14:56:50
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertExpression implements IExpression {

    @JsonProperty
    @Size(min = 1, max = 1)
    private String id;

    @JsonProperty
    private String from;

    @JsonProperty
    private String where;

    @JsonProperty
    private QueryField select;

    @JsonProperty
    private HumanReadableDuration duration = HumanReadableDuration.DURATION_1_MINUTE;

    @Nullable
    @JsonProperty
    private List<String> groupBy;

    @JsonProperty
    private String alertPredicate;

    @JsonProperty
    private Object alertExpected;

    @JsonProperty
    private HumanReadableDuration expectedWindow = null;

    /**
     * Runtime properties
     */
    @JsonIgnore
    private IExpression whereExpression;

    @JsonIgnore
    private String rawWhere;

    @JsonIgnore
    private IMetricEvaluator metricEvaluator;

    public void setWhereExpression(IExpression whereExpression) {
        this.whereExpression = whereExpression;

        // The where property holds the internal expression that will be passed to the query module
        this.where = whereExpression == null ? null : whereExpression.serializeToText(null);

        // This variable holds the raw text
        this.rawWhere = whereExpression == null ? "" : "{" + new WhereExpressionSerializer().serialize(whereExpression) + "}";
    }

    @Override
    public String serializeToText() {
        return serializeToText(true);
    }

    @Override
    public String serializeToText(Function<String, String> quoteIdentifier) {
        return serializeToText(true);
    }

    public String serializeToText(boolean includePredication) {
        StringBuilder sb = new StringBuilder();
        sb.append(select.getAggregator());
        if (CollectionUtils.isNotEmpty(this.groupBy)) {
            sb.append(StringUtils.format(" BY (%s) ", String.join(",", this.groupBy)));
        }

        sb.append(StringUtils.format("(%s.%s%s)[%s]",
                                     from,
                                     select.getName(),
                                     this.rawWhere,
                                     duration));
        if (includePredication) {
            sb.append(' ');
            sb.append(alertPredicate);
            sb.append(' ');
            sb.append(alertExpected);
            if (expectedWindow != null) {
                sb.append('[');
                sb.append(expectedWindow);
                sb.append(']');
            }
        }
        return sb.toString();
    }

    static class WhereExpressionSerializer extends ExpressionSerializer {

        public WhereExpressionSerializer() {
            super(null);
        }

        @Override
        public boolean visit(LogicalExpression expression) {
            for (int i = 0, size = expression.getOperands().size(); i < size; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                expression.getOperands().get(i).accept(this);
            }
            return false;
        }
    }

    @Override
    public IDataType getDataType() {
        return IDataType.BOOLEAN;
    }

    @Override
    public String getType() {
        return null;
    }

    @Override
    public Object evaluate(IEvaluationContext context) {
        return new AlertExpressionEvaluator(this).evaluate((EvaluationContext) context);
    }

    @Override
    public void accept(IExpressionVisitor visitor) {
        if (visitor instanceof IAlertExpressionVisitor) {
            ((IAlertExpressionVisitor) visitor).visit(this);
        }
    }

    @Override
    public <T> T accept(IExpressionVisitor2<T> visitor) {
        if (visitor instanceof IAlertExpressionVisitor2) {
            return ((IAlertExpressionVisitor2<T>) visitor).visit(this);
        }
        return null;
    }
}
