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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test cases for ClassHistogramTable.ClassInfo.from method
 * 
 * @author Frank Chen
 * @date 24/9/25 9:18 pm
 */
public class ClassHistogramTableTest {

    @Test
    public void testSimpleClassName() {
        ClassHistogramTable.ClassInfo info = ClassHistogramTable.ClassInfo.from("java.lang.String");
        
        assertEquals("java.lang.String", info.className);
        assertNull(info.module);
        assertFalse(info.isArray);
    }

    @Test
    public void testClassNameWithModule() {
        ClassHistogramTable.ClassInfo info = ClassHistogramTable.ClassInfo.from("java.lang.String (java.base@17.0.12)");
        
        assertEquals("java.lang.String", info.className);
        assertEquals("java.base", info.module);
        assertFalse(info.isArray);
    }

    @Test
    public void testClassNameWithModuleNoVersion() {
        ClassHistogramTable.ClassInfo info = ClassHistogramTable.ClassInfo.from("java.lang.String (java.base)");
        
        assertEquals("java.lang.String", info.className);
        assertEquals("java.base", info.module);
        assertFalse(info.isArray);
    }

    @Test
    public void testPrimitiveArrayTypes() {
        // Test all primitive array types
        testArrayType("[B", "byte[]");
        testArrayType("[C", "char[]");
        testArrayType("[D", "double[]");
        testArrayType("[F", "float[]");
        testArrayType("[I", "int[]");
        testArrayType("[J", "long[]");
        testArrayType("[S", "short[]");
        testArrayType("[Z", "boolean[]");
    }

    @Test
    public void testObjectArrayType() {
        ClassHistogramTable.ClassInfo info = ClassHistogramTable.ClassInfo.from("[Ljava.lang.String;");
        
        assertEquals("java.lang.String[]", info.className);
        assertNull(info.module);
        assertTrue(info.isArray);
    }

    @Test
    public void testObjectArrayTypeWithModule() {
        ClassHistogramTable.ClassInfo info = ClassHistogramTable.ClassInfo.from("[Ljava.lang.String; (java.base@17.0.12)");
        
        assertEquals("java.lang.String[]", info.className);
        assertEquals("java.base", info.module);
        assertTrue(info.isArray);
    }

    @Test
    public void testTwoDimensionalArray() {
        ClassHistogramTable.ClassInfo info = ClassHistogramTable.ClassInfo.from("[[I");
        
        assertEquals("int[][]", info.className);
        assertNull(info.module);
        assertTrue(info.isArray);
    }

    @Test
    public void testTwoDimensionalArrayWithModule() {
        ClassHistogramTable.ClassInfo info = ClassHistogramTable.ClassInfo.from("[[I (java.base@17.0.12)");
        
        assertEquals("int[][]", info.className);
        assertEquals("java.base", info.module);
        assertTrue(info.isArray);
    }

    @Test
    public void testThreeDimensionalArray() {
        ClassHistogramTable.ClassInfo info = ClassHistogramTable.ClassInfo.from("[[[I");
        
        assertEquals("int[][][]", info.className);
        assertNull(info.module);
        assertTrue(info.isArray);
    }

    @Test
    public void testFourDimensionalArray() {
        ClassHistogramTable.ClassInfo info = ClassHistogramTable.ClassInfo.from("[[[[I");
        
        assertEquals("int[][][][]", info.className);
        assertNull(info.module);
        assertTrue(info.isArray);
    }

    @Test
    public void testMultiDimensionalObjectArray() {
        ClassHistogramTable.ClassInfo info = ClassHistogramTable.ClassInfo.from("[[Ljava.lang.String;");
        
        assertEquals("java.lang.String[][]", info.className);
        assertNull(info.module);
        assertTrue(info.isArray);
    }

    @Test
    public void testMultiDimensionalObjectArrayWithModule() {
        ClassHistogramTable.ClassInfo info = ClassHistogramTable.ClassInfo.from("[[Ljava.lang.String; (java.base@17.0.12)");
        
        assertEquals("java.lang.String[][]", info.className);
        assertEquals("java.base", info.module);
        assertTrue(info.isArray);
    }

