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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/7/18 21:29
 */
public class NumberFunction {
    public static class Round extends AbstractFunction {
        public Round() {
            super("round",
                  Arrays.asList(IDataType.DOUBLE, IDataType.LONG),
                  IDataType.DOUBLE);
        }

        @Override
        public Object evaluate(List<Object> parameters) {
            double i0 = ((Number) parameters.get(0)).doubleValue();
            int scale = ((Number) parameters.get(1)).intValue();
            return BigDecimal.valueOf(i0).setScale(scale, RoundingMode.HALF_UP);
        }

        @Override
        public boolean isDeterministic() {
            return true;
        }
    }
}
