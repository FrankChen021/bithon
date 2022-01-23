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

package org.bithon.agent.core.metric.collector;

import org.bithon.agent.bootstrap.expt.AgentException;
import org.bithon.agent.core.dispatcher.IMessageConverter;
import org.bithon.agent.core.metric.model.IMetricSet;
import org.bithon.agent.core.metric.model.IMetricValueProvider;
import org.bithon.agent.core.metric.model.schema.Schema2;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * This collector clear metrics after it collect metrics.
 *
 * @author frank.chen021@outlook.com
 * @date 2021/11/06 2:37 下午
 */
public abstract class IntervalMetricCollector<T extends IMetricSet> implements IMetricCollector2 {

    protected final Schema2 schema;
    private final Supplier<T> supplier;
    private final boolean clearAfterCollect;
    private Map<List<String>, IMeasurement> metricsMap = new ConcurrentHashMap<>();
    private Map<List<String>, IMeasurement> removedMap = new ConcurrentHashMap<>();

    protected IntervalMetricCollector(String name,
                                      List<String> dimensionSpec,
                                      Class<T> metricClass,
                                      Supplier<T> newMetricSupplier) {
        this(name, dimensionSpec, metricClass, newMetricSupplier, true);
    }

    protected IntervalMetricCollector(String name,
                                      List<String> dimensionSpec,
                                      Class<T> metricClass,
                                      Supplier<T> newMetricSupplier,
                                      boolean clearAfterCollect) {
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

    @SuppressWarnings("unchecked")
    protected T getOrCreateMetrics(String... dimensionValues) {
        if (dimensionValues.length != this.schema.getDimensionsSpec().size()) {
            throw new AgentException("required dimension size is {}, but input is {}", this.schema.getDimensionsSpec().size(), dimensionValues.length);
        }
        List<String> dimensions = Arrays.asList(dimensionValues);
        Measurement measurement = (Measurement) metricsMap.computeIfAbsent(dimensions, key -> new Measurement(dimensions, supplier.get()));
        return (T) measurement.metrics;
    }

    @SuppressWarnings("unchecked")
    protected T getMetrics(List<String> dimensions) {
        if (dimensions.size() != this.schema.getDimensionsSpec().size()) {
            throw new AgentException("required dimension size is {}, but input is {}", this.schema.getDimensionsSpec().size(), dimensions.size());
        }
        Measurement measurement = (Measurement) metricsMap.get(dimensions);
        return measurement == null ? null : (T) measurement.metrics;
    }

    @SuppressWarnings("unchecked")
    protected T register(List<String> dimensions, T metrics) {
        if (dimensions.size() != this.schema.getDimensionsSpec().size()) {
            throw new AgentException("required dimension size is {}, but input is {}", this.schema.getDimensionsSpec().size(), dimensions.size());
        }
        Measurement measurement = (Measurement) metricsMap.computeIfAbsent(dimensions, key -> new Measurement(dimensions, metrics));
        return (T) measurement.metrics;
    }

    protected void unregister(List<String> dimensions, boolean flush) {
        IMeasurement measurement = metricsMap.remove(dimensions);
        if (measurement != null) {
            removedMap.put(dimensions, measurement);
        }
    }

    @Override
    public boolean isEmpty() {
        return metricsMap.isEmpty() && removedMap.isEmpty();
    }

    @Override
    public Object collect(IMessageConverter messageConverter, int interval, long timestamp) {
        Map<List<String>, IMeasurement> metrics = metricsMap;
        if (this.clearAfterCollect) {
            metricsMap = new ConcurrentHashMap<>();
        }
        Collection<IMeasurement> measurements = metrics.values();
        if (!removedMap.isEmpty()) {
            measurements = new ArrayList<>(measurements);
            measurements.addAll(removedMap.values());
            removedMap.clear();
        }
        return messageConverter.from(schema, measurements, timestamp, interval);
    }

    class Measurement implements IMeasurement {
        private final List<String> dimensions;
        private final IMetricSet metrics;

        Measurement(List<String> dimensions, IMetricSet metrics) {
            this.dimensions = dimensions;
            this.metrics = metrics;
        }

        @Override
        public List<String> getDimensions() {
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
}
