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

import org.bithon.component.commons.expression.function.Functions;
import org.bithon.component.commons.expression.function.IFunction;
import org.bithon.component.commons.utils.Preconditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Frank Chen
 * @date 30/6/23 6:21 pm
 */
public class FunctionExpression implements IExpression {
    private final IFunction function;

    /**
     * Modifiable list
     */
    private final List<IExpression> args;

    /**
     * @param args MUST be a WRITABLE list
     */
    public FunctionExpression(IFunction function, List<IExpression> args) {
        this.function = function;
        this.args = args;
    }

    public FunctionExpression(IFunction function, IExpression... args) {
        this(function, new ArrayList<>(Arrays.asList(args)));
    }

    public String getName() {
        return function.getName();
    }

    public List<IExpression> getArgs() {
        return args;
    }

    public IFunction getFunction() {
        return function;
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
        List<Object> argValues = this.args.stream()
                                          .map((arg) -> arg.evaluate(context))
                                          .collect(Collectors.toList());
        return function.evaluate(argValues);
    }

    @Override
    public void accept(IExpressionInDepthVisitor visitor) {
        if (visitor.visit(this)) {
            for (IExpression arg : this.args) {
                arg.accept(visitor);
            }
        }
    }

    @Override
    public <T> T accept(IExpressionVisitor<T> visitor) {
        return visitor.visit(this);
    }

    public static FunctionExpression create(String functionName, String... args) {
        IFunction function = Functions.getInstance().getFunction(functionName);
        Preconditions.checkNotNull(function, "Function [%s] not found.", functionName);

        return new FunctionExpression(function,
                                      Arrays.stream(args)
                                            .map(IdentifierExpression::new)
                                            .collect(Collectors.toList()));
    }
}
