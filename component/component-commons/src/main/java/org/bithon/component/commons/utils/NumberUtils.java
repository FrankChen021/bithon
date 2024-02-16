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

package org.bithon.component.commons.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/4/6 9:34 下午
 */
public class NumberUtils {
    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    public static long getLong(Object value, long defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public static int getInteger(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public static double getDouble(Object value, double defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public static String toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte v : bytes) {
            sb.append(HEX_CHARS[(v & 0xF0) >>> 4]);
            sb.append(HEX_CHARS[v & 0x0F]);
        }
        return sb.toString();
    }

    public static BigDecimal scaleTo(double val,
                                     int scale) {
        return new BigDecimal(val).setScale(scale, RoundingMode.HALF_UP);
    }

    public static String toString(double val,
                                  int scale) {
        return BigDecimal.valueOf(val).setScale(scale, RoundingMode.HALF_UP).toString();
    }

    /**
     * A copy of Apache commons-math 3.6.1
     * <a href="https://github.com/apache/commons-math/blob/3.6.1-release/src/main/java/org/apache/commons/math3/util/FastMath.java">FastMath.floorMod(long, long)</a>
     * <p>
     * This helps us calculate probability in a more determinant way
     * so that the UT tests are not flaky compared to using random-based way.
     * <p>
     * Finds q such that dividend = q * divisor + r with 0 &lt;= r &lt; divisor if divisor &gt; 0 and divisor &lt; r &lt;= 0 if divisor &lt; 0.
     * <p>
     * This methods returns the same value as integer division when
     * a and b are same signs, but returns a different value when
     * they are opposite (i.e. q is negative).
     * </p>
     * @throws IllegalArgumentException if the given divisor is zero
     */
    public static long floorMod(final long dividend, final long divisor) {
        if (divisor == 0L) {
            throw new IllegalArgumentException("divisor must be not zero");
        }

        final long m = dividend % divisor;
        if ((dividend ^ divisor) >= 0L || m == 0L) {
            // dividend and divisor have the same sign, or division is exact
            return m;
        } else {
            // dividend and divisor have opposite signs and division is not exact
            return divisor + m;
        }
    }
}
