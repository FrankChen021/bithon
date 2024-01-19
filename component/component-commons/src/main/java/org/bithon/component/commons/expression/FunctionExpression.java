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

import org.bithon.component.commons.expression.function.IFunction;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Frank Chen
 * @date 30/6/23 6:21 pm
 */
public class FunctionExpression implements IExpression {
    private final IFunction function;

    private final List<IExpression> parameters;

    public FunctionExpression(IFunction function, List<IExpression> parameters) {
        this.function = function;
        this.parameters = parameters;
    }

    public FunctionExpression(IFunction function, IExpression... parameters) {
        this(function, Arrays.asList(parameters));
    }


    public String getName() {
        return function.getName();
    }

    public List<IExpression> getParameters() {
        return parameters;
    }

    @Override
    public IDataType getDataType() {
        return function == null ? null : function.getReturnType();
    }

    @Override
    public String getType() {
        return "Function";
    }

    @Override
    public Object evaluate(IEvaluationContext context) {
        List<Object> arguments = this.parameters.stream().map((parameter) -> parameter.evaluate(context)).collect(Collectors.toList());
        return function.evaluate(arguments);
    }

    @Override
    public void accept(IExpressionVisitor visitor) {
        if (visitor.visit(this)) {
            for (IExpression parameters : this.parameters) {
                parameters.accept(visitor);
            }
        }
    }

    @Override
    public <T> T accept(IExpressionVisitor2<T> visitor) {
        return visitor.visit(this);
    }
}
