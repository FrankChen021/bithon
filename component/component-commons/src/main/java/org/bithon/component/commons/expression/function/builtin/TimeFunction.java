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
import org.bithon.component.commons.expression.function.AbstractFunction;

import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/7/18 21:31
 */
public class TimeFunction {
    public static class ToStartOfMinute extends AbstractFunction {
        public ToStartOfMinute() {
            super("toStartOfMinute", IDataType.LONG, IDataType.LONG);
        }

        @Override
        public Object evaluate(List<Object> args) {
            Object o = args.get(0);
            return (o instanceof Number) ? ((Number) o).longValue() / 1000 / 60 : 0;
        }

        @Override
        public boolean isDeterministic() {
            return true;
        }
    }

    /**
     * returns the current seconds since unix epoch
     */
    public static class Now extends AbstractFunction {
        public Now() {
            super("now", IDataType.LONG);
        }

        @Override
        public Object evaluate(List<Object> args) {
            return System.currentTimeMillis() / 1000;
        }

        @Override
        public boolean isDeterministic() {
            return false;
        }
    }

    public static class ToNanoSeconds extends AbstractFunction {
        public static final ToNanoSeconds INSTANCE = new ToNanoSeconds();

        private ToNanoSeconds() {
            super("toNanoSeconds", IDataType.LONG, IDataType.LONG);
        }

        @Override
        public Object evaluate(List<Object> args) {
            return ((Number) args.get(0)).longValue() * 1000_000_000;
        }

        @Override
        public boolean isDeterministic() {
            return true;
        }
    }

    public static class ToMicroSeconds extends AbstractFunction {
        public static final ToMicroSeconds INSTANCE = new ToMicroSeconds();

        private ToMicroSeconds() {
            super("toMicroSeconds", IDataType.LONG, IDataType.LONG);
        }

        @Override
        public Object evaluate(List<Object> args) {
            return ((Number) args.get(0)).longValue() * 1000_000;
        }

        @Override
        public boolean isDeterministic() {
            return true;
        }
    }

    public static class ToMilliSeconds extends AbstractFunction {
        public static final ToMilliSeconds INSTANCE = new ToMilliSeconds();

        private ToMilliSeconds() {
            super("ToMilliSeconds", IDataType.LONG, IDataType.LONG);
        }

        @Override
        public Object evaluate(List<Object> args) {
            return ((Number) args.get(0)).longValue() * 1000;
        }

        @Override
        public boolean isDeterministic() {
            return true;
        }
    }

    /**
     * Accepts a unix timestamp in second and returns a datetime
     */
    public static class FromUnixTimestamp extends AbstractFunction {
        public static final FromUnixTimestamp INSTANCE = new FromUnixTimestamp();

        public FromUnixTimestamp() {
            // TODO: Change the DateTime_3 to DateTime
            super("fromUnixTimestamp", IDataType.LONG, IDataType.DATETIME_3);
        }

        @Override
        public Object evaluate(List<Object> args) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isDeterministic() {
            return true;
        }
    }
}
