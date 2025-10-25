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

package org.bithon.server.datasource.reader.clickhouse.function;


import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.expt.InvalidExpressionException;
import org.bithon.component.commons.expression.function.AbstractFunction;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 25/10/25 5:28 pm
 */
public class HasFunction extends AbstractFunction {

    public HasFunction() {
        super("has",
              // Don't provide declaring parameters because it accepts either object or array,
              // and we will validate by ourselves
              (List<IDataType>) null,
              IDataType.BOOLEAN);
    }

    @Override
    public Object evaluate(List<Object> args) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDeterministic() {
        return false;
    }

    @Override
    public void validateArgs(List<IExpression> args) {
        if (args.size() != 2) {
            throw new InvalidExpressionException("In expression [%s %s], function [%s] can only accept [%d] args, but got [%d]",
                                                 getName(),
                                                 args.stream().map(IExpression::serializeToText).collect(Collectors.joining(",")),
                                                 getName(),
                                                 this.getDeclaredParameterTypes().size(),
                                                 args.size());
        }

        IDataType containerType = args.get(0).getDataType();
        if (containerType != IDataType.OBJECT && containerType != IDataType.ARRAY) {
            throw new InvalidExpressionException("In expression [%s(%s)], function [%s] can only accept OBJECT/ARRAY as first argument, but got [%s]",
                                                 getName(),
                                                 args.stream().map(IExpression::serializeToText).collect(Collectors.joining(",")),
                                                 getName(),
                                                 containerType.name());
        }

        IDataType dataType = args.get(1).getDataType();
        if (dataType != IDataType.STRING) {
            throw new InvalidExpressionException("In expression [%s(%s)], function [%s] can only accept STRING as 2nd argument, but got [%s]",
                                                 getName(),
                                                 args.stream().map(IExpression::serializeToText).collect(Collectors.joining(",")),
                                                 getName(),
                                                 dataType);
        }
    }
}
