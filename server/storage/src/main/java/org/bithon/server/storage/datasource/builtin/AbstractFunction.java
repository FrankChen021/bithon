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

package org.bithon.server.storage.datasource.builtin;

import lombok.Getter;
import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.function.IFunction;
import org.bithon.component.commons.expression.function.Parameter;
import org.bithon.server.storage.common.expression.InvalidExpressionException;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/11/2 17:31
 */
@Getter
public abstract class AbstractFunction implements IFunction {
    private final String name;
    private final List<Parameter> parameters;
    private final IDataType returnType;

    public AbstractFunction(String name, IDataType returnType) {
        this(name, Collections.emptyList(), returnType);
    }

    public AbstractFunction(String name, Parameter parameter, IDataType returnType) {
        this(name, Collections.singletonList(parameter), returnType);
    }

    public AbstractFunction(String name, List<Parameter> parameters, IDataType returnType) {
        this.name = name;
        this.parameters = parameters;
        this.returnType = returnType;
    }

    @Override
    public void validateParameter(List<IExpression> parameters) {
        if (getParameters().size() != parameters.size()) {
            throw new InvalidExpressionException("In expression [%s %s], function [%s] can only accept [%d] parameters, but got [%d]",
                                                 name,
                                                 parameters.stream().map(IExpression::serializeToText).collect(Collectors.joining(",")),
                                                 name,
                                                 this.parameters.size(),
                                                 parameters.size());
        }
        for (int i = 0; i < parameters.size(); i++) {
            validateParameter(parameters.get(i), i);
        }
    }

    protected void validateParameter(IExpression parameter, int index) {
        if (parameter instanceof LiteralExpression) {
            IDataType declaredType = this.getParameters().get(index).getType();
            IDataType inputType = parameter.getDataType();
            if (!declaredType.canCastFrom(inputType)) {
                throw new InvalidExpressionException("The parameter at index %d of function [%s] must be type of %s",
                                                     index,
                                                     name,
                                                     declaredType);
            }
        }
    }
}
