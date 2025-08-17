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

package org.bithon.agent.controller.config;

import org.bithon.agent.configuration.annotation.ConfigurationProperties;
import org.bithon.agent.configuration.metadata.PropertyMetadata;
import org.bithon.component.commons.utils.HumanReadableDuration;
import org.bithon.component.commons.utils.HumanReadableNumber;
import org.bithon.component.commons.utils.HumanReadablePercentage;
import org.bithon.shaded.com.fasterxml.jackson.databind.JsonNode;
import org.bithon.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests for ConfigurationDefaultValueExtractor
 *
 * @author frank.chen021@outlook.com
 */
public class ConfigurationDefaultValueExtractorTest {

    /**
     * Simple configuration class with primitive and string fields
     */
    @ConfigurationProperties(path = "test.basic")
    public static class BasicConfig {
        private String stringValue = "default-string";
        private int intValue = 42;
        private long longValue = 1000000L;
        private boolean booleanValue = true;
        private double doubleValue = 3.14159;

        // Getters and setters
        public String getStringValue() {
            return stringValue;
        }

        public void setStringValue(String stringValue) {
            this.stringValue = stringValue;
        }

        public int getIntValue() {
            return intValue;
        }

        public void setIntValue(int intValue) {
            this.intValue = intValue;
        }

        public long getLongValue() {
            return longValue;
        }

        public void setLongValue(long longValue) {
            this.longValue = longValue;
        }

        public boolean isBooleanValue() {
            return booleanValue;
        }

        public void setBooleanValue(boolean booleanValue) {
            this.booleanValue = booleanValue;
        }

        public double getDoubleValue() {
            return doubleValue;
        }

        public void setDoubleValue(double doubleValue) {
            this.doubleValue = doubleValue;
        }
    }

    @Test
    public void testBasicTypes() {
        // Create property metadata for BasicConfig
        List<PropertyMetadata> properties = new ArrayList<>();

        // String property
        PropertyMetadata stringProperty = new PropertyMetadata();
        stringProperty.setPath("test.basic.stringValue");
        stringProperty.setType("java.lang.String");
        stringProperty.setConfigurationClass(BasicConfig.class.getName());
        properties.add(stringProperty);

        // Integer property
        PropertyMetadata intProperty = new PropertyMetadata();
        intProperty.setPath("test.basic.intValue");
        intProperty.setType("int");
        intProperty.setConfigurationClass(BasicConfig.class.getName());
        properties.add(intProperty);

        // Long property
        PropertyMetadata longProperty = new PropertyMetadata();
        longProperty.setPath("test.basic.longValue");
        longProperty.setType("long");
        longProperty.setConfigurationClass(BasicConfig.class.getName());
        properties.add(longProperty);

        // Boolean property
        PropertyMetadata booleanProperty = new PropertyMetadata();
        booleanProperty.setPath("test.basic.booleanValue");
        booleanProperty.setType("boolean");
        booleanProperty.setConfigurationClass(BasicConfig.class.getName());
        properties.add(booleanProperty);

        // Double property
        PropertyMetadata doubleProperty = new PropertyMetadata();
        doubleProperty.setPath("test.basic.doubleValue");
        doubleProperty.setType("double");
        doubleProperty.setConfigurationClass(BasicConfig.class.getName());
        properties.add(doubleProperty);

        // Extract default values
        ConfigurationDefaultValueExtractor.extractDefaultValues(properties);

        // Verify extracted values with exact string representation
        Assertions.assertEquals("default-string", stringProperty.getDefaultValue());
        Assertions.assertEquals("42", intProperty.getDefaultValue());
        Assertions.assertEquals("1000000", longProperty.getDefaultValue());
        Assertions.assertEquals("true", booleanProperty.getDefaultValue());
        Assertions.assertEquals("3.14159", doubleProperty.getDefaultValue());
    }

    /**
     * Configuration class with utility types
     */
    @ConfigurationProperties(path = "test.utility")
    public static class UtilityTypesConfig {
        private HumanReadableNumber number = HumanReadableNumber.of("10M");
        private HumanReadableDuration duration = HumanReadableDuration.parse("5m");
        private HumanReadablePercentage percentage = HumanReadablePercentage.of("75%");

        // Getters and setters
        public HumanReadableNumber getNumber() {
            return number;
        }

        public void setNumber(HumanReadableNumber number) {
            this.number = number;
        }

        public HumanReadableDuration getDuration() {
            return duration;
        }

        public void setDuration(HumanReadableDuration duration) {
            this.duration = duration;
        }

        public HumanReadablePercentage getPercentage() {
            return percentage;
        }

        public void setPercentage(HumanReadablePercentage percentage) {
            this.percentage = percentage;
        }
    }

