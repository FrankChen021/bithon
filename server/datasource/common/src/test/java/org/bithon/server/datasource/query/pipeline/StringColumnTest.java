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

import org.junit.jupiter.api.Test;

import java.util.BitSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class StringColumnTest {

    @Test
    void testFilter() {
        StringColumn column = new StringColumn("test", 5);
        column.addString("one");
        column.addString("two");
        column.addString("three");
        column.addString("four");
        column.addString("five");

        BitSet keep = new BitSet(5);
        keep.set(1); // keep "two"
        keep.set(3); // keep "four"

        Column filtered = column.filter(keep);
        assertInstanceOf(StringColumn.class, filtered);
        assertEquals(2, filtered.size());
        assertEquals("two", filtered.getString(0));
        assertEquals("four", filtered.getString(1));
    }

    @Test
    void testView() {
        StringColumn column = new StringColumn("test", 5);
        column.addString("one");
        column.addString("two");
        column.addString("three");
        column.addString("four");
        column.addString("five");

        int[] selections = new int[]{4, 2, 0}; // select "five", "three", "one"
        Column view = column.view(selections, 3);

        assertInstanceOf(StringColumn.class, view);
        assertEquals(3, view.size());
        assertEquals("five", view.getString(0));
        assertEquals("three", view.getString(1));
        assertEquals("one", view.getString(2));
    }

    @Test
    void testViewAndFilter() {
        StringColumn column = new StringColumn("test", 5);
        column.addString("one");
        column.addString("two");
        column.addString("three");
        column.addString("four");
        column.addString("five");

        // Create a view with selections [4, 2, 0] (values: "five", "three", "one")
        int[] selections = new int[]{4, 2, 0};
        Column view = column.view(selections, 3);

        // Filter the view to keep only the first and last elements
        BitSet keep = new BitSet(3);
        keep.set(0); // keep "five"
        keep.set(2); // keep "one"

        Column filtered = view.filter(keep);
        assertInstanceOf(StringColumn.class, filtered);
        assertEquals(2, filtered.size());
        assertEquals("five", filtered.getString(0));
        assertEquals("one", filtered.getString(1));
    }

    @Test
    void testEmptyFilter() {
        StringColumn column = new StringColumn("test", 3);
        column.addString("one");
        column.addString("two");
        column.addString("three");

        BitSet keep = new BitSet(3);
        Column filtered = column.filter(keep);
        assertEquals(0, filtered.size());
    }

    @Test
    void testEmptyView() {
        StringColumn column = new StringColumn("test", 3);
        column.addString("one");
        column.addString("two");
        column.addString("three");

        int[] selections = new int[0];
        Column view = column.view(selections, 0);
        assertEquals(0, view.size());
    }

    @Test
    void testNullValues() {
        StringColumn column = new StringColumn("test", 3);
        column.addObject(null);
        column.addString("two");
        column.addString("three");

        BitSet keep = new BitSet(3);
        keep.set(0); // keep null
        keep.set(2); // keep "three"

        Column filtered = column.filter(keep);
        assertInstanceOf(StringColumn.class, filtered);
        assertEquals(2, filtered.size());
        assertEquals("", filtered.getString(0)); // null should be converted to empty string
        assertEquals("three", filtered.getString(1));
    }
}
