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
    public final boolean isAggregator() {
        return true;
    }

    @Override
    public boolean isDeterministic() {
        return false;
    }

    public static class Min extends AggregateFunction {
        public static final String NAME = "min";

        public Min() {
            super(NAME);
        }

        @Override
        public void validateArgs(List<IExpression> args) {
            Validator.validateParameterSize(1, args.size());
            Validator.validateType(args.get(0).getDataType(), IDataType.DOUBLE, IDataType.LONG, IDataType.DATETIME_3, IDataType.STRING);
        }

        @Override
        public Object evaluate(List<Object> parameters) {
            throw new UnsupportedOperationException("Not implemented yet");
        }
    }

    public static class Max extends AggregateFunction {
        public static final String NAME = "max";

        public Max() {
            super(NAME);
        }

        @Override
        public void validateArgs(List<IExpression> args) {
            Validator.validateParameterSize(1, args.size());
            Validator.validateType(args.get(0).getDataType(), IDataType.DOUBLE, IDataType.LONG, IDataType.DATETIME_3, IDataType.STRING);
        }

        @Override
        public Object evaluate(List<Object> parameters) {
            throw new UnsupportedOperationException("Not implemented yet");
        }
    }

    public static class Sum extends AggregateFunction {
        public static final String NAME = "sum";

        public Sum() {
            super(NAME);
        }

        @Override
        public void validateArgs(List<IExpression> args) {
            Validator.validateParameterSize(1, args.size());
            Validator.validateType(args.get(0).getDataType(), IDataType.DOUBLE, IDataType.LONG, IDataType.DATETIME_3);
        }

        @Override
        public Object evaluate(List<Object> parameters) {
            throw new UnsupportedOperationException("Not implemented yet");
        }
    }

    public static class Count extends AggregateFunction {
        public static final String NAME = "count";

        public Count() {
            super(NAME);
        }

        /**
         * count() is not accepted
         */
        @Override
        public void validateArgs(List<IExpression> args) {
            Validator.validateParameterSize(1, args.size());
        }

        @Override
        public Object evaluate(List<Object> parameters) {
            throw new UnsupportedOperationException("Not implemented yet");
        }
    }

    public static class Avg extends AggregateFunction {
        public Avg() {
            super("avg");
        }

        @Override
        public void validateArgs(List<IExpression> args) {
            Validator.validateParameterSize(1, args.size());
            Validator.validateType(args.get(0).getDataType(), IDataType.DOUBLE, IDataType.LONG, IDataType.DATETIME_3);
        }

        @Override
        public Object evaluate(List<Object> parameters) {
            throw new UnsupportedOperationException("Not implemented yet");
        }
    }

    public static class First extends AggregateFunction {
        public static final String NAME = "first";

        public First() {
            super(NAME);
        }

        @Override
        public void validateArgs(List<IExpression> args) {
            Validator.validateParameterSize(1, args.size());
            Validator.validateType(args.get(0).getDataType(), IDataType.DOUBLE, IDataType.LONG, IDataType.DATETIME_3);
        }

        @Override
        public Object evaluate(List<Object> parameters) {
            throw new UnsupportedOperationException("Not implemented yet");
        }
    }

    public static class Last extends AggregateFunction {
        public static final String NAME = "last";

        public Last() {
            super(NAME);
        }

        @Override
        public void validateArgs(List<IExpression> args) {
            Validator.validateParameterSize(1, args.size());
            Validator.validateType(args.get(0).getDataType(), IDataType.DOUBLE, IDataType.LONG, IDataType.DATETIME_3);
        }

        @Override
        public Object evaluate(List<Object> parameters) {
            throw new UnsupportedOperationException("Not implemented yet");
        }
    }


}
