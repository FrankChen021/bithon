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

package org.bithon.server.datasource.reader.postgresql;

import org.bithon.component.commons.expression.ArithmeticExpression;
import org.bithon.component.commons.expression.ComparisonExpression;
import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.expression.IEvaluationContext;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IExpressionInDepthVisitor;
import org.bithon.component.commons.expression.IExpressionVisitor;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.MacroExpression;
import org.bithon.component.commons.expression.optimzer.AbstractOptimizer;
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
        private IExpression whenExpression;
        private IExpression thenExpression;
        private IExpression elseExpression;
        private boolean optimized;

        CaseWhenStatement(IExpression whenExpression, IExpression thenExpression, IExpression elseExpression) {
            this.whenExpression = whenExpression;
            this.thenExpression = thenExpression;
            this.elseExpression = elseExpression;
            this.optimized = false;
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
            throw new UnsupportedOperationException();
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T accept(IExpressionVisitor<T> visitor) {
            // In the SelectStatementBuilder, the parsedExpression is first optimized
            // This is to support replacing aggregator sum to sumMerge
            // However, for H2, a DIV might be transformed to a safe division expression here
            // which does not support visitor pattern
            // So we need to check if the visitor is AbstractOptimizer
            if (visitor instanceof AbstractOptimizer) {
                if (optimized) {
                    // Don't optimize on the CASE-WHEN as the 'thenExpression' is a DIV expression which should not be optimized again
                    return (T) this;
                }

                this.whenExpression = (IExpression) whenExpression.accept(visitor);
                this.thenExpression = (IExpression) thenExpression.accept(visitor);
                this.elseExpression = (IExpression) elseExpression.accept(visitor);

                return (T) this;
            }

            throw new UnsupportedOperationException();
        }

        @Override
        public String serializeToText() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void serializeToText(ExpressionSerializer serializer) {
            serializer.append("CASE WHEN ( ");
            whenExpression.serializeToText(serializer);
            serializer.append(" ) THEN ( ");
            thenExpression.serializeToText(serializer);
            serializer.append(" ) ELSE ( ");
            elseExpression.serializeToText(serializer);
            serializer.append(" ) END");
        }
    }
}
