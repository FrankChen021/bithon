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

package org.bithon.agent.configuration;

import com.google.common.collect.ImmutableMap;
import org.bithon.component.commons.utils.HumanReadablePercentage;
import org.bithon.shaded.com.fasterxml.jackson.databind.JsonNode;
import org.bithon.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/1/6 20:09
 */
public class TestConfigurationManager {

    @ConfigurationProperties(prefix = "test")
    static class TestConfig {
        private int a;
        private int b;

        private HumanReadablePercentage percentage;

        public TestConfig() {
        }

        public TestConfig(int a, int b, String p) {
            this.a = a;
            this.b = b;
            this.percentage = HumanReadablePercentage.parse(p);
        }

        public int getA() {
            return a;
        }

        public void setA(int a) {
            this.a = a;
        }

        public int getB() {
            return b;
        }

        public void setB(int b) {
            this.b = b;
        }

        public HumanReadablePercentage getPercentage() {
            return percentage;
        }

        public void setPercentage(HumanReadablePercentage percentage) {
            this.percentage = percentage;
        }
    }

    private final ObjectMapper objectMapper = ObjectMapperConfigurer.configure(new ObjectMapper());

    @Test
    public void test_DynamicConfiguration() throws IOException {
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsBytes(Collections.singletonMap("test", new TestConfig(1, 7, "8%"))));
        ConfigurationManager manager = ConfigurationManager.create(new Configuration(json));

        TestConfig testConfig = manager.getConfig(TestConfig.class);
        Assert.assertEquals(1, testConfig.getA());
        Assert.assertEquals(7, testConfig.getB());
        Assert.assertEquals("8%", testConfig.getPercentage().toString());

        //
        // Use new value to refresh the old one
        //
        JsonNode json2 = objectMapper.readTree(objectMapper.writeValueAsBytes(Collections.singletonMap("test", new TestConfig(2, 8, "500%"))));
        manager.refresh(new Configuration(json2));
        Assert.assertEquals(2, testConfig.getA());
        Assert.assertEquals(8, testConfig.getB());
        Assert.assertEquals(5, testConfig.getPercentage().intValue());
    }

    @ConfigurationProperties(prefix = "test")
    static class TestProp {
        private String prop;

        public TestProp() {
        }

        public String getProp() {
            return prop;
        }

        public void setProp(String prop) {
            this.prop = prop;
        }
    }

    private final String defaultConfigLocation = new File("src/test/resources/conf/agent.yml").getAbsolutePath();
    private final String externalConfigLocation = new File("src/test/resources/conf/external.yml").getAbsolutePath();

    @Test
    public void test_ConfigurationFromFile() {
        ConfigurationManager manager = ConfigurationManager.create(defaultConfigLocation);
        TestProp config = manager.getConfig(TestProp.class);
        Assert.assertEquals("from default file", config.getProp());
    }

    @Test
    public void test_FromExternalFile() {
        try (MockedStatic<Configuration.ConfigurationHelper> configurationMock = Mockito.mockStatic(Configuration.ConfigurationHelper.class)) {
            configurationMock.when(Configuration.ConfigurationHelper::getCommandLineInputArgs)
                             .thenReturn(Arrays.asList("-Xms512M",
                                                       // A property without assignment
                                                       // to verify the processing is correct with such configuration
                                                       "-Dbithon.test",
                                                       // Set the external configuration through property
                                                       "-Dbithon.configuration.location=" + externalConfigLocation));

            ConfigurationManager manager = ConfigurationManager.create(defaultConfigLocation);

            TestProp config = manager.getConfig(TestProp.class);
            Assert.assertEquals("from external file", config.getProp());
        }
    }

    @Test
    public void test_CommandLineArgs() {
        try (MockedStatic<Configuration.ConfigurationHelper> configurationMock = Mockito.mockStatic(Configuration.ConfigurationHelper.class)) {
            configurationMock.when(Configuration.ConfigurationHelper::getCommandLineInputArgs)
                             .thenReturn(Arrays.asList("-Xms512M",
                                                       // A property without assignment
                                                       // to verify the processing is correct with such configuration
                                                       "-Dbithon.test",
                                                       // Override the in file configuration
                                                       "-Dbithon.test.prop=from_command_line",

                                                       // Also set the external configuration
                                                       "-Dbithon.configuration.location=" + externalConfigLocation
                                                       ));

            ConfigurationManager manager = ConfigurationManager.create(defaultConfigLocation);

            TestProp config = manager.getConfig(TestProp.class);
            Assert.assertEquals("from_command_line", config.getProp());
        }
    }

    /**
     * Environment variables have the highest priority
     */
    @Test
    public void test_Environment() {
        try (MockedStatic<Configuration.ConfigurationHelper> configurationMock = Mockito.mockStatic(Configuration.ConfigurationHelper.class)) {
            configurationMock.when(Configuration.ConfigurationHelper::getCommandLineInputArgs)
                             .thenReturn(Arrays.asList("-Xms512M",
                                                       // A property without assignment
                                                       // to verify the processing is correct with such configuration
                                                       "-Dbithon.test",
                                                       // Override the in file configuration
                                                       "-Dbithon.test.prop=from_command_line",

                                                       // Also set the external configuration
                                                       "-Dbithon.configuration.location=" + externalConfigLocation
                                                       ));

            configurationMock.when(Configuration.ConfigurationHelper::getEnvironmentVariables)
                             .thenReturn(ImmutableMap.of("bithon.t", "t1",
                                                         "bithon.test.prop", "from_env"));

            ConfigurationManager manager = ConfigurationManager.create(defaultConfigLocation);

            TestProp config = manager.getConfig(TestProp.class);
            Assert.assertEquals("from_env", config.getProp());
        }
    }
}