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
import org.bithon.component.commons.expression.function.Parameter;

import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/7/18 21:31
 */
public class TimeFunction {
    public static class ToStartOfMinute extends AbstractFunction {
        public ToStartOfMinute() {
            super("toStartOfMinute", new Parameter(IDataType.LONG), IDataType.LONG);
        }

        @Override
        public Object evaluate(List<Object> parameters) {
            Object o = parameters.get(0);
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
        public Object evaluate(List<Object> parameters) {
            return System.currentTimeMillis() / 1000;
        }

        @Override
        public boolean isDeterministic() {
            return false;
        }
    }
}
