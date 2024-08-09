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

package org.bithon.component.commons.expression.validation;

import org.bithon.component.commons.expression.ConditionalExpression;
import org.bithon.component.commons.expression.ExpressionList;
import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IExpressionInDepthVisitor;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.component.commons.expression.function.IFunction;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/1/20 14:08
 */
class ExpressionTypeValidator implements IExpressionInDepthVisitor {

    /**
     * If the type of identifier is not defined,
     * it's not able to validate the types for identifiers and related expressions
     */
    private final boolean identifierHasTypeInfo;

    ExpressionTypeValidator(boolean identifierHasTypeInfo) {
        this.identifierHasTypeInfo = identifierHasTypeInfo;
    }

    @Override
    public boolean visit(LogicalExpression expression) {
        for (IExpression operand : expression.getOperands()) {
            IDataType dataType = operand.getDataType();
            if (!dataType.equals(IDataType.BOOLEAN)
                && !dataType.equals(IDataType.DOUBLE)
                && !dataType.equals(IDataType.LONG)) {
                throw new ExpressionValidationException("The expression [%s] in the logical expression should be a conditional expression.",
                                                        operand.serializeToText(null));
            }
        }
        return true;
    }

    @Override
    public boolean visit(ConditionalExpression expression) {
        if (!this.identifierHasTypeInfo
            && (expression.getLhs() instanceof IdentifierExpression
                || expression.getRhs() instanceof IdentifierExpression)) {
            // When the identifier does not have type info,
            // and either one of the operands of conditional expression is identifier,
            // we skip the validation and cast the type
            return true;
        }

        IDataType lhsType = expression.getLhs().getDataType();
        IDataType rhsType = expression.getRhs().getDataType();
        if (lhsType.equals(rhsType)) {
            return true;
        }

        // Type cast
        if (expression.getRhs() instanceof LiteralExpression) {
            if (expression.getLhs() instanceof LiteralExpression) {
                // This is the case that both lhs and rhs are literals
                return true;
            }

            // Only do cast if the left is not literal
            LiteralExpression<?> rhs = (LiteralExpression<?>) expression.getRhs();
            try {
                expression.setRhs(rhs.castTo(lhsType));
            } catch (ExpressionValidationException e) {
                throw new ExpressionValidationException("Can't convert [%s] into type of [%s] in the expression: %s",
                                                        rhs.serializeToText(null),
                                                        lhsType,
                                                        expression.serializeToText(null));
            }
            return true;
        }

        if (!lhsType.canCastFrom(rhsType)) {
            throw new ExpressionValidationException("The data types of the two operators in the expression [%s] are not compatible (%s vs %s).",
                                                    expression.serializeToText(null),
                                                    lhsType.name(),
                                                    rhsType == null ? "null" : rhsType.name());
        }

        return true;
    }

    @Override
    public boolean visit(ExpressionList expression) {
        IDataType dataType = expression.getExpressions().get(0).getDataType();

        // Ensure the types of all elements are the same
        for (int i = 1, size = expression.getExpressions().size(); i < size; i++) {
            if (!dataType.canCastFrom(expression.getExpressions().get(i).getDataType())) {
                throw new ExpressionValidationException("The data types of elements in the expression list %s are not the same.",
                                                        expression.serializeToText(null));
            }
        }
        return true;
    }

    @Override
    public boolean visit(FunctionExpression expression) {
        IFunction function = expression.getFunction();
        if (function != null) {
            function.validateArgs(expression.getArgs());
        }
        return true;
    }
}
