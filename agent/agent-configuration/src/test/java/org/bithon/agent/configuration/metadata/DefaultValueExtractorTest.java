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

package org.bithon.agent.configuration.metadata;

import org.bithon.agent.configuration.processor.MetadataProcessorTest;
import org.bithon.shaded.com.fasterxml.jackson.databind.JsonNode;
import org.bithon.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

/**
 * Tests for ConfigurationDefaultValueExtractor
 *
 * @author frank.chen021@outlook.com
 */
public class DefaultValueExtractorTest {

    @Test
    public void testBasicTypes() {
        String src = "package org.bithon.agent.configuration.test;\n" +
                     "import org.bithon.agent.configuration.annotation.ConfigurationProperties;\n" +
                     "@ConfigurationProperties(path=\"test.basic\")\n" +
                     "public class BasicConfig {\n" +
                     "  private String stringValue = \"default-string\";\n" +
                     "  private int intValue = 42;\n" +
                     "  private long longValue = 1000000L;\n" +
                     "  private boolean booleanValue = true;\n" +
                     "  private double doubleValue = 3.14159;\n" +
                     "  private boolean booleanNotSet;\n" +
                     "  private String stringNotSet;\n" +
                     "  public String getStringValue() { return stringValue; }\n" +
                     "  public int getIntValue() { return intValue; }\n" +
                     "  public long getLongValue() { return longValue; }\n" +
                     "  public boolean isBooleanValue() { return booleanValue; }\n" +
                     "  public double getDoubleValue() { return doubleValue; }\n" +
                     "  public boolean isBooleanNotSet() { return booleanNotSet; }\n" +
                     "  public String getStringNotSet() { return stringNotSet; }\n" +
                     "}";
        MetadataProcessorTest.CompiledConfigResult result = MetadataProcessorTest.compileConfigurationPropertyClass("org.bithon.agent.configuration.test.BasicConfig", src);
        Map<String, PropertyMetadata> props = result.properties;

        // Extract default values
        new DefaultValueExtractor(result.compiledClasses::get).extract(props.values());
        Assertions.assertEquals("default-string", props.get("test.basic.stringValue").getDefaultValue());
        Assertions.assertEquals("42", props.get("test.basic.intValue").getDefaultValue());
        Assertions.assertEquals("1000000", props.get("test.basic.longValue").getDefaultValue());
        Assertions.assertEquals("true", props.get("test.basic.booleanValue").getDefaultValue());
        Assertions.assertEquals("3.14159", props.get("test.basic.doubleValue").getDefaultValue());
    }

    @Test
    public void testUtilityTypes() {
        String src = "package org.bithon.agent.configuration.test;\n" +
                     "import org.bithon.agent.configuration.annotation.ConfigurationProperties;\n" +
                     "import org.bithon.component.commons.utils.HumanReadableNumber;\n" +
                     "import org.bithon.component.commons.utils.HumanReadableDuration;\n" +
                     "import org.bithon.component.commons.utils.HumanReadablePercentage;\n" +
                     "@ConfigurationProperties(path=\"test.utility\")\n" +
                     "public class UtilityTypesConfig {\n" +
                     "  private HumanReadableNumber number = HumanReadableNumber.of(\"10M\");\n" +
                     "  private HumanReadableDuration duration = HumanReadableDuration.parse(\"5m\");\n" +
                     "  private HumanReadablePercentage percentage = HumanReadablePercentage.of(\"75%\");\n" +
                     "  public HumanReadableNumber getNumber() { return number; }\n" +
                     "  public HumanReadableDuration getDuration() { return duration; }\n" +
                     "  public HumanReadablePercentage getPercentage() { return percentage; }\n" +
                     "}";
        MetadataProcessorTest.CompiledConfigResult result = MetadataProcessorTest.compileConfigurationPropertyClass("org.bithon.agent.configuration.test.UtilityTypesConfig", src);
        Map<String, PropertyMetadata> props = result.properties;

        new DefaultValueExtractor(result.compiledClasses::get).extract(props.values());
        Assertions.assertEquals("10M", props.get("test.utility.number").getDefaultValue());
        Assertions.assertEquals("5m", props.get("test.utility.duration").getDefaultValue());
        Assertions.assertEquals("75%", props.get("test.utility.percentage").getDefaultValue());
    }

