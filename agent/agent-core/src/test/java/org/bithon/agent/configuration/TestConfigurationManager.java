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
import org.bithon.agent.configuration.source.Helper;
import org.bithon.agent.configuration.source.PropertySource;
import org.bithon.agent.configuration.source.PropertySourceType;
import org.bithon.component.commons.utils.HumanReadableNumber;
import org.bithon.component.commons.utils.HumanReadablePercentage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/1/6 20:09
 */
public class TestConfigurationManager {

    @ConfigurationProperties(path = "test")
    static class TestConfig {
        private int a;
        private int b;

        private HumanReadablePercentage percentage;

        private HumanReadableNumber number;

        public TestConfig() {
        }

        public TestConfig(int a, int b, String p) {
            this.a = a;
            this.b = b;
            this.percentage = HumanReadablePercentage.of(p);
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

        public HumanReadableNumber getNumber() {
            return number;
        }

        public void setNumber(HumanReadableNumber number) {
            this.number = number;
        }
    }

    @Test
    public void test_DynamicConfiguration() throws IOException {
        ConfigurationManager manager = ConfigurationManager.createForTesting(defaultConfigLocation);
        manager.addPropertySource(PropertySource.from(PropertySourceType.INTERNAL,
                                                      "1",
                                                      "test.a=1\n" +
                                                      "test.b=7\n" +
                                                      "test.percentage=8%\n"
                                                      + "test.number=8KiB"));

        TestConfig testConfig = manager.getConfig(TestConfig.class);
        Assertions.assertEquals(1, testConfig.getA());
        Assertions.assertEquals(7, testConfig.getB());
        Assertions.assertEquals("8%", testConfig.getPercentage().toString());
        Assertions.assertEquals("8KiB", testConfig.getNumber().toString());

        //
        // Use new value to refresh the old one
        //
        manager.addPropertySource(PropertySource.from(PropertySourceType.INTERNAL,
                                                      "2",
                                                      "test.a=2\ntest.b=8\ntest.percentage=500%"
        ));
        Assertions.assertEquals(2, testConfig.getA());
        Assertions.assertEquals(8, testConfig.getB());
        Assertions.assertEquals(5, testConfig.getPercentage().intValue());

        // Get properties that do not exist in the configuration, a default value is returned
        Boolean v = manager.getConfig("do.not.exists", Boolean.class);
        Assertions.assertFalse(v);
    }

    @ConfigurationProperties(path = "test")
    static class TestProp {
        private String prop;

        public String getProp() {
            return prop;
        }

        public void setProp(String prop) {
            this.prop = prop;
        }
    }

    private final File defaultConfigLocation = new File("src/test/resources/conf/agent.yml");
    private final File externalConfigLocation = new File("src/test/resources/conf/external.yml");

    @Test
    public void test_ConfigurationFromFile() {
        ConfigurationManager manager = ConfigurationManager.createForTesting(defaultConfigLocation);
        TestProp config = manager.getConfig(TestProp.class);
        Assertions.assertEquals("from default file", config.getProp());
    }

    @Test
    public void test_FromExternalFile() {
        try (MockedStatic<Helper> configurationMock = Mockito.mockStatic(Helper.class)) {
            configurationMock.when(Helper::getCommandLineInputArgs)
                             .thenReturn(Arrays.asList("-Xms512M",
                                                       // A property without assignment
                                                       // to verify the processing is correct with such configuration
                                                       "-Dbithon.test",
                                                       // Set the external configuration through property
                                                       "-Dbithon.configuration.location=" + externalConfigLocation));

            ConfigurationManager manager = ConfigurationManager.createForTesting(defaultConfigLocation);

            TestProp config = manager.getConfig(TestProp.class);
            Assertions.assertEquals("from external file", config.getProp());
        }
    }

    @Test
    public void test_CommandLineArgs() {
        try (MockedStatic<Helper> configurationMock = Mockito.mockStatic(Helper.class)) {
            configurationMock.when(Helper::getCommandLineInputArgs)
                             .thenReturn(Arrays.asList("-Xms512M",
                                                       // A property without assignment
                                                       // to verify the processing is correct with such configuration
                                                       "-Dbithon.test",
                                                       // Override the in file configuration
                                                       "-Dbithon.test.prop=from_command_line",

                                                       // Also set the external configuration
                                                       "-Dbithon.configuration.location=" + externalConfigLocation
                             ));

            ConfigurationManager manager = ConfigurationManager.createForTesting(defaultConfigLocation);

            TestProp config = manager.getConfig(TestProp.class);
            Assertions.assertEquals("from_command_line", config.getProp());
        }
    }

