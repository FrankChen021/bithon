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

import org.bithon.server.datasource.query.result.Column;
import org.bithon.server.datasource.query.result.DoubleColumn;
import org.junit.jupiter.api.Test;

import java.util.BitSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class DoubleColumnTest {

    @Test
    void testFilter() {
        DoubleColumn column = new DoubleColumn("test", 5);
        column.addDouble(1.1);
        column.addDouble(2.2);
        column.addDouble(3.3);
        column.addDouble(4.4);
        column.addDouble(5.5);

        BitSet keep = new BitSet(5);
        keep.set(1); // keep 2.2
        keep.set(3); // keep 4.4

        Column filtered = column.filter(keep);
        assertInstanceOf(DoubleColumn.class, filtered);
        assertEquals(2, filtered.size());
        assertEquals(2.2, filtered.getDouble(0), 0.001);
        assertEquals(4.4, filtered.getDouble(1), 0.001);
    }

    @Test
    void testView() {
        DoubleColumn column = new DoubleColumn("test", 5);
        column.addDouble(1.1);
        column.addDouble(2.2);
        column.addDouble(3.3);
        column.addDouble(4.4);
        column.addDouble(5.5);

        int[] selections = new int[]{4, 2, 0}; // select 5.5, 3.3, 1.1
        Column view = column.view(selections, 3);

        assertInstanceOf(DoubleColumn.class, view);
        assertEquals(3, view.size());
        assertEquals(5.5, view.getDouble(0), 0.001);
        assertEquals(3.3, view.getDouble(1), 0.001);
        assertEquals(1.1, view.getDouble(2), 0.001);
    }

    @Test
    void testViewAndFilter() {
        DoubleColumn column = new DoubleColumn("test", 5);
        column.addDouble(1.1);
        column.addDouble(2.2);
        column.addDouble(3.3);
        column.addDouble(4.4);
        column.addDouble(5.5);

        // Create a view with selections [4, 2, 0] (values: 5.5, 3.3, 1.1)
        int[] selections = new int[]{4, 2, 0};
        Column view = column.view(selections, 3);

        // Filter the view to keep only the first and last elements
        BitSet keep = new BitSet(3);
        keep.set(0); // keep 5.5
        keep.set(2); // keep 1.1

        Column filtered = view.filter(keep);
        assertInstanceOf(DoubleColumn.class, filtered);
        assertEquals(2, filtered.size());
        assertEquals(5.5, filtered.getDouble(0), 0.001);
        assertEquals(1.1, filtered.getDouble(1), 0.001);
    }

    @Test
    void testEmptyFilter() {
        DoubleColumn column = new DoubleColumn("test", 3);
        column.addDouble(1.1);
        column.addDouble(2.2);
        column.addDouble(3.3);

        BitSet keep = new BitSet(3);
        Column filtered = column.filter(keep);
        assertEquals(0, filtered.size());
    }

    @Test
    void testEmptyView() {
        DoubleColumn column = new DoubleColumn("test", 3);
        column.addDouble(1.1);
        column.addDouble(2.2);
        column.addDouble(3.3);

        int[] selections = new int[0];
        Column view = column.view(selections, 0);
        assertEquals(0, view.size());
    }
}
