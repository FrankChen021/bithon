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

package org.bithon.server.datasource.reader.jdbc;


import org.bithon.server.datasource.query.pipeline.Column;
import org.bithon.server.datasource.query.pipeline.ColumnarTable;
import org.bithon.server.datasource.reader.jdbc.pipeline.SlidingWindowAggregator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 5/5/25 3:03 pm
 */
public class SlidingWindowAggregatorTest {

    @Test
    void testAggregate_NoGroup_OneRow() {
        Column ts = Column.create("_timestamp", "LONG", 3);
        Column value = Column.create("value", "DOUBLE", 3);

        ColumnarTable table = new ColumnarTable();
        table.addColumn(ts);
        table.addColumn(value);

        // Add rows using addRow
        table.addRow(1L, 10.0);

        table = SlidingWindowAggregator.aggregate(table, "_timestamp", List.of(), Duration.ofSeconds(2), List.of("value"));
        Column agg = table.getColumn("value");

        Assertions.assertEquals(1, table.rowCount());
        Assertions.assertEquals(10.0, agg.getDouble(0), 0.0001);
    }

    @Test
    void testAggregate_NoGroup() {
        Column ts = Column.create("_timestamp", "LONG", 3);
        Column value = Column.create("value", "DOUBLE", 3);

        ColumnarTable table = new ColumnarTable();
        table.addColumn(ts);
        table.addColumn(value);

        // Add rows using addRow
        table.addRow(1L, 10.0);
        table.addRow(2L, 20.0);
        table.addRow(3L, 30.0);

        table = SlidingWindowAggregator.aggregate(table, "_timestamp", List.of(), Duration.ofSeconds(2), List.of("value"));
        Column agg = table.getColumn("value");

        Assertions.assertEquals(3, table.rowCount());
        Assertions.assertEquals(10.0, agg.getDouble(0), 0.0001);
        Assertions.assertEquals(30.0, agg.getDouble(1), 0.0001);
        Assertions.assertEquals(50.0, agg.getDouble(2), 0.0001);
    }

    @Test
    void testAggregate_SingleGroup() {
        Column ts = Column.create("_timestamp", "LONG", 3);
        Column value = Column.create("value", "DOUBLE", 3);
        Column group = Column.create("group", "STRING", 3);

        ColumnarTable table = new ColumnarTable();
        table.addColumn(ts);
        table.addColumn(value);
        table.addColumn(group);

        // Add rows using addRow
        table.addRow(1L, 10.0, "A");
        table.addRow(2L, 20.0, "A");
        table.addRow(3L, 30.0, "A");

        table = SlidingWindowAggregator.aggregate(table, "_timestamp", List.of("group"), Duration.ofSeconds(2), List.of("value"));
        Column agg = table.getColumn("value");

        Assertions.assertEquals(3, table.rowCount());
        Assertions.assertEquals(10.0, agg.getDouble(0), 0.0001);
        Assertions.assertEquals(30.0, agg.getDouble(1), 0.0001);
        Assertions.assertEquals(50.0, agg.getDouble(2), 0.0001);
    }

    @Test
    void testAggregate_MultipleGroups() {
        Column ts = Column.create("_timestamp", "LONG", 4);
        Column value = Column.create("value", "DOUBLE", 4);
        Column group = Column.create("group", "STRING", 4);

        ColumnarTable table = new ColumnarTable();
        table.addColumn(ts);
        table.addColumn(value);
        table.addColumn(group);

        // Add rows using addRow
        table.addRow(1L, 10.0, "A");
        table.addRow(2L, 20.0, "A");
        table.addRow(3L, 30.0, "A");
        table.addRow(1L, 100.0, "B");
        table.addRow(2L, 200.0, "B");
        table.addRow(3L, 300.0, "B");

        table = SlidingWindowAggregator.aggregate(table, "_timestamp", List.of("group"), Duration.ofSeconds(2), List.of("value"));
        Column agg = table.getColumn("value");

        Assertions.assertEquals(6, table.rowCount());
        Assertions.assertEquals(10.0, agg.getDouble(0), 0.0001);
        Assertions.assertEquals(30.0, agg.getDouble(1), 0.0001);
        Assertions.assertEquals(50.0, agg.getDouble(2), 0.0001);
        Assertions.assertEquals(100.0, agg.getDouble(3), 0.0001);
        Assertions.assertEquals(300.0, agg.getDouble(4), 0.0001);
        Assertions.assertEquals(500.0, agg.getDouble(5), 0.0001);
    }

