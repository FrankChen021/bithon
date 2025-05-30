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

package org.bithon.server.datasource.reader.jdbc.dialect;

import org.bithon.component.commons.expression.ConditionalExpression;
import org.bithon.component.commons.expression.IEvaluationContext;
import org.bithon.component.commons.expression.IExpression;

/**
 * The Like operator in SQL
 */
public class LikeOperator extends ConditionalExpression {

    public LikeOperator(IExpression left, IExpression right) {
        this("like", left, right);
    }

    public LikeOperator(String operator, IExpression left, IExpression right) {
        super(operator, left, right);
    }

    @Override
    public Object evaluate(IEvaluationContext context) {
        throw new UnsupportedOperationException("Like is not designed for evaluation. Use 'contains' instead.");
    }
}