    @Test
    public void testComplexMultiDimensionalArray() {
        ClassHistogramTable.ClassInfo info = ClassHistogramTable.ClassInfo.from("[[[[[B");
        
        assertEquals("byte[][][][][]", info.className);
        assertNull(info.module);
        assertTrue(info.isArray);
    }

    @Test
    public void testMixedArrayTypes() {
        // Test different combinations
        testArrayType("[[D", "double[][]");
        testArrayType("[[[C", "char[][][]");
        testArrayType("[[[[F", "float[][][][]");
        testArrayType("[[[[[J", "long[][][][][]");
    }

    @Test
    public void testEdgeCases() {
        // Empty string
        ClassHistogramTable.ClassInfo info = ClassHistogramTable.ClassInfo.from("");
        assertEquals("", info.className);
        assertNull(info.module);
        assertFalse(info.isArray);

        // Single character
        info = ClassHistogramTable.ClassInfo.from("A");
        assertEquals("A", info.className);
        assertNull(info.module);
        assertFalse(info.isArray);

        // Just opening bracket - this should be treated as an array since it starts with [
        info = ClassHistogramTable.ClassInfo.from("[");
        assertEquals("[", info.className);
        assertNull(info.module);
        assertTrue(info.isArray);
    }

    @Test
    public void testModuleExtractionEdgeCases() {
        // Module with special characters
        ClassHistogramTable.ClassInfo info = ClassHistogramTable.ClassInfo.from("com.example.Class (my-module@1.2.3)");
        assertEquals("com.example.Class", info.className);
        assertEquals("my-module", info.module);
        assertFalse(info.isArray);

        // Module with underscores
        info = ClassHistogramTable.ClassInfo.from("com.example.Class (my_module@1.2.3)");
        assertEquals("com.example.Class", info.className);
        assertEquals("my_module", info.module);
        assertFalse(info.isArray);

        // Module with dots
        info = ClassHistogramTable.ClassInfo.from("com.example.Class (com.example.module@1.2.3)");
        assertEquals("com.example.Class", info.className);
        assertEquals("com.example.module", info.module);
        assertFalse(info.isArray);
    }

    @Test
    public void testComplexRealWorldExamples() {
        // Real-world examples from JVM class histogram
        testArrayType("[B (java.base@17.0.12)", "byte[]", "java.base");
        testArrayType("[[Ljava.lang.Object; (java.base@17.0.12)", "java.lang.Object[][]", "java.base");
        testArrayType("[Ljava.lang.String; (java.base@17.0.12)", "java.lang.String[]", "java.base");
        testArrayType("[[[I (java.base@17.0.12)", "int[][][]", "java.base");
    }

    @Test
    public void testNestedObjectArrays() {
        // Test deeply nested object arrays
        ClassHistogramTable.ClassInfo info = ClassHistogramTable.ClassInfo.from("[[[Ljava.util.HashMap;");
        assertEquals("java.util.HashMap[][][]", info.className);
        assertNull(info.module);
        assertTrue(info.isArray);

        info = ClassHistogramTable.ClassInfo.from("[[[Ljava.util.HashMap; (java.base@17.0.12)");
        assertEquals("java.util.HashMap[][][]", info.className);
        assertEquals("java.base", info.module);
        assertTrue(info.isArray);
    }

    @Test
    public void testArrayWithComplexClassName() {
        // Test arrays with complex class names
        ClassHistogramTable.ClassInfo info = ClassHistogramTable.ClassInfo.from("[Lorg.bithon.server.web.service.agent.sql.table.ClassHistogramTable;");
        assertEquals("org.bithon.server.web.service.agent.sql.table.ClassHistogramTable[]", info.className);
        assertNull(info.module);
        assertTrue(info.isArray);
    }

    private void testArrayType(String input, String expectedClassName) {
        testArrayType(input, expectedClassName, null);
    }

    private void testArrayType(String input, String expectedClassName, String expectedModule) {
        ClassHistogramTable.ClassInfo info = ClassHistogramTable.ClassInfo.from(input);
        assertEquals(expectedClassName, info.className);
        assertEquals(expectedModule, info.module);
        assertTrue(info.isArray);
    }
}
