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

package org.bithon.agent.observability.metric.collector;

import org.bithon.agent.instrumentation.expt.AgentException;
import org.bithon.agent.observability.exporter.IMessageConverter;
import org.bithon.agent.observability.metric.model.IMetricSet;
import org.bithon.agent.observability.metric.model.IMetricValueProvider;
import org.bithon.agent.observability.metric.model.schema.Dimensions;
import org.bithon.agent.observability.metric.model.schema.Schema2;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Separated from original metrics collector
 *
 * @author frank.chen021@outlook.com
 * @date 2021/11/06 2:37 下午
 */
public class MetricRegistry<T extends IMetricSet> {

    private final Schema2 schema;
    private final Supplier<T> supplier;
    private final boolean clearAfterCollect;
    private final Map<Dimensions, IMeasurement> removedMap = new ConcurrentHashMap<>();
    private Map<Dimensions, IMeasurement> metricsMap = new ConcurrentHashMap<>();

    public MetricRegistry(String name, List<String> dimensionSpec, Class<T> metricClass, Supplier<T> newMetricSupplier, boolean clearAfterCollect) {
        List<String> metricsSpec = new ArrayList<>();
        for (Field field : metricClass.getDeclaredFields()) {
            //noinspection rawtypes
            Class fieldClass = field.getType();
            if (IMetricValueProvider.class.isAssignableFrom(fieldClass)) {
                metricsSpec.add(field.getName());
            }
        }
        this.schema = new Schema2(name, dimensionSpec, metricsSpec);
        this.supplier = newMetricSupplier;
        this.clearAfterCollect = clearAfterCollect;
    }

    protected T getOrCreateMetrics(String... dimensions) {
        if (dimensions.length != this.schema.getDimensionsSpec().size()) {
            throw new AgentException("required dimension size is %d, but input is %d", this.schema.getDimensionsSpec().size(), dimensions.length);
        }
        return createMetrics(Dimensions.of(dimensions), supplier.get());
    }

    @SuppressWarnings("unchecked")
    public T getMetrics(Dimensions dimensions) {
        if (dimensions.length() != this.schema.getDimensionsSpec().size()) {
            throw new AgentException("required dimension size is %d, but input is %d", this.schema.getDimensionsSpec().size(), dimensions.length());
        }
        Measurement measurement = (Measurement) metricsMap.get(dimensions);
        return measurement == null ? null : (T) measurement.metrics;
    }

    @SuppressWarnings("unchecked")
    public T createMetrics(Dimensions dimensions, T metrics) {
        if (dimensions.length() != this.schema.getDimensionsSpec().size()) {
            throw new AgentException("required dimension size is %d, but input is %d", this.schema.getDimensionsSpec().size(), dimensions.length());
        }
        Measurement measurement = (Measurement) metricsMap.computeIfAbsent(dimensions, key -> new Measurement(dimensions, metrics));
        return (T) measurement.metrics;
    }

    public T getOrCreateMetrics(Dimensions dimensions, Supplier<T> supplier) {
        if (dimensions.length() != this.schema.getDimensionsSpec().size()) {
            throw new AgentException("required dimension size is %d, but input is %d", this.schema.getDimensionsSpec().size(), dimensions.length());
        }
        return createMetrics(dimensions, supplier.get());
    }

    public void removeMetrics(Dimensions dimensions) {
        IMeasurement measurement = metricsMap.remove(dimensions);
        if (measurement != null) {
            removedMap.put(dimensions, measurement);
        }
    }

    private class Measurement implements IMeasurement {
        private final Dimensions dimensions;
        private final IMetricSet metrics;

        Measurement(Dimensions dimensions, IMetricSet metrics) {
            this.dimensions = dimensions;
            this.metrics = metrics;
        }

        @Override
        public Dimensions getDimensions() {
            return dimensions;
        }

        @Override
        public int getMetricCount() {
            return schema.getMetricsSpec().size();
        }

        @Override
        public long getMetricValue(int index) {
            return metrics.getMetrics()[index].get();
        }
    }

    static class Collector<T extends IMetricSet> implements IMetricCollector2 {
        final MetricRegistry<T> registry;

        Collector(MetricRegistry<T> registry) {
            this.registry = registry;
        }

        @Override
        public boolean isEmpty() {
            return registry.metricsMap.isEmpty() && registry.removedMap.isEmpty();
        }

        @Override
        public Object collect(IMessageConverter messageConverter, int interval, long timestamp) {
            registry.onCollect();

            Map<Dimensions, IMeasurement> metrics = registry.metricsMap;
            if (registry.clearAfterCollect) {
                registry.metricsMap = new ConcurrentHashMap<>();
            }
            Collection<IMeasurement> measurements = metrics.values();
            if (!registry.removedMap.isEmpty()) {
                measurements = new ArrayList<>(measurements);
                measurements.addAll(registry.removedMap.values());
                registry.removedMap.clear();
            }
            return messageConverter.from(registry.schema, measurements, timestamp, interval);
        }
    }

    protected void onCollect() {
    }
}
