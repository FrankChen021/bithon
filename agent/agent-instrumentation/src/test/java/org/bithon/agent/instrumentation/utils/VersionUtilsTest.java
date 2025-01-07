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

package org.bithon.agent.instrumentation.utils;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author frank.chen021@outlook.com
 * @date 7/1/25 11:56 am
 */
public class VersionUtilsTest {

    @Test
    public void test_EQ() {
        Assert.assertEquals(0, VersionUtils.compare("1.0.0", "1.0.0"));
    }

    @Test
    public void test_LE() {
        Assert.assertEquals(-1, VersionUtils.compare("1.0.0", "1.0.1"));
    }

    @Test
    public void test_GE() {
        Assert.assertEquals(1, VersionUtils.compare("1.0.1", "1.0.0"));
    }

    @Test
    public void test_GE_2() {
        Assert.assertEquals(1, VersionUtils.compare("1.0.11", "1.0.1"));
    }

    @Test
    public void test_GE_3() {
        Assert.assertEquals(1, VersionUtils.compare("1.0.1", "1.0.0.1"));
    }

    @Test
    public void test_GE_4() {
        Assert.assertEquals(1, VersionUtils.compare("1.0.1", "1.0.0.1"));
    }
}
