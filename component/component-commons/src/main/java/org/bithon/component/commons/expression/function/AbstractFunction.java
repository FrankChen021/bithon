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

package org.bithon.component.commons.expression.function;

import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.expt.InvalidExpressionException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/11/2 17:31
 */
public abstract class AbstractFunction implements IFunction {
    private final String name;
    private final List<IDataType> parameterTypeList;
    private final IDataType returnType;

    public AbstractFunction(String name, IDataType returnType) {
        this(name, Collections.emptyList(), returnType);
    }

    public AbstractFunction(String name, IDataType paramType, IDataType returnType) {
        this(name, Collections.singletonList(paramType), returnType);
    }

    public AbstractFunction(String name, List<IDataType> parameterTypeList, IDataType returnType) {
        this.name = name;
        this.parameterTypeList = parameterTypeList;
        this.returnType = returnType;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public List<IDataType> getParameterTypeList() {
        return this.parameterTypeList;
    }

    @Override
    public IDataType getReturnType() {
        return this.returnType;
    }

    @Override
    public void validateArgs(List<IExpression> args) {
        if (getParameterTypeList().size() != args.size()) {
            throw new InvalidExpressionException("In expression [%s %s], function [%s] can only accept [%d] args, but got [%d]",
                                                 name,
                                                 args.stream().map(IExpression::serializeToText).collect(Collectors.joining(",")),
                                                 name,
                                                 this.parameterTypeList.size(),
                                                 args.size());
        }
        for (int i = 0; i < args.size(); i++) {
            validateArgs(args.get(i), i);
        }
    }

    protected void validateArgs(IExpression arg, int index) {
        if (arg instanceof LiteralExpression) {
            IDataType declaredType = this.getParameterTypeList().get(index);
            IDataType inputType = arg.getDataType();
            if (!declaredType.canCastFrom(inputType)) {
                throw new InvalidExpressionException("The parameter at index %d of function [%s] must be type of %s",
                                                     index,
                                                     name,
                                                     declaredType);
            }
        }
    }

    protected void validateParameterSize(int expectedSize, int actualSize) {
        if (expectedSize != actualSize) {
            throw new InvalidExpressionException("Function [%s] requires [%d] parameters, but got [%d]",
                                                 this.name,
                                                 expectedSize,
                                                 actualSize);
        }
    }

    protected void validateType(IDataType actual, IDataType... expected) {
        for (IDataType ex : expected) {
            if (actual.equals(ex)) {
                return;
            }
        }

        throw new InvalidExpressionException("The given parameter is type of [%s], but required one of [%s]",
                                             actual,
                                             Arrays.stream(expected)
                                                   .map(Object::toString)
                                                   .collect(Collectors.joining(",")));
    }

    protected void validateTrue(boolean expression, String messageFormat, Object... args) {
        if (!expression) {
            throw new InvalidExpressionException(messageFormat, args);
        }
    }
}
