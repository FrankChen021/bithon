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
 *
 * ALL methods in this interface MUST return the type of boolean
 * to indicate whether we need to visit the children of the current expression object.
 * 'false' means to skip the visit.
 *
 * @author frank.chen021@outlook.com
 * @date 2023/8/19 20:24
 */
public interface IExpressionInDepthVisitor {
    default boolean visit(LiteralExpression<?> expression) {
        return true;
    }

    default boolean visit(LogicalExpression expression) {
        return true;
    }

    default boolean visit(IdentifierExpression expression) {
        return true;
    }

    default boolean visit(ExpressionList expression) {
        return true;
    }

    default boolean visit(FunctionExpression expression) {
        return true;
    }

    default boolean visit(ArrayAccessExpression expression) {
        return true;
    }

    default boolean visit(ArithmeticExpression expression) {
        return true;
    }

    default boolean visit(ConditionalExpression expression) { return true; }

    default boolean visit(MacroExpression expression) { return true; }

    default boolean visit(MapAccessExpression expression) { return true; }

    default boolean visit(TernaryExpression expression) { return true; }
}
