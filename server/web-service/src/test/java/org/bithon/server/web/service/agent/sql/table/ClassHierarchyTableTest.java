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

package org.bithon.server.web.service.agent.sql.table;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test cases for ClassHierarchyTable.parseClassHierarchy method
 *
 * @author Frank Chen
 * @date 29/9/25 2:35 pm
 */
public class ClassHierarchyTableTest {

    @Test
    public void testBasicHierarchy() {
        String input = """
                        java.lang.Object/null
                        |--org.bithon.component.brpc.message.serializer.ProtocolBufferSerializer$DoubleSerializer/0x0000600001cb8170
                        |--java.util.stream.ReduceOps$Box/null
                        |  |--java.util.stream.ReduceOps$13ReducingSink/null
                        |  |--java.util.stream.ReduceOps$1ReducingSink/null
            """;

        List<ClassHierarchyTable.HierarchyEntry> result = ClassHierarchyTable.parseClassHierarchy(input);

        assertEquals(5, result.size());

        // Root class
        assertEquals(1, result.get(0).id);
        assertNull(result.get(0).parentId);
        assertEquals("java.lang.Object", result.get(0).className);
        assertEquals("null", result.get(0).tag);
        assertFalse(result.get(0).isLambda);

        // First child
        assertEquals(2, result.get(1).id);
        assertEquals(Integer.valueOf(1), result.get(1).parentId);
        assertEquals("org.bithon.component.brpc.message.serializer.ProtocolBufferSerializer$DoubleSerializer", result.get(1).className);
        assertEquals("hex:0x0000600001cb8170", result.get(1).tag);
        assertFalse(result.get(1).isLambda);

        // Second child
        assertEquals(3, result.get(2).id);
        assertEquals(Integer.valueOf(1), result.get(2).parentId);
        assertEquals("java.util.stream.ReduceOps$Box", result.get(2).className);
        assertEquals("null", result.get(2).tag);
        assertFalse(result.get(2).isLambda);

        // Third child (nested under second)
        assertEquals(4, result.get(3).id);
        assertEquals(Integer.valueOf(3), result.get(3).parentId);
        assertEquals("java.util.stream.ReduceOps$13ReducingSink", result.get(3).className);
        assertEquals("null", result.get(3).tag);
        assertFalse(result.get(3).isLambda);

        // Fourth child (nested under second)
        assertEquals(5, result.get(4).id);
        assertEquals(Integer.valueOf(3), result.get(4).parentId);
        assertEquals("java.util.stream.ReduceOps$1ReducingSink", result.get(4).className);
        assertEquals("null", result.get(4).tag);
        assertFalse(result.get(4).isLambda);
    }

    @Test
    public void testMultiLevelHierarchy() {
        String input = """
            java.lang.Object/null
            |--java.util.AbstractList
            |--|--java.util.ArrayList
            |--|--java.util.Vector
            |--java.lang.String""";

        List<ClassHierarchyTable.HierarchyEntry> result = ClassHierarchyTable.parseClassHierarchy(input);

        assertEquals(5, result.size());

        // Root
        assertEquals(1, result.get(0).id);
        assertNull(result.get(0).parentId);
        assertEquals("java.lang.Object", result.get(0).className);
        assertEquals("null", result.get(0).tag);
        assertFalse(result.get(0).isLambda);

        // First level child
        assertEquals(2, result.get(1).id);
        assertEquals(Integer.valueOf(1), result.get(1).parentId);
        assertEquals("java.util.AbstractList", result.get(1).className);
        assertFalse(result.get(1).isLambda);

        // Second level children
        assertEquals(3, result.get(2).id);
        assertEquals(Integer.valueOf(2), result.get(2).parentId);
        assertEquals("java.util.ArrayList", result.get(2).className);
        assertFalse(result.get(2).isLambda);

        assertEquals(4, result.get(3).id);
        assertEquals(Integer.valueOf(2), result.get(3).parentId);
        assertEquals("java.util.Vector", result.get(3).className);
        assertFalse(result.get(3).isLambda);

        // Another first level child
        assertEquals(5, result.get(4).id);
        assertEquals(Integer.valueOf(1), result.get(4).parentId);
        assertEquals("java.lang.String", result.get(4).className);
        assertFalse(result.get(4).isLambda);
    }

