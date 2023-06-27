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

import org.bithon.server.commons.time.Period;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.aggregator.NumberAggregator;
import org.bithon.server.storage.datasource.column.IColumnSpec;
import org.bithon.server.storage.datasource.input.IInputRow;
import org.bithon.server.storage.datasource.input.InputRow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * @author frank.chen021@outlook.com
 * @date 13/4/22 10:42 AM
 */
public class MetricsAggregator {
    private final Map<Key, FieldAggregators> rows = new HashMap<>();
    private final DataSourceSchema schema;
    private final long granularityMs;

    /**
     * @param period granularity
     */
    public MetricsAggregator(DataSourceSchema schema, Period period) {
        this.schema = schema;
        this.granularityMs = period.getMilliseconds();
    }

    public MetricsAggregator(DataSourceSchema schema, long granularity) {
        this.schema = schema;
        this.granularityMs = granularity * 1000;
    }

    public void aggregate(IInputRow row) {
        aggregate(row.getColAsLong("timestamp"),
                  row::getCol,
                  row::getCol);
    }

    public void aggregate(long timestamp, Function<String, Object> dimensionProvider, Function<String, Object> metricProvider) {
        long flooredTimestamp = timestamp / granularityMs * granularityMs;

        List<Object> dimensionValues = new ArrayList<>(schema.getDimensionsSpec().size());
        schema.getDimensionsSpec().forEach((dimensionSpec) -> {
            Object value = dimensionProvider.apply(dimensionSpec.getName());
            dimensionValues.add(value);
        });

        Key key = new Key(flooredTimestamp, dimensionValues);
        rows.computeIfAbsent(key, k -> new FieldAggregators(schema))
            .forEach((field, aggregator) -> aggregator.aggregate(timestamp, metricProvider.apply(field)));
    }

    @SuppressWarnings("rawtypes")
    public void aggregate(long timestamp, Map dimensions, Map metrics) {
        aggregate(timestamp, dimensions::get, metrics::get);
    }

    /**
     * aggregate the input metrics to a specified time slot metrics
     */
    public void aggregate(Measurement measurement) {
        if (measurement == null) {
            return;
        }

        aggregate(measurement.getTimestamp(), measurement.getDimensions(), measurement.getMetrics());
    }

    /**
     * get aggregated result
     */
    public List<IInputRow> getRows() {
        if (this.rows.isEmpty()) {
            return Collections.emptyList();
        }

        final List<IInputRow> finalRows = new ArrayList<>(this.rows.size());
        this.rows.forEach((key, aggregators) -> {
            Map<String, Object> row = new HashMap<>(23);

            // timestamp
            row.put("timestamp", key.timestamp);

            // dimensions
            int i = 0;
            for (IColumnSpec dimensionSpec : this.schema.getDimensionsSpec()) {
                row.put(dimensionSpec.getName(), key.dimensions.get(i++));
            }

            // metrics
            aggregators.forEach((name, aggregator) -> row.put(name, aggregator.getNumber()));

            finalRows.add(new InputRow(row));
        });
        return finalRows;
    }

    static class Key {
        // floored timestamp
        private final long timestamp;
        private final List<Object> dimensions;

        Key(long timestamp, List<Object> dimensions) {
            this.timestamp = timestamp;
            this.dimensions = dimensions;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Key key = (Key) o;
            return timestamp == key.timestamp && Objects.equals(dimensions, key.dimensions);
        }

        @Override
        public int hashCode() {
            return Objects.hash(timestamp, dimensions);
        }
    }

    static class FieldAggregators extends HashMap<String, NumberAggregator> {
        public FieldAggregators(DataSourceSchema schema) {
            for (IColumnSpec metricSpec : schema.getMetricsSpec()) {
                NumberAggregator aggregator = metricSpec.createAggregator();
                if (aggregator == null) {
                    // post aggregator has no physical aggregator
                    continue;
                }

                this.put(metricSpec.getName(), aggregator);
            }
        }
    }
}
