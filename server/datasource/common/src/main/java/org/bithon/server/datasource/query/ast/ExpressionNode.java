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

package org.bithon.server.datasource.query.ast;

import lombok.Getter;
import lombok.Setter;
import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.function.Functions;
import org.bithon.server.datasource.ISchema;
import org.bithon.server.datasource.expression.ExpressionASTBuilder;

import java.util.function.Supplier;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/11/3 22:18
 */
public class ExpressionNode implements IASTNode {

    private final Supplier<String> expression;

    @Getter
    @Setter
    private IExpression parsedExpression;

    public IDataType getDataType() {
        return parsedExpression.getDataType();
    }

    public ExpressionNode(String expression) {
        this.expression = () -> expression;
    }

    public ExpressionNode(ISchema schema, String expression) {
        this.expression = () -> expression;
        this.parsedExpression = ExpressionASTBuilder.builder()
                                                    .functions(Functions.getInstance())
                                                    .schema(schema)
                                                    .build(expression);
    }

    public ExpressionNode(IExpression expression) {
        this.expression = () -> this.getParsedExpression().serializeToText();
        this.parsedExpression = expression;
    }

    public String getExpression() {
        return expression.get();
    }
}
