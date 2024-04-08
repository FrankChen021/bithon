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

import lombok.Data;
import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.expression.IEvaluationContext;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IExpressionVisitor;
import org.bithon.component.commons.expression.IExpressionVisitor2;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.component.commons.expression.serialization.ExpressionSerializer;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.HumanReadableDuration;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.alerting.common.evaluator.AlertExpressionEvaluator;
import org.bithon.server.alerting.common.evaluator.EvaluationContext;
import org.bithon.server.alerting.common.evaluator.metric.IMetricEvaluator;
import org.bithon.server.alerting.common.parser.InvalidExpressionException;
import org.bithon.server.storage.datasource.ISchema;
import org.bithon.server.storage.datasource.column.IColumn;
import org.bithon.server.web.service.datasource.api.QueryField;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * NOTE: When changes are made to this class,
 * do check if the {@link org.bithon.server.alerting.common.serializer.AlertExpressionSerializer} needs to be modified.
 * <p>
 * This class is constructed by {@link org.bithon.server.alerting.common.parser.AlertExpressionASTParser}
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
 *
 * @author frankchen
 * @date 2020-08-21 14:56:50
 */
@Data
public class AlertExpression implements IExpression {

    private String id;
    private String from;
    private String where;
    private QueryField select;
    private HumanReadableDuration window = HumanReadableDuration.DURATION_1_MINUTE;

    @Nullable
    private List<String> groupBy;
    private String alertPredicate;
    private Object alertExpected;

    @Nullable
    private HumanReadableDuration expectedWindow = null;

    /**
     * Runtime properties
     */
    private IExpression whereExpression;
    private String rawWhere;
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
                                     // Use field for serialization because it holds the raw input.
                                     // In some cases, like the 'count' aggregator, the name property has different values from the field property
                                     // See AlertExpressionASTParser for more
                                     select.getField(),
                                     this.rawWhere,
                                     window));
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

        // Use double quote to serialize the expression by default
        @Override
        public boolean visit(LiteralExpression expression) {
            Object value = expression.getValue();
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

    public void validate(Map<String, ISchema> schemas) {
        if (!StringUtils.hasText(this.getFrom())) {
            throw new InvalidExpressionException("data-source is missed in expression [%s]", this.serializeToText());
        }

        ISchema schema = schemas.get(this.getFrom());
        if (schema == null) {
            throw new InvalidExpressionException("data-source [%s] does not exist for expression [%s]",
                                                 this.getFrom(),
                                                 this.serializeToText());
        }

        String metric = this.getSelect().getField();
        IColumn column = schema.getColumnByName(metric);
        if (column == null) {
            throw new InvalidExpressionException("Metric [%s] in expression [%s] does not exist in data-source [%s]",
                                                 metric,
                                                 this.serializeToText(),
                                                 this.getFrom());
        }
        if (!AggregatorEnum.valueOf(this.getSelect().getAggregator()).isColumnSupported(column)) {
            throw new InvalidExpressionException("Aggregator [%s] is not supported8 on column [%s] which has a type of [%s]",
                                                 this.getSelect().getAggregator(),
                                                 metric,
                                                 column.getDataType().name());
        }

        if (this.getWhereExpression() != null) {
            this.getWhereExpression().accept(new IExpressionVisitor() {
                @Override
                public boolean visit(IdentifierExpression expression) {
                    IColumn dimensionSpec = schema.getColumnByName(expression.getIdentifier());
                    if (dimensionSpec == null) {
                        throw new InvalidExpressionException("Dimension [%s] specified in expression [%s] does not exist",
                                                             expression.getIdentifier(),
                                                             serializeToText());
                    }
                    return false;
                }
            });
        }
    }
}
