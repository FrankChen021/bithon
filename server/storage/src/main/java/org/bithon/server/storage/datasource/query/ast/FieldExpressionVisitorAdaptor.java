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

package org.bithon.server.storage.datasource.query.ast;


/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/23
 */
public interface FieldExpressionVisitorAdaptor {

    default void visitField(String field) {
    }

    default void visitConstant(String number) {
    }

    default void visitorOperator(String operator) {
    }

    default void beginSubExpression() {
    }

    default void endSubExpression() {
    }

    default void visitVariable(String variable) {
    }

    default void beginFunction(String name) {
    }

    default void endFunction() {
    }

    default void beginFunctionArgument(int argIndex, int count) {
    }

    default void endFunctionArgument(int argIndex, int count) {
    }
}
