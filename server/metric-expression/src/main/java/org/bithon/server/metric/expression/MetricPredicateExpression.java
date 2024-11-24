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
import org.bithon.component.commons.expression.IExpressionInDepthVisitor;
import org.bithon.component.commons.expression.IExpressionVisitor;
import org.bithon.component.commons.expression.LiteralExpression;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/11/24 22:45
 */
@Data
public class MetricPredicateExpression implements MetricQLExpression {

    private MetricQLExpression metricExpression;

    /**
     * Post filter
     */
    private PredicateEnum predicate;
    private LiteralExpression expected;

    @Override
    public IDataType getDataType() {
        return metricExpression.getDataType();
    }

    @Override
    public String getType() {
        return "metricPredicate";
    }

    @Override
    public Object evaluate(IEvaluationContext context) {
        return null;
    }

    @Override
    public void accept(IExpressionInDepthVisitor visitor) {

    }

    @Override
    public <T> T accept(IExpressionVisitor<T> visitor) {
        return null;
    }
}
