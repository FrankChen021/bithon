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
import org.bithon.component.commons.expression.IExpressionInDepthVisitor;
import org.bithon.component.commons.expression.IExpressionVisitor;
import org.bithon.server.alerting.common.evaluator.metric.IMetricEvaluator;
import org.bithon.server.metric.expression.MetricExpression;

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
 * <p>
 * NOTE that this class is serialized by {@link org.bithon.server.alerting.common.serializer.AlertExpressionSerializer}
 *
 * @author frankchen
 * @date 2020-08-21 14:56:50
 */
@Data
public class AlertExpression implements IExpression {

    private String id;
    private MetricExpression metricExpression;
    private IMetricEvaluator metricEvaluator;

    @Override
    public IDataType getDataType() {
        return IDataType.BOOLEAN;
    }

    @Override
    public String getType() {
        return "";
    }

    @Override
    public Object evaluate(IEvaluationContext context) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void accept(IExpressionInDepthVisitor visitor) {
        if (visitor instanceof IAlertInDepthExpressionVisitor) {
            ((IAlertInDepthExpressionVisitor) visitor).visit(this);
        }
    }

    @Override
    public <T> T accept(IExpressionVisitor<T> visitor) {
        if (visitor instanceof IAlertExpressionVisitor<T>) {
            return ((IAlertExpressionVisitor<T>) visitor).visit(this);
        } else {
            return null;
        }
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
        return metricExpression.serializeToText(includePredication);
    }
}