    /**
     * Environment variables have the highest priority
     */
    @Test
    public void test_Environment() {
        try (MockedStatic<Helper> configurationMock = Mockito.mockStatic(Helper.class)) {
            configurationMock.when(Helper::getCommandLineInputArgs)
                             .thenReturn(Arrays.asList("-Xms512M",
                                                       // A property without assignment
                                                       // to verify the processing is correct with such configuration
                                                       "-Dbithon.test",
                                                       // Override the in file configuration
                                                       "-Dbithon.test.prop=from_command_line",

                                                       // Also set the external configuration
                                                       "-Dbithon.configuration.location=" + externalConfigLocation
                             ));

            configurationMock.when(Helper::getEnvironmentVariables)
                             .thenReturn(ImmutableMap.of("bithon_t", "t1",
                                                         "bithon_test_prop", "from_env"));

            ConfigurationManager manager = ConfigurationManager.createForTesting(defaultConfigLocation);

            TestProp config = manager.getConfig(TestProp.class);
            Assertions.assertEquals("from_env", config.getProp());
        }
    }

    static class TwoProps {
        private String prop1;
        private String prop2;

        public String getProp2() {
            return prop2;
        }

        public void setProp2(String prop2) {
            this.prop2 = prop2;
        }

        public String getProp1() {
            return prop1;
        }

        public void setProp1(String prop1) {
            this.prop1 = prop1;
        }
    }

    @Test
    public void test_PropFromDifferentSource() {
        try (MockedStatic<Helper> configurationMock = Mockito.mockStatic(Helper.class)) {
            configurationMock.when(Helper::getCommandLineInputArgs)
                             .thenReturn(Arrays.asList("-Xms512M",
                                                       // A property without assignment
                                                       // to verify the processing is correct with such configuration
                                                       "-Dbithon.test.prop1=from_command_line",
                                                       // Override the in file configuration
                                                       "-Dbithon.test.prop2=from_command_line",

                                                       // Also set the external configuration
                                                       "-Dbithon.configuration.location=" + externalConfigLocation
                             ));

            configurationMock.when(Helper::getEnvironmentVariables)
                             .thenReturn(ImmutableMap.of("bithon_t", "t1",
                                                         //Overwrite the prop2
                                                         "bithon_test_prop2", "from_env"));

            ConfigurationManager manager = ConfigurationManager.createForTesting(defaultConfigLocation);

            TwoProps config = manager.getConfig("test", TwoProps.class);
            Assertions.assertEquals("from_command_line", config.getProp1());
            Assertions.assertEquals("from_env", config.getProp2());
        }
    }

    static class ApplyChangeTestConfig {
        private String prop;
        private String prop1;
        private String prop2;
        private String prop3;
        private String prop4;
        private String prop5;

        public String getProp() {
            return prop;
        }

        public void setProp(String prop) {
            this.prop = prop;
        }

        public String getProp1() {
            return prop1;
        }

        public void setProp1(String prop1) {
            this.prop1 = prop1;
        }

        public String getProp2() {
            return prop2;
        }

        public void setProp2(String prop2) {
            this.prop2 = prop2;
        }

        public String getProp3() {
            return prop3;
        }

        public void setProp3(String prop3) {
            this.prop3 = prop3;
        }

        public String getProp4() {
            return prop4;
        }

        public void setProp4(String prop4) {
            this.prop4 = prop4;
        }

        public String getProp5() {
            return prop5;
        }

        public void setProp5(String prop5) {
            this.prop5 = prop5;
        }
    }

    @Test
    public void test_ApplyChanges() throws IOException {
        ConfigurationManager manager = ConfigurationManager.createForTesting(defaultConfigLocation);

        ApplyChangeTestConfig bean = manager.getConfig("test", ApplyChangeTestConfig.class, true);
        Assertions.assertEquals("from default file", bean.getProp());

        // Add two new configurations
        manager.applyChanges(Collections.emptyList(),
                             Collections.emptyMap(),
                             Arrays.asList(PropertySource.from(PropertySourceType.DYNAMIC, "d1", "test.prop=a"),
                                           PropertySource.from(PropertySourceType.DYNAMIC, "d2", "test.prop1=from_d2")));
        HashMap<?, ?> map = manager.getConfig("test", HashMap.class);
        Assertions.assertEquals(2, map.size());
        Assertions.assertEquals("a", map.get("prop"));
        Assertions.assertEquals("from_d2", map.get("prop1"));

        // The bean Should be updated
        Assertions.assertEquals("a", bean.getProp());
        Assertions.assertEquals("from_d2", bean.getProp1());

        //
        // Remove 'd1'
        //
        manager.applyChanges(Collections.singletonList("d1"),
                             Collections.emptyMap(),
                             Collections.emptyList());
        map = manager.getConfig("test", HashMap.class);
        Assertions.assertEquals(2, map.size());
        Assertions.assertEquals("from default file", map.get("prop"));
        Assertions.assertEquals("from_d2", map.get("prop1"));

        // The bean Should be updated
        Assertions.assertEquals("from default file", bean.getProp());
        Assertions.assertEquals("from_d2", bean.getProp1());

        // Remove d2,
        // Add d3
        manager.applyChanges(Collections.singletonList("d2"),
                             Collections.emptyMap(),
                             Collections.singletonList(PropertySource.from(PropertySourceType.DYNAMIC, "d3", "test.prop1=from_d3")));
        map = manager.getConfig("test", HashMap.class);
        Assertions.assertEquals(2, map.size());
        Assertions.assertEquals("from default file", map.get("prop"));
        Assertions.assertEquals("from_d3", map.get("prop1"));

        Assertions.assertEquals("from default file", bean.getProp());
        Assertions.assertEquals("from_d3", bean.getProp1());


        // Replace d3, add d4
        manager.applyChanges(Collections.emptyList(),
                             ImmutableMap.of("d3", PropertySource.from(PropertySourceType.DYNAMIC, "d3", "test.prop3=from_d3")),
                             Collections.singletonList(PropertySource.from(PropertySourceType.DYNAMIC, "d4", "test.prop4=from_d4")));
        map = manager.getConfig("test", HashMap.class);
        Assertions.assertEquals(3, map.size());
        Assertions.assertEquals("from default file", map.get("prop"));
        Assertions.assertEquals("from_d3", map.get("prop3"));
        Assertions.assertEquals("from_d4", map.get("prop4"));

        Assertions.assertEquals("from default file", bean.getProp());
        Assertions.assertNull(bean.getProp1());
        Assertions.assertNull(bean.getProp2());
        Assertions.assertEquals("from_d3", bean.getProp3());
        Assertions.assertEquals("from_d4", bean.getProp4());
    }

