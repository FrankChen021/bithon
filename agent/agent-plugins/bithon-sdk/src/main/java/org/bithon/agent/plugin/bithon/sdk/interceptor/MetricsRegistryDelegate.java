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

package org.bithon.agent.plugin.bithon.sdk.interceptor;

import org.bithon.agent.observability.dispatcher.IMessageConverter;
import org.bithon.agent.observability.metric.collector.IMeasurement;
import org.bithon.agent.observability.metric.collector.IMetricCollector2;
import org.bithon.agent.observability.metric.model.schema.IDimensionSpec;
import org.bithon.agent.observability.metric.model.schema.IMetricSpec;
import org.bithon.agent.observability.metric.model.schema.LongLastMetricSpec;
import org.bithon.agent.observability.metric.model.schema.LongMaxMetricSpec;
import org.bithon.agent.observability.metric.model.schema.LongMinMetricSpec;
import org.bithon.agent.observability.metric.model.schema.LongSumMetricSpec;
import org.bithon.agent.observability.metric.model.schema.Schema;
import org.bithon.agent.observability.metric.model.schema.StringDimensionSpec;
import org.bithon.agent.sdk.expt.SdkException;
import org.bithon.agent.sdk.metric.IMetricValueProvider;
import org.bithon.agent.sdk.metric.aggregator.LongMax;
import org.bithon.agent.sdk.metric.aggregator.LongMin;
import org.bithon.agent.sdk.metric.aggregator.LongSum;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2021-10-01
 */
public class MetricsRegistryDelegate implements IMetricCollector2 {

    private static final ILogAdaptor LOG = LoggerFactory.getLogger(MetricsRegistryDelegate.class);
    private final Supplier<Object> metricObjectCreator;
    private final Schema schema;
    /**
     * keep metrics that won't be cleared when they have been collected
     */
    private final Map<List<String>, IMeasurement> retainedMetricsMap = new ConcurrentHashMap<>();
    private final List<Field> metricField = new ArrayList<>();
    private Map<List<String>, IMeasurement> metricsMap = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    protected MetricsRegistryDelegate(String name,
                                      List<String> dimensionSpec,
                                      Class<?> metricClass) {
        final Constructor<?> defaultCtor = Arrays.stream(metricClass.getConstructors())
                                                 .filter(ctor -> ctor.getParameterCount() == 0)
                                                 .findFirst()
                                                 .orElseThrow(() -> new SdkException("Class[%s] has no default ctor",
                                                                                     metricClass.getName()));
        defaultCtor.setAccessible(true);
        this.metricObjectCreator = () -> {
            try {
                return defaultCtor.newInstance();
            } catch (Exception e) {
                throw new SdkException("Can't instantiate metric class [%s]: %s", metricClass, e.getMessage());
            }
        };

        List<IDimensionSpec> dimensionSpecs = dimensionSpec.stream()
                                                           .map(StringDimensionSpec::new)
                                                           .collect(Collectors.toList());

        List<IMetricSpec> metricsSpec = new ArrayList<>();
        for (Field field : metricClass.getDeclaredFields()) {
            //noinspection rawtypes
            Class fieldClass = field.getType();
            if (fieldClass == LongMax.class) {
                metricsSpec.add(new LongMaxMetricSpec(field.getName()));
            } else if (fieldClass == LongMin.class) {
                metricsSpec.add(new LongMinMetricSpec(field.getName()));
            } else if (fieldClass == LongSum.class) {
                metricsSpec.add(new LongSumMetricSpec(field.getName()));
            } else if (fieldClass.isAssignableFrom(IMetricValueProvider.class)) {
                metricsSpec.add(new LongLastMetricSpec(field.getName()));
            } else {
                continue;
            }
            field.setAccessible(true);
            metricField.add(field);
        }
        if (metricField.isEmpty()) {
            throw new SdkException("[%s] has no metric defined");
        }

        this.schema = new Schema(name, dimensionSpecs, metricsSpec);
    }

    public Object getOrCreateMetric(boolean retained, String... dimensions) {
        if (dimensions.length != schema.getDimensionsSpec().size()) {
            throw new RuntimeException("dimensions not matched. Expected dimensions: " + schema.getDimensionsSpec());
        }

        List<String> dimensionList = Arrays.asList(dimensions);

        //noinspection rawtypes
        Map map = retained ? retainedMetricsMap : metricsMap;

        //noinspection unchecked
        return ((Measurement) map.computeIfAbsent(dimensionList, key -> {
            Object metricObject = metricObjectCreator.get();
            return new Measurement(metricObject, dimensionList, metricField);
        })).getMetricObject();
    }

    public Schema getSchema() {
        return schema;
    }

    @Override
    public boolean isEmpty() {
        // this make sure the schema will be sent to remote server even if the metric set is empty
        // so that remote server could show the data source
        return false;
    }

    @Override
    public Object collect(IMessageConverter messageConverter, int interval, long timestamp) {
        Collection<IMeasurement> measurementList = collectMeasurements();

        return messageConverter.from(this.schema, measurementList, timestamp, interval);
    }

    private Collection<IMeasurement> collectMeasurements() {
        if (metricsMap.isEmpty()) {
            // There might be cases that when the code runs here,
            // another thread is attempting to add metrics to the map.
            // It's OK that we return null here, as the metrics will be collected in the next round
            return this.retainedMetricsMap.isEmpty() ? Collections.emptyList() : this.retainedMetricsMap.values();
        }

        Map<List<String>, IMeasurement> collected = this.metricsMap;
        // clear the map for the next round
        this.metricsMap = new ConcurrentHashMap<>();

        if (this.retainedMetricsMap.isEmpty()) {
            return collected.values();
        }

        List<IMeasurement> measurementList = new ArrayList<>(collected.values());
        measurementList.addAll(this.retainedMetricsMap.values());
        return measurementList;
    }

    static class Measurement implements IMeasurement {
        private final List<String> dimensions;

        /**
         * The user object that defines metrics
         */
        private final Object metricObject;

        private final List<Field> metricFields;

        Measurement(Object metricObject, List<String> dimensions, List<Field> metricFields) {
            this.dimensions = dimensions;
            this.metricObject = metricObject;
            this.metricFields = metricFields;
        }

        public Object getMetricObject() {
            return metricObject;
        }

        @Override
        public List<String> getDimensions() {
            return dimensions;
        }

        @Override
        public int getMetricCount() {
            return this.metricFields.size();
        }

        @Override
        public long getMetricValue(int index) {
            if (index >= this.metricFields.size()) {
                return Long.MAX_VALUE;
            }

            Field field = this.metricFields.get(index);
            try {
                IMetricValueProvider provider = (IMetricValueProvider) this.metricFields.get(index).get(metricObject);
                return provider.value();
            } catch (IllegalAccessException e) {
                LOG.error("Can't get value of [{}] on class [{}]: {}",
                          field.getName(),
                          metricObject.getClass().getName(),
                          e.getMessage());
                return Long.MAX_VALUE;
            }
        }
    }
}
