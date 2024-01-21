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

package org.bithon.server.alerting.common.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/1/9 09:03
 */
public class PercentageNumber extends Number {
    private final String percentageFormatText;
    private final BigDecimal value;

    public PercentageNumber(String percentageFormatText) {
        this.percentageFormatText = percentageFormatText;
        value = new BigDecimal(percentageFormatText.substring(0, percentageFormatText.length() - 1)).divide(BigDecimal.valueOf(100))
                                                                                                    .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public int intValue() {
        return value.intValue();
    }

    @Override
    public long longValue() {
        return value.longValue();
    }

    @Override
    public float floatValue() {
        return value.floatValue();
    }

    @Override
    public double doubleValue() {
        return value.doubleValue();
    }

    @Override
    public String toString() {
        return percentageFormatText;
    }
}
