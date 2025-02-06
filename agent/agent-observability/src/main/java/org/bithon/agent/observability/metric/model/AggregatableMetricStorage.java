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

package org.bithon.agent.observability.metric.model;

import org.bithon.agent.observability.exporter.Exporter;
import org.bithon.agent.observability.exporter.Exporters;
import org.bithon.agent.observability.exporter.IMessageConverter;
import org.bithon.agent.observability.metric.collector.IMetricCollector2;
import org.bithon.agent.observability.metric.model.schema.Dimensions;
import org.bithon.agent.observability.metric.model.schema.Schema2;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2025/2/5 20:22
 */
public class AggregatableMetricStorage<T extends IMetricSet2> implements IMetricCollector2 {
    private final Exporter exporter;
    private Map<Dimensions, T> aggregatedStorage = new ConcurrentHashMap<>();

    private List<IMeasurement> rawStorage = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();

    private final Predicate<T> aggreatePredicate;
    private final BiFunction<T, T, T> aggregateFn;
    private final Schema2 schema;

    public AggregatableMetricStorage(String name,
                                     List<String> dimensionSpec,
                                     Class<T> metricClass,
                                     Predicate<T> aggreatePredicate) {
        this(name, dimensionSpec, metricClass, aggreatePredicate, createAggregateFn(metricClass));
    }

    public AggregatableMetricStorage(String name,
                                     List<String> dimensionSpec,
                                     Class<T> metricClass,
                                     Predicate<T> aggreatePredicate,
                                     BiFunction<T, T, T> aggregateFn) {
        this.aggreatePredicate = aggreatePredicate;
        this.aggregateFn = aggregateFn;
        this.schema = createSchema(name, dimensionSpec, metricClass);

        this.exporter = Exporters.getOrCreate(Exporters.EXPORTER_NAME_METRIC);
    }

    public void add(Dimensions dimensions, T metrics) {
        if (aggreatePredicate.test(metrics)) {
            aggregatedStorage.merge(dimensions,
                                    metrics,
                                    this.aggregateFn);
            // will be collected periodically
            return;
        }

        // Add to raw storage and
        List<IMeasurement> batch = null;
        lock.lock();
        try {
            rawStorage.add(new Measurement(dimensions, metrics));
            if (rawStorage.size() >= 10_000) {
                batch = rawStorage;
                rawStorage = new ArrayList<>();
            }
        } finally {
            lock.unlock();
        }
        if (batch != null) {
            Object messages = this.exporter.getMessageConverter().from(schema,
                                                                       batch,
                                                                       System.currentTimeMillis(),
                                                                       0);
            this.exporter.export(messages);
        }
    }

