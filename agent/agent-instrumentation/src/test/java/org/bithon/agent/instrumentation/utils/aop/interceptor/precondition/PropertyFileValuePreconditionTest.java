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

package org.bithon.agent.instrumentation.utils.aop.interceptor.precondition;

import org.bithon.agent.instrumentation.aop.interceptor.precondition.PropertyFileValuePrecondition;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author frank.chen021@outlook.com
 * @date 2025/1/17 21:18
 */
public class PropertyFileValuePreconditionTest {
    @Test
    public void test_EQ_Match() {
        boolean matches = new PropertyFileValuePrecondition("version.properties",
                                                            "v1",
                                                            PropertyFileValuePrecondition.StringEQ.of("1.0.0"))
            .matches(PropertyFileValuePreconditionTest.class.getClassLoader(), null);
        Assert.assertTrue(matches);
    }

    @Test
    public void test_VersionGT() {
        boolean matches = new PropertyFileValuePrecondition("version.properties",
                                                            "v1",
                                                            PropertyFileValuePrecondition.VersionGT.of("0.1.0"))
            .matches(PropertyFileValuePreconditionTest.class.getClassLoader(), null);
        Assert.assertTrue(matches);
    }

    @Test
    public void test_VersionGTE() {
        boolean matches = new PropertyFileValuePrecondition("version.properties",
                                                            "v1",
                                                            PropertyFileValuePrecondition.VersionGT.of("1.0.0"))
            .matches(PropertyFileValuePreconditionTest.class.getClassLoader(), null);
        Assert.assertTrue(matches);

        matches = new PropertyFileValuePrecondition("version.properties",
                                                    "v1",
                                                    PropertyFileValuePrecondition.VersionGT.of("0.0.9"))
            .matches(PropertyFileValuePreconditionTest.class.getClassLoader(), null);
        Assert.assertTrue(matches);
    }

    @Test
    public void test_VersionLT() {
        {
            // LT
            boolean matches = new PropertyFileValuePrecondition("version.properties",
                                                                "v1",
                                                                PropertyFileValuePrecondition.VersionLT.of("2.0.0"))
                .matches(PropertyFileValuePreconditionTest.class.getClassLoader(), null);
            Assert.assertTrue(matches);
        }
        {
            // Same value
            boolean matches = new PropertyFileValuePrecondition("version.properties",
                                                                "v1",
                                                                PropertyFileValuePrecondition.VersionLT.of("1.0.0"))
                .matches(PropertyFileValuePreconditionTest.class.getClassLoader(), null);
            Assert.assertFalse(matches);
        }
        {
            // Same value
            boolean matches = new PropertyFileValuePrecondition("version.properties",
                                                                "v1",
                                                                PropertyFileValuePrecondition.VersionLT.of("0.0.9"))
                .matches(PropertyFileValuePreconditionTest.class.getClassLoader(), null);
            Assert.assertFalse(matches);
        }
    }

    @Test
    public void test_And() {
        {
            boolean matches = new PropertyFileValuePrecondition("version.properties",
                                                                "v2",
                                                                PropertyFileValuePrecondition.and(
                                                                    PropertyFileValuePrecondition.VersionGTE.of("0.10.2.0"),
                                                                    PropertyFileValuePrecondition.VersionLT.of("0.11.0.0")
                                                                ))
                .matches(PropertyFileValuePreconditionTest.class.getClassLoader(), null);
            Assert.assertTrue(matches);
        }
    }
}
