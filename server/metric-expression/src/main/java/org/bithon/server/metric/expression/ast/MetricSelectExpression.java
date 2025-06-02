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

package org.bithon.server.metric.expression.ast;


import lombok.Getter;
import lombok.Setter;
import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.expression.IEvaluationContext;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IExpressionInDepthVisitor;
import org.bithon.component.commons.expression.IExpressionVisitor;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.component.commons.expression.expt.InvalidExpressionException;
import org.bithon.component.commons.expression.serialization.ExpressionSerializer;
import org.bithon.component.commons.expression.serialization.IdentifierQuotaStrategy;
import org.bithon.component.commons.utils.HumanReadableDuration;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.datasource.ISchema;
import org.bithon.server.datasource.column.IColumn;

import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2/6/25 11:46 am
 */
@Getter
public class MetricSelectExpression implements IExpression {
    @Setter
    private String from;

    @Setter
    private String metric;

    @Setter
    private HumanReadableDuration window;

    /**
     * Runtime properties
     */
    private IExpression labelSelectorExpression;
    private String labelSelectorText;
    private String whereText;

    public void setLabelSelectorExpression(IExpression labelSelectorExpression) {
        this.labelSelectorExpression = labelSelectorExpression;

        // The where property holds the internal expression that will be passed to the query module
        this.whereText = labelSelectorExpression == null ? null : new MetricExpression.SqlStyleSerializer().serialize(labelSelectorExpression);

        // This variable holds the raw text
        this.labelSelectorText = labelSelectorExpression == null ? "" : "{" + new MetricExpression.LabelSelectorExpressionSerializer().serialize(labelSelectorExpression) + "}";
    }

    @Override
    public String serializeToText() {
        StringBuilder sb = new StringBuilder();
        sb.append(StringUtils.format("%s.%s%s",
                                     from,
                                     // Use field for serialization because it holds the raw input.
                                     // In some cases, like the 'count' aggregator, the name property has different values from the field property
                                     // See AlertExpressionASTParser for more
                                     metric,
                                     this.labelSelectorText));

        if (window != null) {
            sb.append('[');
            sb.append(window);
            sb.append(']');
        }

        return sb.toString();
    }

    @Override
    public void serializeToText(ExpressionSerializer serializer) {
        serializer.append(serializeToText());
    }

    static class SqlStyleSerializer extends ExpressionSerializer {
        public SqlStyleSerializer() {
            super(null);
        }

        @Override
        public void serialize(LiteralExpression<?> expression) {
            if (expression instanceof LiteralExpression.StringLiteral stringLiteral) {
                sb.append('\'');
                sb.append(StringUtils.escape(stringLiteral.getValue(), '\\', '\''));
                sb.append('\'');
            } else {
                sb.append(expression.getValue());
            }
        }
    }

    static class LabelSelectorExpressionSerializer extends ExpressionSerializer {

        public LabelSelectorExpressionSerializer() {
            super(IdentifierQuotaStrategy.NONE);
        }

        @Override
        public void serialize(LogicalExpression expression) {
            for (int i = 0, size = expression.getOperands().size(); i < size; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                expression.getOperands().get(i).serializeToText(this);
            }
        }

        // Use double quote to serialize the expression by default
        @Override
        public void serialize(LiteralExpression<?> expression) {
            if (expression instanceof LiteralExpression.StringLiteral stringLiteral) {
                sb.append('"');
                sb.append(StringUtils.escape(stringLiteral.getValue(), '\\', '"'));
                sb.append('"');
            } else {
                sb.append(expression.getValue());
            }
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
    public void accept(IExpressionInDepthVisitor visitor) {
    }

    @Override
    public <T> T accept(IExpressionVisitor<T> visitor) {
        if (visitor instanceof IMetricExpressionVisitor<T> metricExpressionVisitor) {
            return metricExpressionVisitor.visit(this);
        }
        throw new UnsupportedOperationException("Evaluate an alert expression is not supported.");
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

        String metricName = this.getMetric();
        IColumn metricSpec = schema.getColumnByName(metricName);
        if (metricSpec == null) {
            throw new InvalidExpressionException("Metric [%s] in expression [%s] does not exist in data-source [%s]",
                                                 metricName,
                                                 this.serializeToText(),
                                                 this.getFrom());
        }
    }
}
