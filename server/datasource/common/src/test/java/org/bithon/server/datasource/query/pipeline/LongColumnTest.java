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

public class LongColumnTest {

    @Test
    void testFilter() {
        LongColumn column = new LongColumn("test", 5);
        column.addLong(1);
        column.addLong(2);
        column.addLong(3);
        column.addLong(4);
        column.addLong(5);

        BitSet keep = new BitSet(5);
        keep.set(1); // keep 2
        keep.set(3); // keep 4

        Column filtered = column.filter(keep);
        assertInstanceOf(LongColumn.class, filtered);
        assertEquals(2, filtered.size());
        assertEquals(2, filtered.getLong(0));
        assertEquals(4, filtered.getLong(1));
    }

    @Test
    void testView() {
        LongColumn column = new LongColumn("test", 5);
        column.addLong(1);
        column.addLong(2);
        column.addLong(3);
        column.addLong(4);
        column.addLong(5);

        int[] selections = new int[]{4, 2, 0}; // select 5, 3, 1
        Column view = column.view(selections, 3);

        assertInstanceOf(LongColumn.class, view);
        assertEquals(3, view.size());
        assertEquals(5, view.getLong(0));
        assertEquals(3, view.getLong(1));
        assertEquals(1, view.getLong(2));
    }

    @Test
    void testViewAndFilter() {
        LongColumn column = new LongColumn("test", 5);
        column.addLong(1);
        column.addLong(2);
        column.addLong(3);
        column.addLong(4);
        column.addLong(5);

        // Create a view with selections [4, 2, 0] (values: 5, 3, 1)
        int[] selections = new int[]{4, 2, 0};
        Column view = column.view(selections, 3);

        // Filter the view to keep only the first and last elements
        BitSet keep = new BitSet(3);
        keep.set(0); // keep 5
        keep.set(2); // keep 1

        Column filtered = view.filter(keep);
        assertInstanceOf(LongColumn.class, filtered);
        assertEquals(2, filtered.size());
        assertEquals(5, filtered.getLong(0));
        assertEquals(1, filtered.getLong(1));
    }

    @Test
    void testEmptyFilter() {
        LongColumn column = new LongColumn("test", 3);
        column.addLong(1);
        column.addLong(2);
        column.addLong(3);

        BitSet keep = new BitSet(3);
        Column filtered = column.filter(keep);
        assertEquals(0, filtered.size());
    }

    @Test
    void testEmptyView() {
        LongColumn column = new LongColumn("test", 3);
        column.addLong(1);
        column.addLong(2);
        column.addLong(3);

        int[] selections = new int[0];
        Column view = column.view(selections, 0);
        assertEquals(0, view.size());
    }
}
