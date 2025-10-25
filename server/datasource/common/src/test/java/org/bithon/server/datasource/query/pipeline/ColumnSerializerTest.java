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

package org.bithon.server.datasource.query.pipeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test that custom serializers and deserializers work correctly for column classes.
 * Serializers only serialize elements up to the size boundary.
 * Deserializers reconstruct the column from serialized data.
 *
 * @author frank.chen021@outlook.com
 * @date 20/10/25
 */
public class ColumnSerializerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testLongColumnSerializer() throws Exception {
        // Create a column with capacity 10 but only 3 elements
        LongColumn column = new LongColumn("test", 10);
        column.addLong(1L);
        column.addLong(2L);
        column.addLong(3L);

        String json = objectMapper.writeValueAsString(column);
        
        // Verify that only 3 elements are serialized, not 10
        assertTrue(json.contains("\"data\":[1,2,3]"));
        assertFalse(json.contains(",0")); // Should not contain trailing zeros
        assertFalse(json.contains("size")); // Size field should not be in JSON
    }

    @Test
    void testDoubleColumnSerializer() throws Exception {
        // Create a column with capacity 10 but only 3 elements
        DoubleColumn column = new DoubleColumn("test", 10);
        column.addDouble(1.5);
        column.addDouble(2.5);
        column.addDouble(3.5);

        String json = objectMapper.writeValueAsString(column);
        
        // Verify that only 3 elements are serialized, not 10
        assertTrue(json.contains("\"data\":[1.5,2.5,3.5]"));
        assertFalse(json.contains(",0.0")); // Should not contain trailing zeros
        assertFalse(json.contains("size")); // Size field should not be in JSON
    }

    @Test
    void testStringColumnSerializer() throws Exception {
        // Create a column with capacity 10 but only 3 elements
        StringColumn column = new StringColumn("test", 10);
        column.addString("a");
        column.addString("b");
        column.addString("c");

        String json = objectMapper.writeValueAsString(column);
        
        // Verify that only 3 elements are serialized, not 10
        assertTrue(json.contains("\"data\":[\"a\",\"b\",\"c\"]"));
        assertFalse(json.contains("null")); // Should not contain null values for unused capacity
        assertFalse(json.contains("size")); // Size field should not be in JSON
    }

    @Test
    void testLongColumnSerializerWithFullCapacity() throws Exception {
        // Test when size equals capacity
        LongColumn column = new LongColumn("test", new long[]{1L, 2L, 3L});

        String json = objectMapper.writeValueAsString(column);
        
        assertTrue(json.contains("\"data\":[1,2,3]"));
        assertFalse(json.contains("size")); // Size field should not be in JSON
    }

    @Test
    void testDoubleColumnSerializerWithPartialData() throws Exception {
        // Test with array that has more capacity than used
        double[] data = new double[5];
        data[0] = 1.1;
        data[1] = 2.2;
        DoubleColumn column = new DoubleColumn("test", data, 2);

        String json = objectMapper.writeValueAsString(column);
        
        // Should only serialize first 2 elements
        assertTrue(json.contains("\"data\":[1.1,2.2]"));
        assertFalse(json.contains("size")); // Size field should not be in JSON
    }

    @Test
    void testStringColumnSerializerWithPartialData() throws Exception {
        // Test with array that has more capacity than used
        String[] data = new String[5];
        data[0] = "first";
        data[1] = "second";
        StringColumn column = new StringColumn("test", data, 2);

        String json = objectMapper.writeValueAsString(column);
        
        // Should only serialize first 2 elements
        assertTrue(json.contains("\"data\":[\"first\",\"second\"]"));
        assertFalse(json.contains("size")); // Size field should not be in JSON
    }

    @Test
    void testLongColumnDeserializer() throws Exception {
        String json = "{\"name\":\"test\",\"data\":[10,20,30]}";
        
        LongColumn column = objectMapper.readValue(json, LongColumn.class);
        
        assertEquals("test", column.getName());
        assertEquals(3, column.size());
        assertEquals(10L, column.getLong(0));
        assertEquals(20L, column.getLong(1));
        assertEquals(30L, column.getLong(2));
    }

    @Test
    void testDoubleColumnDeserializer() throws Exception {
        String json = "{\"name\":\"test\",\"data\":[1.5,2.5,3.5]}";
        
        DoubleColumn column = objectMapper.readValue(json, DoubleColumn.class);
        
        assertEquals("test", column.getName());
        assertEquals(3, column.size());
        assertEquals(1.5, column.getDouble(0), 0.001);
        assertEquals(2.5, column.getDouble(1), 0.001);
        assertEquals(3.5, column.getDouble(2), 0.001);
    }

    @Test
    void testStringColumnDeserializer() throws Exception {
        String json = "{\"name\":\"test\",\"data\":[\"a\",\"b\",\"c\"]}";
        
        StringColumn column = objectMapper.readValue(json, StringColumn.class);
        
        assertEquals("test", column.getName());
        assertEquals(3, column.size());
        assertEquals("a", column.getString(0));
        assertEquals("b", column.getString(1));
        assertEquals("c", column.getString(2));
    }

    @Test
    void testLongColumnRoundTrip() throws Exception {
        // Create a column with capacity 10 but only 3 elements
        LongColumn original = new LongColumn("testColumn", 10);
        original.addLong(100L);
        original.addLong(200L);
        original.addLong(300L);

        // Serialize
        String json = objectMapper.writeValueAsString(original);
        
        // Deserialize
        LongColumn deserialized = objectMapper.readValue(json, LongColumn.class);
        
        // Verify
        assertEquals(original.getName(), deserialized.getName());
        assertEquals(original.size(), deserialized.size());
        for (int i = 0; i < original.size(); i++) {
            assertEquals(original.getLong(i), deserialized.getLong(i));
        }
    }

    @Test
    void testDoubleColumnRoundTrip() throws Exception {
        // Create a column with capacity 10 but only 4 elements
        DoubleColumn original = new DoubleColumn("testColumn", 10);
        original.addDouble(1.1);
        original.addDouble(2.2);
        original.addDouble(3.3);
        original.addDouble(4.4);

        // Serialize
        String json = objectMapper.writeValueAsString(original);
        
        // Deserialize
        DoubleColumn deserialized = objectMapper.readValue(json, DoubleColumn.class);
        
        // Verify
        assertEquals(original.getName(), deserialized.getName());
        assertEquals(original.size(), deserialized.size());
        for (int i = 0; i < original.size(); i++) {
            assertEquals(original.getDouble(i), deserialized.getDouble(i), 0.001);
        }
    }

    @Test
    void testStringColumnRoundTrip() throws Exception {
        // Create a column with capacity 10 but only 3 elements
        StringColumn original = new StringColumn("testColumn", 10);
        original.addString("hello");
        original.addString("world");
        original.addString("test");

        // Serialize
        String json = objectMapper.writeValueAsString(original);
        
        // Deserialize
        StringColumn deserialized = objectMapper.readValue(json, StringColumn.class);
        
        // Verify
        assertEquals(original.getName(), deserialized.getName());
        assertEquals(original.size(), deserialized.size());
        for (int i = 0; i < original.size(); i++) {
            assertEquals(original.getString(i), deserialized.getString(i));
        }
    }

    @Test
    void testLongColumnRoundTripWithPartialArray() throws Exception {
        // Create a column with partial array usage
        long[] data = new long[10];
        data[0] = 111L;
        data[1] = 222L;
        data[2] = 333L;
        LongColumn original = new LongColumn("partial", data, 3);

        // Serialize
        String json = objectMapper.writeValueAsString(original);
        
        // Verify only 3 elements are in JSON
        assertFalse(json.contains(",0"));
        
        // Deserialize
        LongColumn deserialized = objectMapper.readValue(json, LongColumn.class);
        
        // Verify
        assertEquals(original.getName(), deserialized.getName());
        assertEquals(3, deserialized.size());
        assertEquals(111L, deserialized.getLong(0));
        assertEquals(222L, deserialized.getLong(1));
        assertEquals(333L, deserialized.getLong(2));
    }

    @Test
    void testDoubleColumnRoundTripWithPartialArray() throws Exception {
        // Create a column with partial array usage
        double[] data = new double[10];
        data[0] = 11.1;
        data[1] = 22.2;
        DoubleColumn original = new DoubleColumn("partial", data, 2);

        // Serialize
        String json = objectMapper.writeValueAsString(original);
        
        // Verify only 2 elements are in JSON
        assertFalse(json.contains(",0.0"));
        
        // Deserialize
        DoubleColumn deserialized = objectMapper.readValue(json, DoubleColumn.class);
        
        // Verify
        assertEquals(original.getName(), deserialized.getName());
        assertEquals(2, deserialized.size());
        assertEquals(11.1, deserialized.getDouble(0), 0.001);
        assertEquals(22.2, deserialized.getDouble(1), 0.001);
    }

    @Test
    void testStringColumnRoundTripWithPartialArray() throws Exception {
        // Create a column with partial array usage
        String[] data = new String[10];
        data[0] = "alpha";
        data[1] = "beta";
        StringColumn original = new StringColumn("partial", data, 2);

        // Serialize
        String json = objectMapper.writeValueAsString(original);
        
        // Verify only 2 elements are in JSON
        assertFalse(json.contains("null"));
        
        // Deserialize
        StringColumn deserialized = objectMapper.readValue(json, StringColumn.class);
        
        // Verify
        assertEquals(original.getName(), deserialized.getName());
        assertEquals(2, deserialized.size());
        assertEquals("alpha", deserialized.getString(0));
        assertEquals("beta", deserialized.getString(1));
    }

    @Test
    void testLongColumnViewSerialization() throws Exception {
        // Create a column with 5 elements
        LongColumn column = new LongColumn("test", 5);
        column.addLong(10);
        column.addLong(20);
        column.addLong(30);
        column.addLong(40);
        column.addLong(50);

        // Create a view that selects elements at indices [4, 2, 0]
        // This should give us values [50, 30, 10]
        int[] selections = new int[]{4, 2, 0};
        Column view = column.view(selections, 3);

        // Verify the view returns correct values
        assertEquals(3, view.size());
        assertEquals(50, view.getLong(0));
        assertEquals(30, view.getLong(1));
        assertEquals(10, view.getLong(2));

        // Serialize and deserialize the view
        String json = objectMapper.writeValueAsString(view);
        
        // The deserialized column should contain the view's selected values [50, 30, 10]
        // NOT the first 3 elements of the underlying array [10, 20, 30]
        LongColumn deserialized = objectMapper.readValue(json, LongColumn.class);
        assertEquals(3, deserialized.size());
        assertEquals(50, deserialized.getLong(0));
        assertEquals(30, deserialized.getLong(1));
        assertEquals(10, deserialized.getLong(2));
    }

    @Test
    void testDoubleColumnViewSerialization() throws Exception {
        // Create a column with 5 elements
        DoubleColumn column = new DoubleColumn("test", 5);
        column.addDouble(1.1);
        column.addDouble(2.2);
        column.addDouble(3.3);
        column.addDouble(4.4);
        column.addDouble(5.5);

        // Create a view that selects elements at indices [4, 2, 0]
        // This should give us values [5.5, 3.3, 1.1]
        int[] selections = new int[]{4, 2, 0};
        Column view = column.view(selections, 3);

        // Verify the view returns correct values
        assertEquals(3, view.size());
        assertEquals(5.5, view.getDouble(0), 0.001);
        assertEquals(3.3, view.getDouble(1), 0.001);
        assertEquals(1.1, view.getDouble(2), 0.001);

        // Serialize and deserialize the view
        String json = objectMapper.writeValueAsString(view);
        
        // The deserialized column should contain the view's selected values [5.5, 3.3, 1.1]
        // NOT the first 3 elements of the underlying array [1.1, 2.2, 3.3]
        DoubleColumn deserialized = objectMapper.readValue(json, DoubleColumn.class);
        assertEquals(3, deserialized.size());
        assertEquals(5.5, deserialized.getDouble(0), 0.001);
        assertEquals(3.3, deserialized.getDouble(1), 0.001);
        assertEquals(1.1, deserialized.getDouble(2), 0.001);
    }

    @Test
    void testStringColumnViewSerialization() throws Exception {
        // Create a column with 5 elements
        StringColumn column = new StringColumn("test", 5);
        column.addString("one");
        column.addString("two");
        column.addString("three");
        column.addString("four");
        column.addString("five");

        // Create a view that selects elements at indices [4, 2, 0]
        // This should give us values ["five", "three", "one"]
        int[] selections = new int[]{4, 2, 0};
        Column view = column.view(selections, 3);

        // Verify the view returns correct values
        assertEquals(3, view.size());
        assertEquals("five", view.getString(0));
        assertEquals("three", view.getString(1));
        assertEquals("one", view.getString(2));

        // Serialize and deserialize the view
        String json = objectMapper.writeValueAsString(view);
        
        // The deserialized column should contain the view's selected values ["five", "three", "one"]
        // NOT the first 3 elements of the underlying array ["one", "two", "three"]
        StringColumn deserialized = objectMapper.readValue(json, StringColumn.class);
        assertEquals(3, deserialized.size());
        assertEquals("five", deserialized.getString(0));
        assertEquals("three", deserialized.getString(1));
        assertEquals("one", deserialized.getString(2));
    }

    @Test
    void testLongColumnViewSerializationWithNonSequentialSelection() throws Exception {
        // Create a column with capacity larger than actual size
        LongColumn column = new LongColumn("test", 10);
        column.addLong(100);
        column.addLong(200);
        column.addLong(300);
        column.addLong(400);
        column.addLong(500);

        // Create a view that selects non-sequential elements [3, 1, 4]
        // This should give us values [400, 200, 500]
        int[] selections = new int[]{3, 1, 4};
        Column view = column.view(selections, 3);

        assertEquals(3, view.size());
        assertEquals(400, view.getLong(0));
        assertEquals(200, view.getLong(1));
        assertEquals(500, view.getLong(2));

        // Serialize and deserialize
        String json = objectMapper.writeValueAsString(view);
        
        LongColumn deserialized = objectMapper.readValue(json, LongColumn.class);
        assertEquals(3, deserialized.size());
        assertEquals(400, deserialized.getLong(0));
        assertEquals(200, deserialized.getLong(1));
        assertEquals(500, deserialized.getLong(2));
    }

    @Test
    void testDoubleColumnViewSerializationWithNonSequentialSelection() throws Exception {
        // Create a column with capacity larger than actual size
        DoubleColumn column = new DoubleColumn("test", 10);
        column.addDouble(10.0);
        column.addDouble(20.0);
        column.addDouble(30.0);
        column.addDouble(40.0);
        column.addDouble(50.0);

        // Create a view that selects non-sequential elements [3, 1, 4]
        // This should give us values [40.0, 20.0, 50.0]
        int[] selections = new int[]{3, 1, 4};
        Column view = column.view(selections, 3);

        assertEquals(3, view.size());
        assertEquals(40.0, view.getDouble(0), 0.001);
        assertEquals(20.0, view.getDouble(1), 0.001);
        assertEquals(50.0, view.getDouble(2), 0.001);

        // Serialize and deserialize
        String json = objectMapper.writeValueAsString(view);
        
        DoubleColumn deserialized = objectMapper.readValue(json, DoubleColumn.class);
        assertEquals(3, deserialized.size());
        assertEquals(40.0, deserialized.getDouble(0), 0.001);
        assertEquals(20.0, deserialized.getDouble(1), 0.001);
        assertEquals(50.0, deserialized.getDouble(2), 0.001);
    }

    @Test
    void testStringColumnViewSerializationWithNonSequentialSelection() throws Exception {
        // Create a column with capacity larger than actual size
        StringColumn column = new StringColumn("test", 10);
        column.addString("alpha");
        column.addString("beta");
        column.addString("gamma");
        column.addString("delta");
        column.addString("epsilon");

        // Create a view that selects non-sequential elements [3, 1, 4]
        // This should give us values ["delta", "beta", "epsilon"]
        int[] selections = new int[]{3, 1, 4};
        Column view = column.view(selections, 3);

        assertEquals(3, view.size());
        assertEquals("delta", view.getString(0));
        assertEquals("beta", view.getString(1));
        assertEquals("epsilon", view.getString(2));

        // Serialize and deserialize
        String json = objectMapper.writeValueAsString(view);
        
        StringColumn deserialized = objectMapper.readValue(json, StringColumn.class);
        assertEquals(3, deserialized.size());
        assertEquals("delta", deserialized.getString(0));
        assertEquals("beta", deserialized.getString(1));
        assertEquals("epsilon", deserialized.getString(2));
    }
}

