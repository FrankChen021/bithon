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

package org.bithon.component.commons.expression.function.builtin;

import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.function.AbstractFunction;

import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/7/18 21:13
 */
public abstract class AggregateFunction extends AbstractFunction {

    public AggregateFunction(String name) {
        super(name, IDataType.LONG);
    }

    @Override
    final public boolean isAggregator() {
        return true;
    }

    public static class Min extends AggregateFunction {
        public Min() {
            super("min");
        }

        @Override
        public void validateParameter(List<IExpression> parameters) {
            Validator.validateParameterSize(1, parameters.size());
            Validator.validateType(parameters.get(0).getDataType(), IDataType.DOUBLE, IDataType.LONG, IDataType.DATETIME_3, IDataType.STRING);
        }

        @Override
        public Object evaluate(List<Object> parameters) {
            throw new UnsupportedOperationException("Not implemented yet");
        }
    }

    public static class Max extends AggregateFunction {
        public Max() {
            super("max");
        }

        @Override
        public void validateParameter(List<IExpression> parameters) {
            Validator.validateParameterSize(1, parameters.size());
            Validator.validateType(parameters.get(0).getDataType(), IDataType.DOUBLE, IDataType.LONG, IDataType.DATETIME_3, IDataType.STRING);
        }

        @Override
        public Object evaluate(List<Object> parameters) {
            throw new UnsupportedOperationException("Not implemented yet");
        }
    }

    public static class Sum extends AggregateFunction {
        public Sum() {
            super("sum");
        }

        @Override
        public void validateParameter(List<IExpression> parameters) {
            Validator.validateParameterSize(1, parameters.size());
            Validator.validateType(parameters.get(0).getDataType(), IDataType.DOUBLE, IDataType.LONG, IDataType.DATETIME_3);
        }

        @Override
        public Object evaluate(List<Object> parameters) {
            throw new UnsupportedOperationException("Not implemented yet");
        }
    }
}
