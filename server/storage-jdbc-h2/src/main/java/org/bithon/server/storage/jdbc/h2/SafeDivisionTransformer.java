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

package org.bithon.server.storage.jdbc.h2;

import org.bithon.component.commons.expression.ArithmeticExpression;
import org.bithon.component.commons.expression.ComparisonExpression;
import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.expression.IEvaluationContext;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IExpressionInDepthVisitor;
import org.bithon.component.commons.expression.IExpressionVisitor;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.MacroExpression;
import org.bithon.component.commons.expression.serialization.ExpressionSerializer;

/**
 * @author frank.chen021@outlook.com
 * @date 12/9/24 4:51 pm
 */
public class SafeDivisionTransformer {

    /**
     * Optimize the DIV into a safe division expression in H2.
     * <code><pre>
     *  CASE WHEN "retriedRequest" <> 0 THEN "retrySuccess" / "retriedRequest" ELSE 0 END</pre>
     * </code>
     */
    public static IExpression transform(ArithmeticExpression expression) {
        if (!(expression instanceof ArithmeticExpression.DIV)) {
            return expression;
        }

        if (expression.getRhs() instanceof LiteralExpression<?>
            || expression.getRhs() instanceof MacroExpression) {
            return expression;
        }

        // If the rhs is not a literal, we need to turn the DIV as a safe division
        return new CaseWhenStatement(new ComparisonExpression.NE(expression.getRhs(), LiteralExpression.ofLong(0)),
                                     expression,
                                     LiteralExpression.ofLong(0));
    }

    /**
     * Since the expression is only used in the SQL generation, we do not need to implement the evaluate method
     */
    static class CaseWhenStatement implements IExpression {
        private final IExpression whenExpression;
        private final IExpression thenExpression;
        private final IExpression elseExpression;

        CaseWhenStatement(IExpression whenExpression, IExpression thenExpression, IExpression elseExpression) {
            this.whenExpression = whenExpression;
            this.thenExpression = thenExpression;
            this.elseExpression = elseExpression;
        }

        @Override
        public IDataType getDataType() {
            return elseExpression.getDataType();
        }

        @Override
        public String getType() {
            return "case-when";
        }

        @Override
        public Object evaluate(IEvaluationContext context) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void accept(IExpressionInDepthVisitor visitor) {
            if (!(visitor instanceof ExpressionSerializer serializer)) {
                throw new UnsupportedOperationException();
            }

            serializer.append("CASE WHEN ( ");
            whenExpression.accept(serializer);
            serializer.append(" ) THEN ( ");
            thenExpression.accept(serializer);
            serializer.append(" ) ELSE ( ");
            elseExpression.accept(serializer);
            serializer.append(" ) END ");
        }

        @Override
        public <T> T accept(IExpressionVisitor<T> visitor) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String serializeToText() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void serializeToText(ExpressionSerializer serializer) {
            serializer.append("CASE WHEN ( ");
            whenExpression.accept(serializer);
            serializer.append(" ) THEN ( ");
            thenExpression.accept(serializer);
            serializer.append(" ) ELSE ( ");
            elseExpression.accept(serializer);
            serializer.append(" ) END ");
        }
    }
}