    @Test
    public void test_BindToSimpleTypes() {
        try (MockedStatic<Helper> configurationMock = Mockito.mockStatic(Helper.class)) {
            configurationMock.when(Helper::getCommandLineInputArgs)
                             .thenReturn(Arrays.asList("-Xms512M",
                                                       // A property without assignment
                                                       // to verify the processing is correct with such configuration
                                                       "-Dbithon.test.a=1",
                                                       // Override the in file configuration
                                                       "-Dbithon.test.b=true",
                                                       "-Dbithon.test.percentage=8%"
                             ));

            ConfigurationManager manager = ConfigurationManager.createForTesting(defaultConfigLocation);

            Assertions.assertEquals("8%", manager.getConfig("test.percentage", String.class, true));
            Assertions.assertEquals("8%", manager.getConfig("test.percentage", HumanReadablePercentage.class).toString());
            Assertions.assertEquals(1, (int) manager.getConfig("test.a", Integer.class));
            Assertions.assertEquals(true, manager.getConfig("test.b", Boolean.class));
        }
    }

    @Test
    public void test_BindToArray() {
        try (MockedStatic<Helper> configurationMock = Mockito.mockStatic(Helper.class)) {
            configurationMock.when(Helper::getCommandLineInputArgs)
                             .thenReturn(Arrays.asList("-Xms512M",
                                                       // Array configuration
                                                       "-Dbithon.test.b[0]=1",
                                                       "-Dbithon.test.b[1]=2",
                                                       "-Dbithon.test.b[3]=3",
                                                       "-Dbithon.test.p[0]=8%",
                                                       "-Dbithon.test.p[2]=9%",
                                                       "-Dbithon.test.p[3]=10%"
                             ));

            ConfigurationManager manager = ConfigurationManager.createForTesting(defaultConfigLocation);

            Assertions.assertArrayEquals(new int[]{1, 2, 3}, manager.getConfig("test.b", int[].class, true));
            Assertions.assertArrayEquals(new HumanReadablePercentage[]{
                                             HumanReadablePercentage.of("8%"),
                                             HumanReadablePercentage.of("9%"),
                                             HumanReadablePercentage.of("10%")
                                         },
                                         manager.getConfig("test.p", HumanReadablePercentage[].class, true));
        }
    }

    @ConfigurationProperties(path = "test.arrayList")
    public static class StringListConfig extends ArrayList<String> {
    }

    @Test
    public void test_BindToArray_ReplaceDefault() {
        // If the property is not given, the default value is the one in the external configuration file
        {
            ConfigurationManager manager = ConfigurationManager.createForTesting(externalConfigLocation);
            StringListConfig config = manager.getConfig(StringListConfig.class);
            Assertions.assertEquals(Collections.singletonList("from file a"), config);
        }

        // When the property is given, the default one is overridden
        try (MockedStatic<Helper> configurationMock = Mockito.mockStatic(Helper.class)) {
            configurationMock.when(Helper::getCommandLineInputArgs)
                             .thenReturn(Arrays.asList("-Xms512M",
                                                       // Array configuration
                                                       "-Dbithon.test.arrayList[0]=1",
                                                       "-Dbithon.test.arrayList[1]=2"
                             ));

            ConfigurationManager manager = ConfigurationManager.createForTesting(externalConfigLocation);
            StringListConfig config = manager.getConfig(StringListConfig.class);
            Assertions.assertEquals(Arrays.asList("1", "2"), config);
        }
    }
}
