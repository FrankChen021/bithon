/*
 *    Copyright 2020 bithon.cn
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

package com.sbss.bithon.server.metric.typing;

import java.text.DecimalFormat;

/**
 * @author frank.chen021@outlook.com
 * @date
 */
public class LongValueType implements IValueType {

    public static final IValueType INSTANCE = new LongValueType();

    @Override
    public String format(Number value) {
        return new DecimalFormat("#,###").format(value.longValue());
    }

    @Override
    public boolean isGreaterThan(Number left, Number right) {
        return left.longValue() > right.longValue();
    }

    @Override
    public boolean isGreaterThanOrEqual(Number left, Number right) {
        return left.longValue() >= right.longValue();
    }

    @Override
    public boolean isLessThan(Number left, Number right) {
        return left.longValue() < right.longValue();
    }

    @Override
    public boolean isLessThanOrEqual(Number left, Number right) {
        return left.longValue() <= right.longValue();
    }

    @Override
    public boolean isEqual(Number left, Number right) {
        return left.longValue() == right.longValue();
    }

    @Override
    public Number diff(Number left, Number right) {
        return left.longValue() - right.longValue();
    }

    @Override
    public Number scaleTo(Number value, int scale) {
        return value;
    }
}
