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

package org.bithon.server.storage.datasource.input.filter;

import lombok.extern.slf4j.Slf4j;
import org.bithon.server.storage.datasource.input.IInputRow;

import java.util.function.Function;

/**
 * @author Frank Chen
 * @date 4/8/22 6:08 PM
 */
@Slf4j
public abstract class BinaryExpressionFilter implements IInputRowFilter {

    private final Function<IInputRow, Object> leftValueSupplier;
    private final Function<IInputRow, Object> rightValueSupplier;

    public BinaryExpressionFilter(Function<IInputRow, Object> leftValueSupplier,
                                  Function<IInputRow, Object> rightValueSupplier) {
        this.leftValueSupplier = leftValueSupplier;
        this.rightValueSupplier = rightValueSupplier;
    }

    @Override
    public boolean shouldInclude(IInputRow inputRow) {
        Object left = leftValueSupplier.apply(inputRow);
        Object right = rightValueSupplier.apply(inputRow);
        if (left == null || right == null) {
            return matchNull(left, right);
        }

        if (left instanceof Number) {
            if (right instanceof Number) {
                return matchesNumber((Number) left, (Number) right);
            } else {
                return matchesNumber((Number) left, Long.parseLong(right.toString()));
            }
        }

        return matchesString(left.toString(), right.toString());
    }

    protected abstract boolean matchNull(Object left, Object right);

    protected abstract boolean matchesNumber(Number left, Number right);

    protected abstract boolean matchesString(String left, String right);

    public static class EQ extends BinaryExpressionFilter {
        public EQ(Function<IInputRow, Object> leftValueSupplier,
                  Function<IInputRow, Object> rightValueSupplier) {
            super(leftValueSupplier, rightValueSupplier);
        }

        @Override
        protected boolean matchNull(Object left, Object right) {
            return left == null && right == null;
        }

        @Override
        protected boolean matchesNumber(Number left, Number right) {
            return left.longValue() == right.longValue();
        }

        @Override
        protected boolean matchesString(String left, String right) {
            return left.equals(right);
        }
    }

    public static class NE extends BinaryExpressionFilter {
        public NE(Function<IInputRow, Object> leftValueSupplier, Function<IInputRow, Object> rightValueSupplier) {
            super(leftValueSupplier, rightValueSupplier);
        }

        @Override
        protected boolean matchNull(Object left, Object right) {
            return !(left == null && right == null);
        }

        @Override
        protected boolean matchesNumber(Number left, Number right) {
            return left.longValue() != right.longValue();
        }

        @Override
        protected boolean matchesString(String left, String right) {
            return !left.equals(right);
        }
    }

    public static class DebuggableFilter implements IInputRowFilter {
        private final String expression;
        private final BinaryExpressionFilter delegate;

        public DebuggableFilter(String expression, BinaryExpressionFilter delegate) {
            this.expression = expression;
            this.delegate = delegate;
        }

        @Override
        public boolean shouldInclude(IInputRow inputRow) {
            boolean ret = delegate.shouldInclude(inputRow);
            log.info("Expression[{}] evaluates to be {}: left val={}, right val = {}",
                     expression,
                     ret,
                     delegate.leftValueSupplier.apply(inputRow),
                     delegate.rightValueSupplier.apply(inputRow));
            return ret;
        }
    }
}
