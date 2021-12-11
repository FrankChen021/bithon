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

package org.bithon.agent.core.utils.lang;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/8/7 19:01
 */
public class MathUtils {

    /**
     * A of copy Apache commons-math 3.6.1 FastMath.floorMod(long, long)
     * Finds q such that dividend = q * divisor + r with 0 &lt;= r &lt; divisor if divisor &gt; 0 and divisor &lt; r &lt;= 0 if divisor &lt; 0.
     * <p>
     * This methods returns the same value as integer division when
     * a and b are same signs, but returns a different value when
     * they are opposite (i.e. q is negative).
     * </p>
     */
    public static long floorMod(final long dividend, final long divisor) {

        if (divisor == 0L) {
            throw new IllegalArgumentException("denominator must be different from 0");
        }

        final long m = dividend % divisor;
        if ((dividend ^ divisor) >= 0L || m == 0L) {
            // a an b have same sign, or division is exact
            return m;
        } else {
            // a and b have opposite signs and division is not exact
            return divisor + m;
        }
    }
}
