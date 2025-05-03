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

package org.bithon.server.pipeline.metrics;

import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.datasource.DefaultSchema;
import org.bithon.server.datasource.TimestampSpec;
import org.bithon.server.datasource.column.StringColumn;
import org.bithon.server.datasource.column.aggregatable.max.AggregateLongMaxColumn;
import org.bithon.server.datasource.column.aggregatable.min.AggregateLongMinColumn;
import org.bithon.server.datasource.column.aggregatable.sum.AggregateLongSumColumn;
import org.bithon.server.datasource.input.IInputRow;
import org.bithon.server.datasource.input.InputRow;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author frank.chen021@outlook.com
 * @date 13/4/22 3:30 PM
 */
public class MetricAggregatorTest {

    static Stream<Arguments> testArguments() {
        DefaultSchema emptyDimensionSchema = new DefaultSchema(
            "empty-dimension-table",
            "empty-dimension-table",
            new TimestampSpec("timestamp"),
            Collections.emptyList(),
            Arrays.asList(new AggregateLongSumColumn("totalCount", "totalCount"),
                          new AggregateLongMinColumn("minTime", "minTime"),
                          new AggregateLongMaxColumn("maxTime", "maxTime"))
        );

        DefaultSchema hasDimensionSchema = new DefaultSchema(
            "one-dimension-table",
            "one-dimension-table",
            new TimestampSpec("timestamp"),
            List.of(new StringColumn("appName", "appName")),
            Arrays.asList(new AggregateLongSumColumn("totalCount", "totalCount"),
                          new AggregateLongMinColumn("minTime", "minTime"),
                          new AggregateLongMaxColumn("maxTime", "maxTime"))
        );

        return Stream.of(
            Arguments.of(emptyDimensionSchema.getName(), emptyDimensionSchema),
            Arguments.of(hasDimensionSchema.getName(), hasDimensionSchema)
        );
    }

    @ParameterizedTest
    @MethodSource("testArguments")
    public void testInSameTimeBucket(String name, DefaultSchema schema) {

        long time = TimeSpan.fromISO8601("2012-05-15T12:38:23.000+08:00").getMilliseconds();

        MetricsAggregator aggregator = new MetricsAggregator(schema, 10);

        IInputRow row1 = new InputRow(new HashMap<>());
        row1.updateColumn("timestamp", time);
        row1.updateColumn("appName", "app1");
        row1.updateColumn("totalCount", 1);
        row1.updateColumn("minTime", 1);
        row1.updateColumn("maxTime", 1);
        aggregator.aggregate(row1);

        IInputRow row2 = new InputRow(new HashMap<>());
        row2.updateColumn("timestamp", time + 3000); // in the same bucket
        row2.updateColumn("appName", "app1");
        row2.updateColumn("totalCount", 1);
        row2.updateColumn("minTime", 5);
        row2.updateColumn("maxTime", 5);
        aggregator.aggregate(row2);

        List<IInputRow> rows = aggregator.getRows();
        Assertions.assertEquals(1, rows.size()); // the two rows are in the same time bucket
        Assertions.assertEquals(2L, rows.get(0).getColAsLong("totalCount", 0));
        Assertions.assertEquals(1L, rows.get(0).getColAsLong("minTime", 0));
        Assertions.assertEquals(5L, rows.get(0).getColAsLong("maxTime", 0));
    }

    @ParameterizedTest
    @MethodSource("testArguments")
    public void testInDifferentTimeBucket(String name, DefaultSchema schema) {
        long time = TimeSpan.fromISO8601("2012-05-15T12:38:23.000+08:00").getMilliseconds();

        MetricsAggregator aggregator = new MetricsAggregator(schema, 10);

        IInputRow row1 = new InputRow(new HashMap<>());
        row1.updateColumn("timestamp", time);
        row1.updateColumn("appName", "app1");
        row1.updateColumn("totalCount", 1);
        row1.updateColumn("minTime", 1);
        row1.updateColumn("maxTime", 1);
        aggregator.aggregate(row1);

        IInputRow row2 = new InputRow(new HashMap<>());
        row2.updateColumn("timestamp", time + 7000); // in two buckets
        row1.updateColumn("appName", "app1");
        row2.updateColumn("totalCount", 1);
        row2.updateColumn("minTime", 5);
        row2.updateColumn("maxTime", 5);
        aggregator.aggregate(row2);

        List<IInputRow> rows = aggregator.getRows();
        Assertions.assertEquals(2, rows.size()); // the two rows are in the same time bucket
        Assertions.assertEquals(1L, rows.get(0).getColAsLong("totalCount", 0));
        Assertions.assertEquals(1L, rows.get(0).getColAsLong("minTime", 0));
        Assertions.assertEquals(1L, rows.get(0).getColAsLong("maxTime", 0));

        Assertions.assertEquals(1L, rows.get(1).getColAsLong("totalCount", 0));
        Assertions.assertEquals(5L, rows.get(1).getColAsLong("minTime", 0));
        Assertions.assertEquals(5L, rows.get(1).getColAsLong("maxTime", 0));
    }

    @ParameterizedTest
    @MethodSource("testArguments")
    public void testInDifferentDimension(String name, DefaultSchema schema) {
        if (schema.getDimensionsSpec().isEmpty()) {
            return;
        }

        long time = TimeSpan.fromISO8601("2012-05-15T12:38:23.000+08:00").getMilliseconds();

        MetricsAggregator aggregator = new MetricsAggregator(schema, 10);

        IInputRow row1 = new InputRow(new HashMap<>());
        row1.updateColumn("timestamp", time);
        row1.updateColumn("appName", "app1");
        row1.updateColumn("totalCount", 1);
        row1.updateColumn("minTime", 1);
        row1.updateColumn("maxTime", 1);
        aggregator.aggregate(row1);

        IInputRow row2 = new InputRow(new HashMap<>());
        row2.updateColumn("timestamp", time); // in same buckets
        row2.updateColumn("appName", "app2"); // but with different dimension
        row2.updateColumn("totalCount", 1);
        row2.updateColumn("minTime", 5);
        row2.updateColumn("maxTime", 5);
        aggregator.aggregate(row2);

        List<IInputRow> rows = aggregator.getRows();
        Assertions.assertEquals(2, rows.size()); // the two rows are in the same time bucket
        Assertions.assertEquals("app1", rows.get(0).getColAsString("appName"));
        Assertions.assertEquals(1L, rows.get(0).getColAsLong("totalCount", 0));
        Assertions.assertEquals(1L, rows.get(0).getColAsLong("minTime", 0));
        Assertions.assertEquals(1L, rows.get(0).getColAsLong("maxTime", 0));

        Assertions.assertEquals("app2", rows.get(1).getColAsString("appName"));
        Assertions.assertEquals(1L, rows.get(1).getColAsLong("totalCount", 0));
        Assertions.assertEquals(5L, rows.get(1).getColAsLong("minTime", 0));
        Assertions.assertEquals(5L, rows.get(1).getColAsLong("maxTime", 0));
    }
}
