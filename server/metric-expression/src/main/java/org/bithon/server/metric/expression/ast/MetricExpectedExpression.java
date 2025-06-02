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


import lombok.Builder;
import lombok.Getter;
import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.expression.IEvaluationContext;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IExpressionInDepthVisitor;
import org.bithon.component.commons.expression.IExpressionVisitor;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.serialization.ExpressionSerializer;
import org.bithon.component.commons.utils.HumanReadableDuration;

import javax.annotation.Nullable;

/**
 * @author frank.chen021@outlook.com
 * @date 2/6/25 10:03 pm
 */
@Getter
@Builder
public class MetricExpectedExpression implements IExpression {
    /**
     * The expected value of the metric.
     */
    private final LiteralExpression<?> expected;

    @Nullable
    private final HumanReadableDuration offset;

    @Override
    public IDataType getDataType() {
        return expected.getDataType();
    }

    @Override
    public String getType() {
        return "expected";
    }

    @Override
    public Object evaluate(IEvaluationContext context) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void accept(IExpressionInDepthVisitor visitor) {
    }

    @Override
    public <T> T accept(IExpressionVisitor<T> visitor) {
        if (visitor instanceof IMetricExpressionVisitor<T> metricVisitor) {
            return metricVisitor.visit(this);
        } else {
            throw new UnsupportedOperationException("Unsupported visitor type: " + visitor.getClass().getName());
        }
    }

    @Override
    public void serializeToText(ExpressionSerializer serializer) {
        expected.serializeToText(serializer);
        if (offset != null) {
            serializer.append("[");
            serializer.append(offset.toString());
            serializer.append("]");
        }
    }
}