    @Override
    public Object collect(IMessageConverter messageConverter, int interval, long timestamp) {
        Map<Dimensions, T> newAggregatedStorage = new ConcurrentHashMap<>(aggregatedStorage);
        Map<Dimensions, T> currAggregatedStorage = this.aggregatedStorage;
        this.aggregatedStorage = newAggregatedStorage;

        return messageConverter.from(schema,
                                     currAggregatedStorage.values()
                                                          .stream()
                                                          .map((s) -> (IMeasurement) s).collect(Collectors.toList()),
                                     timestamp,
                                     interval);
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    static class Measurement implements IMeasurement {
        private final Dimensions dimensions;
        private final IMetricSet2 metricSet;

        public Measurement(Dimensions dimensions, IMetricSet2 metrics) {
            this.dimensions = dimensions;
            this.metricSet = metrics;
        }

        @Override
        public Dimensions getDimensions() {
            return dimensions;
        }

        @Override
        public int getMetricCount() {
            return metricSet.getMetricCount();
        }

        @Override
        public long getMetricValue(int index) {
            return metricSet.getMetricValue(index);
        }
    }

    static <T extends IMetricSet2> BiFunction<T, T, T> createAggregateFn(Class<T> clazz) {
        List<IAggregateFunction<T>> aggregateFunctions = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(org.bithon.agent.observability.metric.model.annotation.Sum.class)) {
                aggregateFunctions.add(new SumAggregateFunction<>(field));
            } else if (field.isAnnotationPresent(org.bithon.agent.observability.metric.model.annotation.Max.class)) {
                aggregateFunctions.add(new MaxAggregateFunction<>(field));
            } else if (field.isAnnotationPresent(org.bithon.agent.observability.metric.model.annotation.Min.class)) {
                aggregateFunctions.add(new MinAggregateFunction<>(field));
            } else if (field.isAnnotationPresent(org.bithon.agent.observability.metric.model.annotation.Last.class)) {
                aggregateFunctions.add(new LastAggregateFunction<>(field));
            } else if (field.isAnnotationPresent(org.bithon.agent.observability.metric.model.annotation.First.class)) {
                aggregateFunctions.add(new FirstAggregateFunction<>(field));
            }
        }
        if (aggregateFunctions.isEmpty()) {
            throw new IllegalArgumentException("No aggregation annotation defined in class " + clazz.getName());
        }
        for (IAggregateFunction<T> aggregateFunction : aggregateFunctions) {
            aggregateFunction.getField().setAccessible(true);
        }
        return (prev, now) -> {
            for (IAggregateFunction<T> aggregateFunction : aggregateFunctions) {
                Field f = aggregateFunction.getField();
                try {
                    f.set(prev, aggregateFunction.aggregate(prev, now));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return prev;
        };
    }

    static <T> Schema2 createSchema(String name, List<String> dimensions, Class<T> clazz) {
        List<String> metrics = new ArrayList<>();

        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(org.bithon.agent.observability.metric.model.annotation.Sum.class)
                || field.isAnnotationPresent(org.bithon.agent.observability.metric.model.annotation.Max.class)
                || field.isAnnotationPresent(org.bithon.agent.observability.metric.model.annotation.Min.class)
                || field.isAnnotationPresent(org.bithon.agent.observability.metric.model.annotation.Last.class)
                || field.isAnnotationPresent(org.bithon.agent.observability.metric.model.annotation.First.class)
            ) {
                metrics.add(field.getName());
            }
        }

        return new Schema2(name, dimensions, metrics);
    }

    interface IAggregateFunction<T> {
        Field getField();

        long aggregate(T prev, T now) throws Exception;
    }

    static class SumAggregateFunction<T> implements IAggregateFunction<T> {
        private final Field field;

        public SumAggregateFunction(Field field) {
            this.field = field;
        }

        @Override
        public Field getField() {
            return field;
        }

        @Override
        public long aggregate(T prev, T now) throws Exception {
            return (long) field.get(prev) + (long) field.get(now);
        }
    }

    static class MinAggregateFunction<T> implements IAggregateFunction<T> {
        private final Field field;

        public MinAggregateFunction(Field field) {
            this.field = field;
        }

        @Override
        public Field getField() {
            return field;
        }

        @Override
        public long aggregate(T prev, T now) throws Exception {
            return Math.min((long) field.get(prev), (long) field.get(now));
        }
    }

    static class MaxAggregateFunction<T> implements IAggregateFunction<T> {
        private final Field field;

        public MaxAggregateFunction(Field field) {
            this.field = field;
        }

        @Override
        public Field getField() {
            return field;
        }

        @Override
        public long aggregate(T prev, T now) throws Exception {
            return Math.max((long) field.get(prev), (long) field.get(now));
        }
    }

    static class LastAggregateFunction<T> implements IAggregateFunction<T> {
        private final Field field;

        public LastAggregateFunction(Field field) {
            this.field = field;
        }

        @Override
        public Field getField() {
            return field;
        }

        @Override
        public long aggregate(T prev, T now) throws Exception {
            return (long) field.get(now);

        }
    }

    static class FirstAggregateFunction<T> implements IAggregateFunction<T> {
        private final Field field;

        public FirstAggregateFunction(Field field) {
            this.field = field;
        }

        @Override
        public Field getField() {
            return field;
        }

        @Override
        public long aggregate(T prev, T now) throws Exception {
            return (long) field.get(prev);
        }
    }
}