    @Test
    public void testCollectionTypes() throws IOException {
        String src = "package org.bithon.agent.configuration.test;\n" +
                     "import org.bithon.agent.configuration.annotation.ConfigurationProperties;\n" +
                     "import java.util.*;\n" +
                     "@ConfigurationProperties(path=\"test.collections\")\n" +
                     "public class CollectionsConfig {\n" +
                     "  private List<String> stringList = new ArrayList<>(Arrays.asList(\"item1\", \"item2\", \"item3\"));\n" +
                     "  private Map<String, Integer> integerMap = new HashMap<>();\n" +
                     "  private String[] stringArray = {\"array1\", \"array2\", \"array3\"};\n" +
                     "  public CollectionsConfig() { integerMap.put(\"key1\", 1); integerMap.put(\"key2\", 2); integerMap.put(\"key3\", 3); }\n" +
                     "  public List<String> getStringList() { return stringList; }\n" +
                     "  public Map<String, Integer> getIntegerMap() { return integerMap; }\n" +
                     "  public String[] getStringArray() { return stringArray; }\n" +
                     "}";
        MetadataProcessorTest.CompiledConfigResult result = MetadataProcessorTest.compileConfigurationPropertyClass("org.bithon.agent.configuration.test.CollectionsConfig", src);
        Map<String, PropertyMetadata> props = result.properties;

        // Extract default values
        new DefaultValueExtractor(result.compiledClasses::get).extract(props.values());
        String listValue = props.get("test.collections.stringList").getDefaultValue();
        Assertions.assertNotNull(listValue);
        Assertions.assertEquals("[\"item1\",\"item2\",\"item3\"]", listValue);
        String mapValue = props.get("test.collections.integerMap").getDefaultValue();
        Assertions.assertNotNull(mapValue);

        // Normalize the map value to ensure consistent formatting
        ObjectMapper om = new ObjectMapper();
        JsonNode mapNode = om.readTree(mapValue);
        String normalizedMapValue = om.writeValueAsString(mapNode);
        Assertions.assertEquals("{\"key1\":1,\"key2\":2,\"key3\":3}", normalizedMapValue);
        String arrayValue = props.get("test.collections.stringArray").getDefaultValue();
        Assertions.assertNotNull(arrayValue);
        Assertions.assertEquals("[\"array1\",\"array2\",\"array3\"]", arrayValue);
    }

    @Test
    public void testNestedObjects() {
        String src = "package org.bithon.agent.configuration.test;\n" +
                     "import org.bithon.agent.configuration.annotation.ConfigurationProperties;\n" +
                     "@ConfigurationProperties(path=\"test.nested\")\n" +
                     "public class NestedConfig {\n" +
                     "  private String name = \"nested-config\";\n" +
                     "  private NestedObject nestedObject = new NestedObject();\n" +
                     "  public String getName() { return name; }\n" +
                     "  public NestedObject getNestedObject() { return nestedObject; }\n" +
                     "  public static class NestedObject {\n" +
                     "    private String nestedValue = \"nested-default\";\n" +
                     "    private int nestedInt = 500;\n" +
                     "    public String getNestedValue() { return nestedValue; }\n" +
                     "    public int getNestedInt() { return nestedInt; }\n" +
                     "    public String toString() { return \"NestedObject{nestedValue='\" + nestedValue + \"', nestedInt=\" + nestedInt + \"}\"; }\n" +
                     "  }\n" +
                     "}";
        MetadataProcessorTest.CompiledConfigResult result = MetadataProcessorTest.compileConfigurationPropertyClass("org.bithon.agent.configuration.test.NestedConfig", src);
        Map<String, PropertyMetadata> props = result.properties;

        // Extract default values
        new DefaultValueExtractor(result.compiledClasses::get).extract(props.values());
        Assertions.assertEquals("nested-config", props.get("test.nested.name").getDefaultValue());
        String nestedValue = props.get("test.nested.nestedObject").getDefaultValue();
        Assertions.assertNotNull(nestedValue);
        Assertions.assertEquals("NestedObject{nestedValue='nested-default', nestedInt=500}", nestedValue);
        Assertions.assertTrue(nestedValue.contains("nestedValue='nested-default'"));
        Assertions.assertTrue(nestedValue.contains("nestedInt=500"));
    }

    @Test
    public void testInheritance() {
        String src = "package org.bithon.agent.configuration.test;\n" +
                     "import org.bithon.agent.configuration.annotation.ConfigurationProperties;\n" +
                     "class ParentConfig {\n" +
                     "  private int parentValue = 100;\n" +
                     "  private String parentString = \"parent-default\";\n" +
                     "  public int getParentValue() { return parentValue; }\n" +
                     "  public String getParentString() { return parentString; }\n" +
                     "}\n" +
                     "@ConfigurationProperties(path=\"test.inheritance\")\n" +
                     "public class ChildConfig extends ParentConfig {\n" +
                     "  private int childValue = 200;\n" +
                     "  private String childString = \"child-default\";\n" +
                     "  public int getChildValue() { return childValue; }\n" +
                     "  public String getChildString() { return childString; }\n" +
                     "}";
        MetadataProcessorTest.CompiledConfigResult result = MetadataProcessorTest.compileConfigurationPropertyClass("org.bithon.agent.configuration.test.ChildConfig", src);
        Map<String, PropertyMetadata> props = result.properties;

        // Extract default values
        new DefaultValueExtractor(result.compiledClasses::get).extract(props.values());
        Assertions.assertEquals("200", props.get("test.inheritance.childValue").getDefaultValue());
        Assertions.assertEquals("child-default", props.get("test.inheritance.childString").getDefaultValue());
        Assertions.assertEquals("100", props.get("test.inheritance.parentValue").getDefaultValue());
        Assertions.assertEquals("parent-default", props.get("test.inheritance.parentString").getDefaultValue());
    }
}