    @Test
    public void testComplexHierarchyWithHexNumbers() {
        String input = """
            java.lang.Object/null
            |--com.intellij.grazie.detection.LangDetector$$Lambda$123/0x0000007005063878/0x0000600005566d00
            |--git4idea.ui.branch.dashboard.RefInfo/0x0000600001222c60 (intf)
            |--org.jetbrains.completion.full.line.python.PythonFullLineSupporter$$Lambda$456/0x0000007003ac9250/0x0000600009278c80""";

        List<ClassHierarchyTable.HierarchyEntry> result = ClassHierarchyTable.parseClassHierarchy(input);

        assertEquals(4, result.size());

        // Root
        assertEquals(1, result.get(0).id);
        assertNull(result.get(0).parentId);
        assertEquals("java.lang.Object", result.get(0).className);
        assertEquals("null", result.get(0).tag);
        assertFalse(result.get(0).isLambda);

        // First child with TWO hex numbers - this is a lambda class
        assertEquals(2, result.get(1).id);
        assertEquals(Integer.valueOf(1), result.get(1).parentId);
        assertEquals("com.intellij.grazie.detection.LangDetector$$Lambda$123/0x0000007005063878", result.get(1).className);
        assertEquals("hex:0x0000600005566d00, hex:0x0000007005063878", result.get(1).tag);
        assertTrue(result.get(1).isLambda);

        // Second child with hex number and type annotation
        assertEquals(3, result.get(2).id);
        assertEquals(Integer.valueOf(1), result.get(2).parentId);
        assertEquals("git4idea.ui.branch.dashboard.RefInfo", result.get(2).className);
        assertEquals("hex:0x0000600001222c60, type:intf", result.get(2).tag);
        assertFalse(result.get(2).isLambda);

        // Third child with TWO hex numbers - this is a lambda class
        assertEquals(4, result.get(3).id);
        assertEquals(Integer.valueOf(1), result.get(3).parentId);
        assertEquals("org.jetbrains.completion.full.line.python.PythonFullLineSupporter$$Lambda$456/0x0000007003ac9250", result.get(3).className);
        assertEquals("hex:0x0000600009278c80, hex:0x0000007003ac9250", result.get(3).tag);
        assertTrue(result.get(3).isLambda);
    }

    @Test
    public void testDeepHierarchy() {
        String input = """
            java.lang.Object/null
            |--java.util.AbstractCollection
            |--|--java.util.AbstractList
            |--|--|--java.util.ArrayList
            |--|--|--|--java.util.Vector
            |--|--java.util.AbstractSet
            |--|--|--java.util.HashSet
            |--java.lang.String""";

        List<ClassHierarchyTable.HierarchyEntry> result = ClassHierarchyTable.parseClassHierarchy(input);

        assertEquals(8, result.size());

        // Verify the hierarchy structure
        assertEquals(1, result.get(0).id); // Object
        assertEquals(2, result.get(1).id); // AbstractCollection
        assertEquals(3, result.get(2).id); // AbstractList
        assertEquals(4, result.get(3).id); // ArrayList
        assertEquals(5, result.get(4).id); // Vector
        assertEquals(6, result.get(5).id); // AbstractSet
        assertEquals(7, result.get(6).id); // HashSet
        assertEquals(8, result.get(7).id); // String

        // Verify parent relationships
        assertNull(result.get(0).parentId); // Object has no parent
        assertEquals(Integer.valueOf(1), result.get(1).parentId); // AbstractCollection -> Object
        assertEquals(Integer.valueOf(2), result.get(2).parentId); // AbstractList -> AbstractCollection
        assertEquals(Integer.valueOf(3), result.get(3).parentId); // ArrayList -> AbstractList
        assertEquals(Integer.valueOf(4), result.get(4).parentId); // Vector -> ArrayList
        assertEquals(Integer.valueOf(2), result.get(5).parentId); // AbstractSet -> AbstractCollection
        assertEquals(Integer.valueOf(6), result.get(6).parentId); // HashSet -> AbstractSet
        assertEquals(Integer.valueOf(1), result.get(7).parentId); // String -> Object
    }

