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
import org.bithon.agent.observability.metric.collector.MetricAccessorGenerator;
import org.bithon.agent.observability.metric.model.generator.AggregateFunctorGenerator;
import org.bithon.agent.observability.metric.model.generator.IAggregate;
import org.bithon.agent.observability.metric.model.schema.Dimensions;
import org.bithon.agent.observability.metric.model.schema.Schema2;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2025/2/5 20:22
 */
public class AbstractMetricStorage<T> implements IMetricCollector2 {

    private Exporter exporter;

    /**
     * Generate an instantiator that create an instance of given type {@link T}.
     */
    private final MetricAccessorGenerator.IMetricsInstantiator<T> metricsInstantiator;
    private Map<Dimensions, T> aggregatedStorage = new ConcurrentHashMap<>();

    private List<IMeasurement> rawStorage = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();

    private final IMetricAggregatePredicate<T> aggregatePredicate;
    private final BiFunction<T, T, T> aggregateFn;
    private final Schema2 schema;

    public AbstractMetricStorage(String name,
                                 List<String> dimensionSpec,
                                 Class<T> metricsClass,
                                 IMetricAggregatePredicate<T> aggregatePredicate) {
        this(name,
             dimensionSpec,
             metricsClass,
             aggregatePredicate,
             AggregateFunctorGenerator.createAggregateFunctor(metricsClass));
    }

    public AbstractMetricStorage(String name,
                                 List<String> dimensionSpec,
                                 Class<T> metricClass,
                                 IMetricAggregatePredicate<T> aggregatePredicate,
                                 IAggregate<T> aggregator) {
        this(name,
             dimensionSpec,
             metricClass,
             aggregatePredicate,
             (T prev, T now) -> {
                 aggregator.aggregate(prev, now);
                 return prev;
             });
    }

    public AbstractMetricStorage(String name,
                                 List<String> dimensionSpec,
                                 Class<T> metricClass,
                                 IMetricAggregatePredicate<T> aggregatePredicate,
                                 BiFunction<T, T, T> aggregateFn) {
        this.aggregatePredicate = aggregatePredicate;
        this.aggregateFn = aggregateFn;
        this.schema = createSchema(name, dimensionSpec, metricClass);
        this.metricsInstantiator = MetricAccessorGenerator.createInstantiator(metricClass);
    }

    /**
     * @param metricProvider fill the metric instance
     */
    public void add(Dimensions dimensions, Consumer<T> metricProvider) {
        // Create a new metric instance
        T metrics = this.metricsInstantiator.newInstance();

        // Call the consumer interface to fill metrics
        metricProvider.accept(metrics);

        // Aggregate the metrics if possible
        if (aggregatePredicate.isAggregatable(dimensions, metrics)) {
            try {
                aggregatedStorage.merge(dimensions,
                                        metrics,
                                        this.aggregateFn);
            } catch (Throwable t) {
                this.metricsInstantiator.newInstance();
            }
            // will be collected periodically
            return;
        }

        // Add to raw storage
        List<IMeasurement> batch = null;
        lock.lock();
        try {
            rawStorage.add(new Measurement(dimensions, (IMetricAccessor) metrics));
            if (rawStorage.size() >= 10_000) {
                batch = rawStorage;
                rawStorage = new ArrayList<>();
            }
        } finally {
            lock.unlock();
        }
        if (batch != null) {
            if (this.exporter == null) {
                this.exporter = Exporters.getOrCreate(Exporters.EXPORTER_NAME_METRIC);
            }
            Object messages = this.exporter.getMessageConverter().from(schema,
                                                                       batch,
                                                                       System.currentTimeMillis(),
                                                                       0);
            this.exporter.export(messages);
        }
    }

    @Override
    public Object collect(IMessageConverter messageConverter, int interval, long timestamp) {
        Map<Dimensions, T> currAggregatedStorage = this.aggregatedStorage;
        this.aggregatedStorage = new ConcurrentHashMap<>();

        List<IMeasurement> batch = currAggregatedStorage.entrySet()
                                                        .stream()
                                                        .map((e) -> new Measurement(e.getKey(),
                                                                                    (IMetricAccessor) e.getValue()))
                                                        .collect(Collectors.toList());

        lock.lock();
        try {
            if (!rawStorage.isEmpty()) {
                batch.addAll(rawStorage);
                rawStorage.clear();
            }
        } finally {
            lock.unlock();
        }

        return messageConverter.from(schema,
                                     batch,
                                     timestamp,
                                     interval);
    }

    @Override
    public boolean isEmpty() {
        return this.aggregatedStorage.isEmpty() && this.rawStorage.isEmpty();
    }

    static class Measurement implements IMeasurement {
        private final long timestamp;
        private final Dimensions dimensions;
        private final IMetricAccessor metricAccessor;

        public Measurement(Dimensions dimensions, IMetricAccessor metrics) {
            this.timestamp = System.currentTimeMillis();
            this.dimensions = dimensions;
            this.metricAccessor = metrics;
        }

        @Override
        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public Dimensions getDimensions() {
            return dimensions;
        }

        @Override
        public int getMetricCount() {
            return metricAccessor.getMetricCount();
        }

        @Override
        public long getMetricValue(int index) {
            return metricAccessor.getMetricValue(index);
        }

        @Override
        public long getMetricValue(String name) {
            return metricAccessor.getMetricValue(name);
        }
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
}
