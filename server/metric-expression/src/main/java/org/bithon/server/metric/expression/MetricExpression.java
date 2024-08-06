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

package org.bithon.server.metric.expression;

import lombok.Data;
import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.expression.IEvaluationContext;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IExpressionVisitor;
import org.bithon.component.commons.expression.IExpressionVisitor2;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.component.commons.expression.expt.InvalidExpressionException;
import org.bithon.component.commons.expression.serialization.ExpressionSerializer;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.HumanReadableDuration;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.datasource.ISchema;
import org.bithon.server.storage.datasource.column.IColumn;
import org.bithon.server.web.service.datasource.api.QueryField;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 *
 * <p>
 * Absolute comparison
 * avg by (a) (data-source.metric{dim1 = 'x'}) > 0.5
 * <p>
 * Relative comparison with an absolute value
 * avg by (a) (data-source.metric{dim1 = 'z', dim2=''}[1m|h]) > 5[-60m]
 * <p>
 * Relative comparison with a percentage
 * avg by (a) (data-source.metric{dim1 = 'a', dim2=''}[1m|h]) > 5%[-1d]
 * avg by (a) (data-source.metric{dim1 = 'b', dim2=''}[1m|h]) > 5%['2023-01-01']
 * <p>
 *
 * @author frankchen
 * @date 2020-08-21 14:56:50
 */
@Data
public class MetricExpression implements IExpression {

    private String from;
    private QueryField metric;
    private String whereText;
    private HumanReadableDuration window;

    @Nullable
    private List<String> groupBy;

    /**
     * Post filter
     */
    private PredicateEnum predicate;
    private LiteralExpression expected;

    @Nullable
    private HumanReadableDuration expectedWindow = null;

    /**
     * Runtime properties
     */
    private IExpression labelSelectorExpression;
    private String labelSelectorText;

    public void setLabelSelectorExpression(IExpression labelSelectorExpression) {
        this.labelSelectorExpression = labelSelectorExpression;

        // The where property holds the internal expression that will be passed to the query module
        this.whereText = labelSelectorExpression == null ? null : labelSelectorExpression.serializeToText(null);

        // This variable holds the raw text
        this.labelSelectorText = labelSelectorExpression == null ? "" : "{" + new LabelSelectorExpressionSerializer().serialize(labelSelectorExpression) + "}";
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
        sb.append(metric.getAggregator());
        sb.append(StringUtils.format("(%s.%s%s)",
                                     from,
                                     // Use field for serialization because it holds the raw input.
                                     // In some cases, like the 'count' aggregator, the name property has different values from the field property
                                     // See AlertExpressionASTParser for more
                                     metric.getField(),
                                     this.labelSelectorText));

        if (window != null) {
            sb.append('[');
            sb.append(window);
            sb.append(']');
        }

        if (CollectionUtils.isNotEmpty(this.groupBy)) {
            sb.append(StringUtils.format(" BY (%s) ", String.join(",", this.groupBy)));
        }

        if (includePredication) {
            sb.append(' ');
            sb.append(predicate);
            sb.append(' ');
            sb.append(expected);
            if (expectedWindow != null) {
                sb.append('[');
                sb.append(expectedWindow);
                sb.append(']');
            }
        }
        return sb.toString();
    }

    static class LabelSelectorExpressionSerializer extends ExpressionSerializer {

        public LabelSelectorExpressionSerializer() {
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

        // Use double quote to serialize the expression by default
        @Override
        public boolean visit(LiteralExpression expression) {
            Object value = expression.getLiteralValue();
            if (expression instanceof LiteralExpression.StringLiteral) {
                sb.append('"');
                sb.append(value);
                sb.append('"');
            } else {
                sb.append(value);
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
        // No need
        return null;
    }

    @Override
    public Object evaluate(IEvaluationContext context) {
        throw new UnsupportedOperationException("Evaluate an alert expression is not supported.");
    }

    @Override
    public void accept(IExpressionVisitor visitor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T accept(IExpressionVisitor2<T> visitor) {
        throw new UnsupportedOperationException();
    }

    public void validate(Map<String, ISchema> schemas) {
        if (!StringUtils.hasText(this.getFrom())) {
            throw new InvalidExpressionException("data-source expression is missed in expression [%s]", this.serializeToText());
        }

        ISchema schema = schemas.get(this.getFrom());
        if (schema == null) {
            throw new InvalidExpressionException("data-source expression [%s] does not exist for expression [%s]",
                                                 this.getFrom(),
                                                 this.serializeToText());
        }

        String metricName = this.getMetric().getField();
        IColumn metricSpec = schema.getColumnByName(metricName);
        if (metricSpec == null) {
            throw new InvalidExpressionException("Metric [%s] in expression [%s] does not exist in data-source [%s]",
                                                 metricName,
                                                 this.serializeToText(),
                                                 this.getFrom());
        }
        if (!AggregatorEnum.valueOf(this.getMetric().getAggregator()).isColumnSupported(metricSpec)) {
            throw new InvalidExpressionException("Aggregator [%s] is not supported8 on metricSpec [%s] which has a type of [%s]",
                                                 this.getMetric().getAggregator(),
                                                 metricName,
                                                 metricSpec.getDataType().name());
        }

        if (this.getLabelSelectorExpression() != null) {
            this.getLabelSelectorExpression().accept(new IExpressionVisitor() {
                @Override
                public boolean visit(IdentifierExpression expression) {
                    IColumn dimSpec = schema.getColumnByName(expression.getIdentifier());
                    if (dimSpec == null) {
                        throw new InvalidExpressionException("label [%s] specified in the expression [%s] does not exist",
                                                             expression.getIdentifier(),
                                                             serializeToText());
                    }
                    return false;
                }
            });
        }

        if (CollectionUtils.isNotEmpty(this.getGroupBy())) {
            for (String groupBy : this.getGroupBy()) {
                IColumn dimSpec = schema.getColumnByName(groupBy);
                if (dimSpec == null) {
                    throw new InvalidExpressionException("BY expression [%s] specified in expression does not exist",
                                                         groupBy);
                }
            }
        }
    }
}