    @Test
    public void testUtilityTypes() {
        // Create property metadata for UtilityTypesConfig
        List<PropertyMetadata> properties = new ArrayList<>();

        // HumanReadableNumber property
        PropertyMetadata numberProperty = new PropertyMetadata();
        numberProperty.setPath("test.utility.number");
        numberProperty.setType("org.bithon.component.commons.utils.HumanReadableNumber");
        numberProperty.setConfigurationClass(UtilityTypesConfig.class.getName());
        properties.add(numberProperty);

        // HumanReadableDuration property
        PropertyMetadata durationProperty = new PropertyMetadata();
        durationProperty.setPath("test.utility.duration");
        durationProperty.setType("org.bithon.component.commons.utils.HumanReadableDuration");
        durationProperty.setConfigurationClass(UtilityTypesConfig.class.getName());
        properties.add(durationProperty);

        // HumanReadablePercentage property
        PropertyMetadata percentageProperty = new PropertyMetadata();
        percentageProperty.setPath("test.utility.percentage");
        percentageProperty.setType("org.bithon.component.commons.utils.HumanReadablePercentage");
        percentageProperty.setConfigurationClass(UtilityTypesConfig.class.getName());
        properties.add(percentageProperty);

        // Extract default values
        ConfigurationDefaultValueExtractor.extractDefaultValues(properties);

        // Verify extracted values with exact string representation
        Assertions.assertEquals("10M", numberProperty.getDefaultValue());
        Assertions.assertEquals("5m", durationProperty.getDefaultValue());
        Assertions.assertEquals("75%", percentageProperty.getDefaultValue());
    }

    /**
     * Configuration class with collection and map fields
     */
    @ConfigurationProperties(path = "test.collections")
    public static class CollectionsConfig {
        private List<String> stringList = new ArrayList<>(Arrays.asList("item1", "item2", "item3"));
        private Map<String, Integer> integerMap = new HashMap<>();
        private String[] stringArray = {"array1", "array2", "array3"};

        public CollectionsConfig() {
            integerMap.put("key1", 1);
            integerMap.put("key2", 2);
            integerMap.put("key3", 3);
        }

        // Getters and setters
        public List<String> getStringList() {
            return stringList;
        }

        public void setStringList(List<String> stringList) {
            this.stringList = stringList;
        }

        public Map<String, Integer> getIntegerMap() {
            return integerMap;
        }

        public void setIntegerMap(Map<String, Integer> integerMap) {
            this.integerMap = integerMap;
        }

        public String[] getStringArray() {
            return stringArray;
        }

        public void setStringArray(String[] stringArray) {
            this.stringArray = stringArray;
        }
    }

    @Test
    public void testCollectionTypes() throws Exception {
        // Create property metadata for CollectionsConfig
        List<PropertyMetadata> properties = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();

        // List property
        PropertyMetadata listProperty = new PropertyMetadata();
        listProperty.setPath("test.collections.stringList");
        listProperty.setType("java.util.List");
        listProperty.setConfigurationClass(CollectionsConfig.class.getName());
        properties.add(listProperty);

        // Map property
        PropertyMetadata mapProperty = new PropertyMetadata();
        mapProperty.setPath("test.collections.integerMap");
        mapProperty.setType("java.util.Map");
        mapProperty.setConfigurationClass(CollectionsConfig.class.getName());
        properties.add(mapProperty);

        // Array property
        PropertyMetadata arrayProperty = new PropertyMetadata();
        arrayProperty.setPath("test.collections.stringArray");
        arrayProperty.setType("java.lang.String[]");
        arrayProperty.setConfigurationClass(CollectionsConfig.class.getName());
        properties.add(arrayProperty);

        // Extract default values
        ConfigurationDefaultValueExtractor.extractDefaultValues(properties);

        // Verify list value
        String listValue = listProperty.getDefaultValue();
        Assertions.assertNotNull(listValue);
        // Verify exact string value
        Assertions.assertEquals("[\"item1\",\"item2\",\"item3\"]", listValue);

        // Verify map value
        String mapValue = mapProperty.getDefaultValue();
        Assertions.assertNotNull(mapValue);
        // Verify exact string value - note: order may vary in JSON object serialization
        // We'll use a normalized approach to verify the content
        JsonNode mapNode = mapper.readTree(mapValue);
        String normalizedMapValue = mapper.writeValueAsString(mapNode);
        Assertions.assertEquals("{\"key1\":1,\"key2\":2,\"key3\":3}", normalizedMapValue);

        // Verify array value
        String arrayValue = arrayProperty.getDefaultValue();
        Assertions.assertNotNull(arrayValue);
        // Verify exact string value
        Assertions.assertEquals("[\"array1\",\"array2\",\"array3\"]", arrayValue);
    }

    /**
     * Configuration class with nested objects
     */
    @ConfigurationProperties(path = "test.nested")
    public static class NestedConfig {
        private String name = "nested-config";
        private NestedObject nestedObject = new NestedObject();

        // Getters and setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public NestedObject getNestedObject() {
            return nestedObject;
        }

        public void setNestedObject(NestedObject nestedObject) {
            this.nestedObject = nestedObject;
        }