    @Test
    public void testEmptyInput() {
        List<ClassHierarchyTable.HierarchyEntry> result = ClassHierarchyTable.parseClassHierarchy("");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testNullInput() {
        List<ClassHierarchyTable.HierarchyEntry> result = ClassHierarchyTable.parseClassHierarchy(null);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testOnlyPidLine() {
        String input = "";
        List<ClassHierarchyTable.HierarchyEntry> result = ClassHierarchyTable.parseClassHierarchy(input);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSingleRootClass() {
        String input = "java.lang.Object/null";

        List<ClassHierarchyTable.HierarchyEntry> result = ClassHierarchyTable.parseClassHierarchy(input);

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).id);
        assertNull(result.get(0).parentId);
        assertEquals("java.lang.Object", result.get(0).className);
        assertEquals("null", result.get(0).tag);
        assertFalse(result.get(0).isLambda);
    }

    @Test
    public void testRootClassWithoutNullSuffix() {
        String input = """
            java.lang.Object
            |--java.lang.String""";

        List<ClassHierarchyTable.HierarchyEntry> result = ClassHierarchyTable.parseClassHierarchy(input);

        assertEquals(2, result.size());
        assertEquals(1, result.get(0).id);
        assertNull(result.get(0).parentId);
        assertEquals("java.lang.Object", result.get(0).className);
        assertNull(result.get(0).tag);
        assertFalse(result.get(0).isLambda);
    }

    @Test
    public void testTypeAnnotations() {
        String input = """
            java.lang.Object/null
            |--java.util.List (interface)
            |--java.util.ArrayList (class)
            |--java.lang.String (final)""";

        List<ClassHierarchyTable.HierarchyEntry> result = ClassHierarchyTable.parseClassHierarchy(input);

        assertEquals(4, result.size());
        assertEquals("type:interface", result.get(1).tag);
        assertEquals("type:class", result.get(2).tag);
        assertEquals("type:final", result.get(3).tag);
    }

    @Test
    public void testHexNumbersOnly() {
        String input = """
            java.lang.Object/null
            |--java.lang.String/0x0000007005063878
            |--java.util.List/0x0000600001222c60""";

        List<ClassHierarchyTable.HierarchyEntry> result = ClassHierarchyTable.parseClassHierarchy(input);

        assertEquals(3, result.size());
        assertEquals("hex:0x0000007005063878", result.get(1).tag);
        assertEquals("hex:0x0000600001222c60", result.get(2).tag);
    }

    @Test
    public void testMixedHexAndTypeAnnotations() {
        String input = """
            java.lang.Object/null
            |--java.util.List/0x0000007005063878 (interface)
            |--java.util.ArrayList/0x0000600001222c60 (class)
            |--java.lang.String/0x0000007003ac9250 (final)""";

        List<ClassHierarchyTable.HierarchyEntry> result = ClassHierarchyTable.parseClassHierarchy(input);

        assertEquals(4, result.size());
        assertEquals("hex:0x0000007005063878, type:interface", result.get(1).tag);
        assertEquals("hex:0x0000600001222c60, type:class", result.get(2).tag);
        assertEquals("hex:0x0000007003ac9250, type:final", result.get(3).tag);
    }

    @Test
    public void testComplexClassNameWithSpecialCharacters() {
        String input = """
            java.lang.Object/null
            |--com.example.MyClass$$Lambda$123/0x0000007005063878/0x0000600005566d00
            |--com.example.AnotherClass$InnerClass/0x0000600001222c60 (intf)""";

        List<ClassHierarchyTable.HierarchyEntry> result = ClassHierarchyTable.parseClassHierarchy(input);

        assertEquals(3, result.size());
        assertEquals("com.example.MyClass$$Lambda$123/0x0000007005063878", result.get(1).className);
        assertEquals("hex:0x0000600005566d00, hex:0x0000007005063878", result.get(1).tag);
        assertTrue(result.get(1).isLambda);
        assertEquals("com.example.AnotherClass$InnerClass", result.get(2).className);
        assertEquals("hex:0x0000600001222c60, type:intf", result.get(2).tag);
        assertFalse(result.get(2).isLambda);
    }

    @Test
    public void testEmptyLinesAndWhitespace() {
        String input = """
            
            java.lang.Object/null
             \s
            |--java.lang.String
            \t
            |--java.util.List""";

        List<ClassHierarchyTable.HierarchyEntry> result = ClassHierarchyTable.parseClassHierarchy(input);

        assertEquals(3, result.size());
        assertEquals("java.lang.Object", result.get(0).className);
        assertEquals("java.lang.String", result.get(1).className);
        assertEquals("java.util.List", result.get(2).className);
    }

    @Test
    public void testMalformedLines() {
        String input = """
            java.lang.Object/null
            |--java.lang.String
            invalid-line-without-prefix
            |--java.util.List
            another-invalid-line""";

        List<ClassHierarchyTable.HierarchyEntry> result = ClassHierarchyTable.parseClassHierarchy(input);

        // Should only parse valid lines
        assertEquals(3, result.size());
        assertEquals("java.lang.Object", result.get(0).className);
        assertEquals("java.lang.String", result.get(1).className);
        assertEquals("java.util.List", result.get(2).className);
    }

    @Test
    public void testDifferentHexFormats() {
        String input = """
            java.lang.Object/null
            |--java.lang.String/0x1234567890abcdef
            |--java.util.List/0XABCDEF1234567890
            |--java.util.ArrayList/1234567890abcdef""";

        List<ClassHierarchyTable.HierarchyEntry> result = ClassHierarchyTable.parseClassHierarchy(input);

        assertEquals(4, result.size());
        assertEquals("hex:0x1234567890abcdef", result.get(1).tag);
        assertEquals("hex:0XABCDEF1234567890", result.get(2).tag);
        assertEquals("hex:1234567890abcdef", result.get(3).tag);
    }

    @Test
    public void testComplexTypeAnnotations() {
        String input = """
            java.lang.Object/null
            |--java.util.List (interface)
            |--java.util.ArrayList (class, serializable)
            |--java.lang.String (final, immutable)""";

        List<ClassHierarchyTable.HierarchyEntry> result = ClassHierarchyTable.parseClassHierarchy(input);

        assertEquals(4, result.size());
        assertEquals("type:interface", result.get(1).tag);
        assertEquals("type:class, serializable", result.get(2).tag);
        assertEquals("type:final, immutable", result.get(3).tag);
    }

    @Test
    public void testRealWorldExample() {
        // Test with a more realistic class hierarchy
        String input = """
            java.lang.Object/null
            |--java.util.AbstractCollection
            |--|--java.util.AbstractList
            |--|--|--java.util.ArrayList/0x0000007005063878/0x0000600005566d00
            |--|--|--java.util.Vector/0x0000600001222c60 (synchronized)
            |--|--java.util.AbstractSet
            |--|--|--java.util.HashSet/0x0000007003ac9250/0x0000600009278c80
            |--|--|--java.util.TreeSet/0x0000600001222c60 (sorted)
            |--java.lang.String/0x0000007005063878/0x0000600005566d00 (final, immutable)""";

        List<ClassHierarchyTable.HierarchyEntry> result = ClassHierarchyTable.parseClassHierarchy(input);

        assertEquals(9, result.size());

        // Verify some key relationships
        assertEquals(Integer.valueOf(1), result.get(1).parentId); // AbstractCollection -> Object
        assertEquals(Integer.valueOf(2), result.get(2).parentId); // AbstractList -> AbstractCollection
        assertEquals(Integer.valueOf(3), result.get(3).parentId); // ArrayList -> AbstractList
        assertEquals(Integer.valueOf(3), result.get(4).parentId); // Vector -> AbstractList
        assertEquals(Integer.valueOf(2), result.get(5).parentId); // AbstractSet -> AbstractCollection
        assertEquals(Integer.valueOf(6), result.get(6).parentId); // HashSet -> AbstractSet
        assertEquals(Integer.valueOf(6), result.get(7).parentId); // TreeSet -> AbstractSet
        assertEquals(Integer.valueOf(1), result.get(8).parentId); // String -> Object

        // Verify tags
        assertEquals("hex:0x0000600005566d00, hex:0x0000007005063878", result.get(3).tag);
        assertEquals("hex:0x0000600001222c60, type:synchronized", result.get(4).tag);
        assertEquals("hex:0x0000600009278c80, hex:0x0000007003ac9250", result.get(6).tag);
        assertEquals("hex:0x0000600001222c60, type:sorted", result.get(7).tag);
        assertEquals("hex:0x0000600005566d00, hex:0x0000007005063878, type:final, immutable", result.get(8).tag);
    }

    @Test
    public void testNewHierarchyFormat() {
        // Test with the new format using |  (pipe + two spaces) for deeper levels
        String input = """
            java.lang.Object/null
            |--com.intellij.configurationStore.ComponentStoreImpl/0x00006000012a95e0
            |  |--com.intellij.configurationStore.ModuleStoreImpl/0x00006000012a95e0
            |  |--com.intellij.configurationStore.ComponentStoreWithExtraComponents/0x00006000012a95e0
            |  |  |--com.intellij.configurationStore.DefaultProjectStoreImpl/0x00006000012a95e0
            |  |  |--com.intellij.configurationStore.ProjectStoreImpl/0x00006000012a95e0
            |  |  |  |--com.intellij.configurationStore.ProjectWithModuleStoreImpl/0x00006000012a95e0
            |  |  |  |  |--com.intellij.configurationScript.providers.MyProjectStore/0x000060000521dd60
            |  |  |--com.intellij.configurationStore.ApplicationStoreImpl/0x00006000012a95e0
            |--icons.JavaScriptLanguageIcons$BuildTools$Grunt/0x000060000159be80""";

        List<ClassHierarchyTable.HierarchyEntry> result = ClassHierarchyTable.parseClassHierarchy(input);

        assertEquals(10, result.size());

        // Verify the hierarchy structure
        assertEquals(1, result.get(0).id); // Object
        assertEquals(2, result.get(1).id); // ComponentStoreImpl
        assertEquals(3, result.get(2).id); // ModuleStoreImpl
        assertEquals(4, result.get(3).id); // ComponentStoreWithExtraComponents
        assertEquals(5, result.get(4).id); // DefaultProjectStoreImpl
        assertEquals(6, result.get(5).id); // ProjectStoreImpl
        assertEquals(7, result.get(6).id); // ProjectWithModuleStoreImpl
        assertEquals(8, result.get(7).id); // MyProjectStore
        assertEquals(9, result.get(8).id); // ApplicationStoreImpl
        assertEquals(10, result.get(9).id); // JavaScriptLanguageIcons$BuildTools$Grunt

        // Verify parent relationships
        assertNull(result.get(0).parentId); // Object has no parent
        assertEquals(Integer.valueOf(1), result.get(1).parentId); // ComponentStoreImpl -> Object
        assertEquals(Integer.valueOf(2), result.get(2).parentId); // ModuleStoreImpl -> ComponentStoreImpl
        assertEquals(Integer.valueOf(2), result.get(3).parentId); // ComponentStoreWithExtraComponents -> ComponentStoreImpl
        assertEquals(Integer.valueOf(4), result.get(4).parentId); // DefaultProjectStoreImpl -> ComponentStoreWithExtraComponents
        assertEquals(Integer.valueOf(4), result.get(5).parentId); // ProjectStoreImpl -> ComponentStoreWithExtraComponents
        assertEquals(Integer.valueOf(6), result.get(6).parentId); // ProjectWithModuleStoreImpl -> ProjectStoreImpl
        assertEquals(Integer.valueOf(7), result.get(7).parentId); // MyProjectStore -> ProjectWithModuleStoreImpl
        assertEquals(Integer.valueOf(4), result.get(8).parentId); // ApplicationStoreImpl -> ComponentStoreWithExtraComponents
        assertEquals(Integer.valueOf(1), result.get(9).parentId); // JavaScriptLanguageIcons$BuildTools$Grunt -> Object

        // Verify class names and tags
        assertEquals("java.lang.Object", result.get(0).className);
        assertEquals("null", result.get(0).tag);

        assertEquals("com.intellij.configurationStore.ComponentStoreImpl", result.get(1).className);
        assertEquals("hex:0x00006000012a95e0", result.get(1).tag);

        assertEquals("com.intellij.configurationStore.ModuleStoreImpl", result.get(2).className);
        assertEquals("hex:0x00006000012a95e0", result.get(2).tag);

        assertEquals("com.intellij.configurationStore.ComponentStoreWithExtraComponents", result.get(3).className);
        assertEquals("hex:0x00006000012a95e0", result.get(3).tag);

        assertEquals("com.intellij.configurationStore.DefaultProjectStoreImpl", result.get(4).className);
        assertEquals("hex:0x00006000012a95e0", result.get(4).tag);

        assertEquals("com.intellij.configurationStore.ProjectStoreImpl", result.get(5).className);
        assertEquals("hex:0x00006000012a95e0", result.get(5).tag);

        assertEquals("com.intellij.configurationStore.ProjectWithModuleStoreImpl", result.get(6).className);
        assertEquals("hex:0x00006000012a95e0", result.get(6).tag);

        assertEquals("com.intellij.configurationScript.providers.MyProjectStore", result.get(7).className);
        assertEquals("hex:0x000060000521dd60", result.get(7).tag);

        assertEquals("com.intellij.configurationStore.ApplicationStoreImpl", result.get(8).className);
        assertEquals("hex:0x00006000012a95e0", result.get(8).tag);

        assertEquals("icons.JavaScriptLanguageIcons$BuildTools$Grunt", result.get(9).className);
        assertEquals("hex:0x000060000159be80", result.get(9).tag);
    }

    @Test
    public void testMixedHierarchyFormats() {
        // Test with both old and new formats mixed
        String input = """
            java.lang.Object/null
            |--java.util.AbstractCollection
            |  |--java.util.AbstractList
            |  |  |--java.util.ArrayList
            |  |--java.util.AbstractSet
            |--java.lang.String""";

        List<ClassHierarchyTable.HierarchyEntry> result = ClassHierarchyTable.parseClassHierarchy(input);

        assertEquals(6, result.size());

        // Verify the hierarchy structure
        assertEquals(1, result.get(0).id); // Object
        assertEquals(2, result.get(1).id); // AbstractCollection
        assertEquals(3, result.get(2).id); // AbstractList
        assertEquals(4, result.get(3).id); // ArrayList
        assertEquals(5, result.get(4).id); // AbstractSet
        assertEquals(6, result.get(5).id); // String

        // Verify parent relationships
        assertNull(result.get(0).parentId); // Object has no parent
        assertEquals(Integer.valueOf(1), result.get(1).parentId); // AbstractCollection -> Object
        assertEquals(Integer.valueOf(2), result.get(2).parentId); // AbstractList -> AbstractCollection
        assertEquals(Integer.valueOf(3), result.get(3).parentId); // ArrayList -> AbstractList
        assertEquals(Integer.valueOf(2), result.get(4).parentId); // AbstractSet -> AbstractCollection
        assertEquals(Integer.valueOf(1), result.get(5).parentId); // String -> Object
    }

    @Test
    public void testComplexNewFormatHierarchy() {
        // Test with complex hierarchy using the new format
        String input = """
            java.lang.Object/null
            |--com.example.BaseClass/0x0000007005063878
            |  |--com.example.ChildClass/0x0000600005566d00 (interface)
            |  |  |--com.example.GrandChildClass/0x0000600001222c60 (final)
            |  |  |  |--com.example.GreatGrandChildClass/0x0000600009278c80
            |  |  |--com.example.AnotherGrandChildClass/0x0000600001222c60 (abstract)
            |  |--com.example.AnotherChildClass/0x0000600001222c60 (synchronized)
            |--com.example.OtherBaseClass/0x0000007003ac9250""";

        List<ClassHierarchyTable.HierarchyEntry> result = ClassHierarchyTable.parseClassHierarchy(input);

        assertEquals(8, result.size());

        // Verify the hierarchy structure
        assertEquals(1, result.get(0).id); // Object
        assertEquals(2, result.get(1).id); // BaseClass
        assertEquals(3, result.get(2).id); // ChildClass
        assertEquals(4, result.get(3).id); // GrandChildClass
        assertEquals(5, result.get(4).id); // GreatGrandChildClass
        assertEquals(6, result.get(5).id); // AnotherGrandChildClass
        assertEquals(7, result.get(6).id); // AnotherChildClass
        assertEquals(8, result.get(7).id); // OtherBaseClass

        // Verify parent relationships
        assertNull(result.get(0).parentId); // Object has no parent
        assertEquals(Integer.valueOf(1), result.get(1).parentId); // BaseClass -> Object
        assertEquals(Integer.valueOf(2), result.get(2).parentId); // ChildClass -> BaseClass
        assertEquals(Integer.valueOf(3), result.get(3).parentId); // GrandChildClass -> ChildClass
        assertEquals(Integer.valueOf(4), result.get(4).parentId); // GreatGrandChildClass -> GrandChildClass
        assertEquals(Integer.valueOf(3), result.get(5).parentId); // AnotherGrandChildClass -> ChildClass
        assertEquals(Integer.valueOf(2), result.get(6).parentId); // AnotherChildClass -> BaseClass
        assertEquals(Integer.valueOf(1), result.get(7).parentId); // OtherBaseClass -> Object

        // Verify tags
        assertEquals("hex:0x0000007005063878", result.get(1).tag);
        assertEquals("hex:0x0000600005566d00, type:interface", result.get(2).tag);
        assertEquals("hex:0x0000600001222c60, type:final", result.get(3).tag);
        assertEquals("hex:0x0000600009278c80", result.get(4).tag);
        assertEquals("hex:0x0000600001222c60, type:abstract", result.get(5).tag);
        assertEquals("hex:0x0000600001222c60, type:synchronized", result.get(6).tag);
        assertEquals("hex:0x0000007003ac9250", result.get(7).tag);
    }

    @Test
    public void testLambdaClassDetection() {
        // Test lambda class detection
        String input = """
            java.lang.Object/null
            |--com.example.MyClass$$Lambda$123/0x0000007005063878
            |--com.example.AnotherClass$$Lambda$456/null
            |--com.example.RegularClass/0x0000600001222c60
            |--com.example.InnerClass$Lambda$789/0x0000600005566d00""";

        List<ClassHierarchyTable.HierarchyEntry> result = ClassHierarchyTable.parseClassHierarchy(input);

        assertEquals(5, result.size());

        // Root - not a lambda
        assertFalse(result.get(0).isLambda);
        assertEquals("java.lang.Object", result.get(0).className);

        // First lambda class
        assertTrue(result.get(1).isLambda);
        assertEquals("com.example.MyClass$$Lambda$123/0x0000007005063878", result.get(1).className);
        assertEquals("hex:0x0000007005063878", result.get(1).tag);

        // Second lambda class with null
        assertTrue(result.get(2).isLambda);
        assertEquals("com.example.AnotherClass$$Lambda$456", result.get(2).className);
        assertEquals("null", result.get(2).tag);

        // Regular class - not a lambda
        assertFalse(result.get(3).isLambda);
        assertEquals("com.example.RegularClass", result.get(3).className);
        assertEquals("hex:0x0000600001222c60", result.get(3).tag);

        // Inner lambda class
        assertTrue(result.get(4).isLambda);
        assertEquals("com.example.InnerClass$Lambda$789/0x0000600005566d00", result.get(4).className);
        assertEquals("hex:0x0000600005566d00", result.get(4).tag);
    }

    @Test
    public void testClassNameWithTwoHexNumbers() {
        // Test className/hex1/hex2 format
        String input = """
            java.lang.Object/null
            |--com.example.MyClass$$Lambda$123/0x0000007005063878/0x0000600005566d00
            |--com.example.AnotherClass/0x0000600001222c60/0x0000600009278c80 (interface)""";

        List<ClassHierarchyTable.HierarchyEntry> result = ClassHierarchyTable.parseClassHierarchy(input);

        assertEquals(3, result.size());

        // Root
        assertEquals("java.lang.Object", result.get(0).className);
        assertEquals("null", result.get(0).tag);

        // Lambda class with two hex numbers
        assertEquals("com.example.MyClass$$Lambda$123/0x0000007005063878", result.get(1).className);
        assertEquals("hex:0x0000600005566d00, hex:0x0000007005063878", result.get(1).tag);
        assertTrue(result.get(1).isLambda);

        // Regular class with two hex numbers and type annotation
        assertEquals("com.example.AnotherClass", result.get(2).className);
        assertEquals("hex:0x0000600009278c80, hex:0x0000600001222c60, type:interface", result.get(2).tag);
        assertFalse(result.get(2).isLambda);
    }

    @Test
    public void testClassNameWithOneHexNumber() {
        // Test className/hex format
        String input = """
            java.lang.Object/null
            |--com.example.MyClass/0x0000007005063878
            |--com.example.AnotherClass/0x0000600001222c60 (final)""";

        List<ClassHierarchyTable.HierarchyEntry> result = ClassHierarchyTable.parseClassHierarchy(input);

        assertEquals(3, result.size());

        // Root
        assertEquals("java.lang.Object", result.get(0).className);
        assertEquals("null", result.get(0).tag);

        // Class with one hex number
        assertEquals("com.example.MyClass", result.get(1).className);
        assertEquals("hex:0x0000007005063878", result.get(1).tag);
        assertFalse(result.get(1).isLambda);

        // Class with one hex number and type annotation
        assertEquals("com.example.AnotherClass", result.get(2).className);
        assertEquals("hex:0x0000600001222c60, type:final", result.get(2).tag);
        assertFalse(result.get(2).isLambda);
    }

    @Test
    public void testClassNameWithNull() {
        // Test className/null format
        String input = """
            java.lang.Object/null
            |--com.example.MyClass/null
            |--com.example.AnotherClass/null (interface)
            |--com.example.ThirdClass""";

        List<ClassHierarchyTable.HierarchyEntry> result = ClassHierarchyTable.parseClassHierarchy(input);

        assertEquals(4, result.size());

        // Root
        assertEquals("java.lang.Object", result.get(0).className);
        assertEquals("null", result.get(0).tag);

        // Class with null
        assertEquals("com.example.MyClass", result.get(1).className);
        assertEquals("null", result.get(1).tag);
        assertFalse(result.get(1).isLambda);

        // Class with null and type annotation
        assertEquals("com.example.AnotherClass", result.get(2).className);
        assertEquals("null, type:interface", result.get(2).tag);
        assertFalse(result.get(2).isLambda);

        // Class without any suffix
        assertEquals("com.example.ThirdClass", result.get(3).className);
        assertNull(result.get(3).tag);
        assertFalse(result.get(3).isLambda);
    }

    @Test
    public void testMixedFormatsComprehensive() {
        // Test all formats mixed together
        String input = """
            java.lang.Object/null
            |--com.example.Lambda1$$Lambda$100/0x1111111111111111/0x2222222222222222
            |--com.example.Lambda2$$Lambda$200/0x3333333333333333
            |--com.example.Lambda3$$Lambda$300/null
            |--com.example.RegularClass1/0x4444444444444444/0x5555555555555555 (interface)
            |--com.example.RegularClass2/0x6666666666666666 (final)
            |--com.example.RegularClass3/null (abstract)
            |--com.example.RegularClass4""";

        List<ClassHierarchyTable.HierarchyEntry> result = ClassHierarchyTable.parseClassHierarchy(input);

        assertEquals(8, result.size());

        // Root
        assertEquals("java.lang.Object", result.get(0).className);
        assertEquals("null", result.get(0).tag);
        assertFalse(result.get(0).isLambda);

        // Lambda with two hex numbers
        assertEquals("com.example.Lambda1$$Lambda$100/0x1111111111111111", result.get(1).className);
        assertEquals("hex:0x2222222222222222, hex:0x1111111111111111", result.get(1).tag);
        assertTrue(result.get(1).isLambda);

        // Lambda with one hex number
        assertEquals("com.example.Lambda2$$Lambda$200/0x3333333333333333", result.get(2).className);
        assertEquals("hex:0x3333333333333333", result.get(2).tag);
        assertTrue(result.get(2).isLambda);

        // Lambda with null
        assertEquals("com.example.Lambda3$$Lambda$300", result.get(3).className);
        assertEquals("null", result.get(3).tag);
        assertTrue(result.get(3).isLambda);

        // Regular class with two hex numbers and type
        assertEquals("com.example.RegularClass1", result.get(4).className);
        assertEquals("hex:0x5555555555555555, hex:0x4444444444444444, type:interface", result.get(4).tag);
        assertFalse(result.get(4).isLambda);

        // Regular class with one hex number and type
        assertEquals("com.example.RegularClass2", result.get(5).className);
        assertEquals("hex:0x6666666666666666, type:final", result.get(5).tag);
        assertFalse(result.get(5).isLambda);

        // Regular class with null and type
        assertEquals("com.example.RegularClass3", result.get(6).className);
        assertEquals("null, type:abstract", result.get(6).tag);
        assertFalse(result.get(6).isLambda);

        // Regular class with no suffix
        assertEquals("com.example.RegularClass4", result.get(7).className);
        assertNull(result.get(7).tag);
        assertFalse(result.get(7).isLambda);
    }

    @Test
    public void testAnonymousInnerClass() {
        // Test with anonymous inner class like Class$3
        String input = """
                        java.lang.Object/null
                        |--java.lang.Class$3/null
            """;
        List<ClassHierarchyTable.HierarchyEntry> result = ClassHierarchyTable.parseClassHierarchy(input);

        assertEquals(2, result.size());
        
        // Root
        assertEquals("java.lang.Object", result.get(0).className);
        assertEquals("null", result.get(0).tag);
        assertFalse(result.get(0).isLambda);
        
        // Anonymous inner class - not a lambda
        assertEquals("java.lang.Class$3", result.get(1).className);
        assertEquals("null", result.get(1).tag);
        assertFalse(result.get(1).isLambda);
    }
}