    @Test
    void testAggregate_WindowEviction() {
        Column ts = Column.create("_timestamp", "LONG", 3);
        Column value = Column.create("value", "DOUBLE", 3);
        Column group = Column.create("group", "STRING", 3);

        ColumnarTable table = new ColumnarTable();
        table.addColumn(ts);
        table.addColumn(value);
        table.addColumn(group);

        // Add rows using addRow
        table.addRow(1L, 1.0, "A");
        table.addRow(5L, 2.0, "A");
        table.addRow(10L, 3.0, "A");

        table = SlidingWindowAggregator.aggregate(table, "_timestamp", List.of("group"), Duration.ofSeconds(3), List.of("value"));
        Column agg = table.getColumn("value");

        Assertions.assertEquals(3, table.rowCount());
        Assertions.assertEquals(1.0, agg.getDouble(0), 0.0001);
        Assertions.assertEquals(2.0, agg.getDouble(1), 0.0001);
        Assertions.assertEquals(3.0, agg.getDouble(2), 0.0001);
    }

    @Test
    void testAggregate_WindowEviction_2() {
        Column ts = Column.create("_timestamp", "LONG", 3);
        Column value = Column.create("value", "DOUBLE", 3);
        Column group = Column.create("group", "STRING", 3);

        ColumnarTable table = new ColumnarTable();
        table.addColumn(ts);
        table.addColumn(value);
        table.addColumn(group);

        // Add rows using addRow
        table.addRow(1L, 1.0, "A");
        table.addRow(3L, 2.0, "A");
        table.addRow(4L, 3.0, "A");
        table.addRow(6L, 4.0, "A");

        table = SlidingWindowAggregator.aggregate(table, "_timestamp", List.of("group"), Duration.ofSeconds(3), List.of("value"));
        Column agg = table.getColumn("value");

        Assertions.assertEquals(4, table.rowCount());
        Assertions.assertEquals(1.0, agg.getDouble(0), 0.0001);
        Assertions.assertEquals(3.0, agg.getDouble(1), 0.0001);

        // Including ts 2(which is missing),3,4
        Assertions.assertEquals(5.0, agg.getDouble(2), 0.0001);

        // Including ts 4,5(which is missing),6
        Assertions.assertEquals(7.0, agg.getDouble(3), 0.0001);
    }

    @Test
    void testAggregate_MultipleInputFields() {
        Column ts = Column.create("_timestamp", "LONG", 3);
        Column value1 = Column.create("value1", "DOUBLE", 3);
        Column value2 = Column.create("value2", "DOUBLE", 3);
        Column group = Column.create("group", "STRING", 3);

        ColumnarTable table = new ColumnarTable();
        table.addColumn(ts);
        table.addColumn(value1);
        table.addColumn(value2);
        table.addColumn(group);

        // Add rows
        table.addRow(1L, 10.0, 100.0, "A");
        table.addRow(2L, 20.0, 200.0, "A");
        table.addRow(3L, 30.0, 300.0, "A");
        table.addRow(1L, 5.0, 50.0, "B");
        table.addRow(2L, 15.0, 150.0, "B");


        table = SlidingWindowAggregator.aggregate(table,
                                                  "_timestamp",
                                                  List.of("group"),
                                                  Duration.ofSeconds(2),
                                                  List.of("value1", "value2"));

        Column agg1 = table.getColumn("value1");
        Column agg2 = table.getColumn("value2");

        Assertions.assertEquals(5, table.rowCount());

        // Group A
        Assertions.assertEquals(10.0, agg1.getDouble(0), 0.0001);
        Assertions.assertEquals(100.0, agg2.getDouble(0), 0.0001);

        Assertions.assertEquals(30.0, agg1.getDouble(1), 0.0001); // 10 + 20
        Assertions.assertEquals(300.0, agg2.getDouble(1), 0.0001); // 100 + 200

        Assertions.assertEquals(50.0, agg1.getDouble(2), 0.0001); // 20 + 30
        Assertions.assertEquals(500.0, agg2.getDouble(2), 0.0001); // 200 + 300

        // Group B
        Assertions.assertEquals(5.0, agg1.getDouble(3), 0.0001);
        Assertions.assertEquals(50.0, agg2.getDouble(3), 0.0001);

        Assertions.assertEquals(20.0, agg1.getDouble(4), 0.0001); // 5 + 15
        Assertions.assertEquals(200.0, agg2.getDouble(4), 0.0001); // 50 + 150
    }
}