        public static class NestedObject {
            private String nestedValue = "nested-default";
            private int nestedInt = 500;

            // Getters and setters
            public String getNestedValue() {
                return nestedValue;
            }

            public void setNestedValue(String nestedValue) {
                this.nestedValue = nestedValue;
            }

            public int getNestedInt() {
                return nestedInt;
            }

            public void setNestedInt(int nestedInt) {
                this.nestedInt = nestedInt;
            }

            @Override
            public String toString() {
                return "NestedObject{" +
                       "nestedValue='" + nestedValue + '\'' +
                       ", nestedInt=" + nestedInt +
                       '}';
            }
        }
    }

    @Test
    public void testNestedObjects() {
        // Create property metadata for NestedConfig
        List<PropertyMetadata> properties = new ArrayList<>();

        // Simple property
        PropertyMetadata nameProperty = new PropertyMetadata();
        nameProperty.setPath("test.nested.name");
        nameProperty.setType("java.lang.String");
        nameProperty.setConfigurationClass(NestedConfig.class.getName());
        properties.add(nameProperty);

        // Nested object property
        PropertyMetadata nestedProperty = new PropertyMetadata();
        nestedProperty.setPath("test.nested.nestedObject");
        nestedProperty.setType(NestedConfig.NestedObject.class.getName());
        nestedProperty.setConfigurationClass(NestedConfig.class.getName());
        properties.add(nestedProperty);

        // Extract default values
        ConfigurationDefaultValueExtractor.extractDefaultValues(properties);

        // Verify simple property
        Assertions.assertEquals("nested-config", nameProperty.getDefaultValue());

        // Verify nested object property - should be converted to string representation
        String nestedValue = nestedProperty.getDefaultValue();
        Assertions.assertNotNull(nestedValue);
        // Verify raw string format
        Assertions.assertEquals("NestedObject{nestedValue='nested-default', nestedInt=500}", nestedValue);
        // Verify specific content
        Assertions.assertTrue(nestedValue.contains("nestedValue='nested-default'"));
        Assertions.assertTrue(nestedValue.contains("nestedInt=500"));
    }

    /**
     * Parent configuration class for inheritance testing
     */
    public static class ParentConfig {
        private int parentValue = 100;
        private String parentString = "parent-default";

        // Getters and setters
        public int getParentValue() {
            return parentValue;
        }

        public void setParentValue(int parentValue) {
            this.parentValue = parentValue;
        }

        public String getParentString() {
            return parentString;
        }

        public void setParentString(String parentString) {
            this.parentString = parentString;
        }
    }

    /**
     * Child configuration class that inherits from ParentConfig
     */
    @ConfigurationProperties(path = "test.inheritance")
    public static class ChildConfig extends ParentConfig {
        private int childValue = 200;
        private String childString = "child-default";

        // Getters and setters
        public int getChildValue() {
            return childValue;
        }

        public void setChildValue(int childValue) {
            this.childValue = childValue;
        }

        public String getChildString() {
            return childString;
        }

        public void setChildString(String childString) {
            this.childString = childString;
        }
    }

    @Test
    public void testInheritance() {
        // Create property metadata for ChildConfig (which extends ParentConfig)
        List<PropertyMetadata> properties = new ArrayList<>();

        // Child class properties
        PropertyMetadata childValueProperty = new PropertyMetadata();
        childValueProperty.setPath("test.inheritance.childValue");
        childValueProperty.setType("int");
        childValueProperty.setConfigurationClass(ChildConfig.class.getName());
        properties.add(childValueProperty);

        PropertyMetadata childStringProperty = new PropertyMetadata();
        childStringProperty.setPath("test.inheritance.childString");
        childStringProperty.setType("java.lang.String");
        childStringProperty.setConfigurationClass(ChildConfig.class.getName());
        properties.add(childStringProperty);

        // Parent class properties (inherited by ChildConfig)
        PropertyMetadata parentValueProperty = new PropertyMetadata();
        parentValueProperty.setPath("test.inheritance.parentValue");
        parentValueProperty.setType("int");
        parentValueProperty.setConfigurationClass(ChildConfig.class.getName());
        properties.add(parentValueProperty);

        PropertyMetadata parentStringProperty = new PropertyMetadata();
        parentStringProperty.setPath("test.inheritance.parentString");
        parentStringProperty.setType("java.lang.String");
        parentStringProperty.setConfigurationClass(ChildConfig.class.getName());
        properties.add(parentStringProperty);

        // Extract default values
        ConfigurationDefaultValueExtractor.extractDefaultValues(properties);

        // Verify child class properties with exact string representation
        Assertions.assertEquals("200", childValueProperty.getDefaultValue());
        Assertions.assertEquals("child-default", childStringProperty.getDefaultValue());

        // Verify parent class properties (inherited) with exact string representation
        Assertions.assertEquals("100", parentValueProperty.getDefaultValue());
        Assertions.assertEquals("parent-default", parentStringProperty.getDefaultValue());
    }
}
