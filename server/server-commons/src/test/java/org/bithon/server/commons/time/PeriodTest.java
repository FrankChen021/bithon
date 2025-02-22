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

package org.bithon.server.commons.time;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author frank.chen021@outlook.com
 * @date 8/4/22 2:08 PM
 */
public class PeriodTest {

    @Test
    public void testZeroDay() {
        Period period = new Period("P0D");

        Assertions.assertEquals(0, period.getMilliseconds());
    }

    @Test
    public void testZeroHour() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Period("P0H"));
    }

}
