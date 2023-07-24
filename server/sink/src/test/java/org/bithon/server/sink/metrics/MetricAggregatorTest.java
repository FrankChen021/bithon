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

package org.bithon.server.sink.metrics;

import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.TimestampSpec;
import org.bithon.server.storage.datasource.column.StringColumn;
import org.bithon.server.storage.datasource.column.aggregatable.max.AggregateLongMaxColumn;
import org.bithon.server.storage.datasource.column.aggregatable.min.AggregateLongMinColumn;
import org.bithon.server.storage.datasource.column.aggregatable.sum.AggregateLongSumColumn;
import org.bithon.server.storage.datasource.input.IInputRow;
import org.bithon.server.storage.datasource.input.InputRow;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 13/4/22 3:30 PM
 */
@RunWith(Parameterized.class)
public class MetricAggregatorTest {

    private final DataSourceSchema schema;

    /**
     * The arguments are passed from the method {@link #createSchemas()} below.
     */
    public MetricAggregatorTest(String name, DataSourceSchema schema) {
        this.schema = schema;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Object[] createSchemas() {
        DataSourceSchema emptyDimensionSchema = new DataSourceSchema(
            "empty-dimension-table",
            "empty-dimension-table",
            new TimestampSpec("timestamp", null, null),
            Collections.emptyList(),
            Arrays.asList(new AggregateLongSumColumn("totalCount", "totalCount"),
                          new AggregateLongMinColumn("minTime", "minTime" ),
                          new AggregateLongMaxColumn("maxTime", "maxTime"))
        );

        DataSourceSchema hasDimensionSchema = new DataSourceSchema(
            "one-dimension-table",
            "one-dimension-table",
            new TimestampSpec("timestamp", null, null),
            Arrays.asList(new StringColumn("appName", "appName")),
            Arrays.asList(new AggregateLongSumColumn("totalCount", "totalCount"),
                          new AggregateLongMinColumn("minTime", "minTime"),
                          new AggregateLongMaxColumn("maxTime", "maxTime"))
        );

        return new Object[]{
            new Object[]{emptyDimensionSchema.getName(), emptyDimensionSchema},
            new Object[]{hasDimensionSchema.getName(), hasDimensionSchema}
        };
    }

    @Test
    public void testInSameTimeBucket() {

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
        Assert.assertEquals(1, rows.size()); // the two rows are in the same time bucket
        Assert.assertEquals(2L, rows.get(0).getColAsLong("totalCount", 0));
        Assert.assertEquals(1L, rows.get(0).getColAsLong("minTime", 0));
        Assert.assertEquals(5L, rows.get(0).getColAsLong("maxTime", 0));
    }

    @Test
    public void testInDifferentTimeBucket() {
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
        Assert.assertEquals(2, rows.size()); // the two rows are in the same time bucket
        Assert.assertEquals(1L, rows.get(0).getColAsLong("totalCount", 0));
        Assert.assertEquals(1L, rows.get(0).getColAsLong("minTime", 0));
        Assert.assertEquals(1L, rows.get(0).getColAsLong("maxTime", 0));

        Assert.assertEquals(1L, rows.get(1).getColAsLong("totalCount", 0));
        Assert.assertEquals(5L, rows.get(1).getColAsLong("minTime", 0));
        Assert.assertEquals(5L, rows.get(1).getColAsLong("maxTime", 0));
    }

    @Test
    public void testInDifferentDimension() {
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
        Assert.assertEquals(2, rows.size()); // the two rows are in the same time bucket
        Assert.assertEquals("app1", rows.get(0).getColAsString("appName"));
        Assert.assertEquals(1L, rows.get(0).getColAsLong("totalCount", 0));
        Assert.assertEquals(1L, rows.get(0).getColAsLong("minTime", 0));
        Assert.assertEquals(1L, rows.get(0).getColAsLong("maxTime", 0));

        Assert.assertEquals("app2", rows.get(1).getColAsString("appName"));
        Assert.assertEquals(1L, rows.get(1).getColAsLong("totalCount", 0));
        Assert.assertEquals(5L, rows.get(1).getColAsLong("minTime", 0));
        Assert.assertEquals(5L, rows.get(1).getColAsLong("maxTime", 0));
    }
}
