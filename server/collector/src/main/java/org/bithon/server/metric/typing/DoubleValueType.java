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

package org.bithon.server.metric.typing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * @author
 * @date
 */
public class DoubleValueType implements IValueType {

    public static final IValueType INSTANCE = new DoubleValueType();

    @Override
    public String format(Number value) {
        return new DecimalFormat("#,###.00").format(value.doubleValue());
    }

    @Override
    public boolean isGreaterThan(Number left, Number right) {
        return left.doubleValue() > right.doubleValue();
    }

    @Override
    public boolean isGreaterThanOrEqual(Number left, Number right) {
        return left.doubleValue() >= right.doubleValue();
    }

    @Override
    public boolean isLessThan(Number left, Number right) {
        return left.doubleValue() < right.doubleValue();
    }

    @Override
    public boolean isLessThanOrEqual(Number left, Number right) {
        return left.doubleValue() <= right.doubleValue();
    }

    @Override
    public boolean isEqual(Number left, Number right) {
        return left.doubleValue() == right.doubleValue();
    }

    @Override
    public Number diff(Number left, Number right) {
        return left.doubleValue() - right.doubleValue();
    }

    @Override
    public Number scaleTo(Number value, int scale) {
        return BigDecimal.valueOf(value.doubleValue()).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }
}
