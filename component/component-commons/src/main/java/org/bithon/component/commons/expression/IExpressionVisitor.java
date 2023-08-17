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

package org.bithon.component.commons.expression;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/4/8 11:41
 */
public interface IExpressionVisitor<T> {
    default T visit(LiteralExpression expression) {
        return null;
    }

    default T visit(LogicalExpression expression) {
        return null;
    }

    default T visit(IdentifierExpression expression) {
        return null;
    }

    default T visit(ComparisonExpression.EQ expression) {
        return null;
    }

    default T visit(ComparisonExpression.GT expression) {
        return null;
    }

    default T visit(ComparisonExpression.GTE expression) {
        return null;
    }

    default T visit(ComparisonExpression.LT expression) {
        return null;
    }

    default T visit(ComparisonExpression.LTE expression) {
        return null;
    }

    default T visit(ComparisonExpression.NE expression) {
        return null;
    }

    default T visit(ComparisonExpression.IN expression) {
        return null;
    }

    default T visit(ComparisonExpression.LIKE expression) {
        return null;
    }

    default T visit(ArithmeticExpression.Add expression) {
        return null;
    }

    default T visit(ArithmeticExpression.Sub expression) {
        return null;
    }

    default T visit(ArithmeticExpression.Mul expression) {
        return null;
    }

    default T visit(ArithmeticExpression.Div expression) {
        return null;
    }

    default T visit(CollectionExpression expression) {
        return null;
    }

    default T visit(FieldExpression expression) {
        return null;
    }

    default T visit(FunctionExpression expression) {
        return null;
    }

    default T visit(ArrayAccessExpression expression) {
        return null;
    }
}
