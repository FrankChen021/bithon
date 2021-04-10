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

package com.sbss.bithon.server.common.utils;

import java.math.BigDecimal;

/**
 * @author frankchen
 * @Date 2020-08-27 17:28:11
 */
public class NumberUtils {

    public static double div(double dividend,
                             double divisor,
                             int scale) {
        try {
            return new BigDecimal(dividend / divisor).setScale(scale, BigDecimal.ROUND_HALF_UP).doubleValue();
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static double div(BigDecimal dividend,
                             BigDecimal divisor,
                             int scale) {
        if (divisor.equals(BigDecimal.ZERO)) {
            return dividend.doubleValue();
        }
        return dividend.divide(divisor, scale, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    public static BigDecimal scaleTo(double val,
                                     int scale) {
        return new BigDecimal(val).setScale(scale, BigDecimal.ROUND_HALF_UP);
    }

    public static String toString(double val,
                                  int scale) {
        return BigDecimal.valueOf(val).setScale(scale, BigDecimal.ROUND_HALF_UP).toString();
    }
}
