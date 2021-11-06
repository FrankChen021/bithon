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

import org.bithon.agent.core.context.AgentContext;
import org.bithon.agent.core.context.AppInstance;
import org.bithon.agent.core.dispatcher.IMessageConverter;
import org.bithon.agent.core.metric.collector.IMetricCollector2;
import org.bithon.agent.core.metric.collector.IMetricSet;
import org.bithon.agent.core.metric.model.schema.IDimensionSpec;
import org.bithon.agent.core.metric.model.schema.IMetricSpec;
import org.bithon.agent.core.metric.model.schema.LongLastMetricSpec;
import org.bithon.agent.core.metric.model.schema.LongMaxMetricSpec;
import org.bithon.agent.core.metric.model.schema.LongMinMetricSpec;
import org.bithon.agent.core.metric.model.schema.LongSumMetricSpec;
import org.bithon.agent.core.metric.model.schema.Schema;
import org.bithon.agent.core.metric.model.schema.StringDimensionSpec;
import org.bithon.agent.sdk.expt.SdkException;
import org.bithon.agent.sdk.metric.IMetricValueProvider;
import org.bithon.agent.sdk.metric.aggregator.LongMax;
import org.bithon.agent.sdk.metric.aggregator.LongMin;
import org.bithon.agent.sdk.metric.aggregator.LongSum;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author Frank Chen
 * @date 2021-10-01
 */
public class MetricsRegistryDelegate implements IMetricCollector2 {

    private static final Logger log = LoggerFactory.getLogger(MetricsRegistryDelegate.class);

    static class NullMetricValueProvider implements IMetricValueProvider {
        static NullMetricValueProvider INSTANCE = new NullMetricValueProvider();

        @Override
        public long value() {
            return 0;
        }
    }

    static class MetricSet implements IMetricSet {
        private final List<String> dimensions;
        private final Object metricProvider;
        private final List<Field> metricFields;

        public Object getMetricProvider() {
            return metricProvider;
        }

        MetricSet(Object metricProvider, List<String> dimensions, List<Field> metricFields) {
            this.dimensions = dimensions;
            this.metricProvider = metricProvider;
            this.metricFields = metricFields;
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
            if (index < this.metricFields.size()) {

                Field field = this.metricFields.get(index);
                try {
                    IMetricValueProvider provider = (IMetricValueProvider) this.metricFields.get(index).get(metricProvider);
                    return provider.value();
                } catch (IllegalAccessException e) {
                    log.error("Can't get value of [{}] on class [{}]: {}",
                              field.getName(),
                              metricProvider.getClass().getName(),
                              e.getMessage());
                    return Long.MAX_VALUE;
                }
            }
            return Long.MAX_VALUE;
        }
    }

    private final Supplier<Object> metricInstantiator;
    private final Schema schema;
    private Map<List<String>, IMetricSet> metricsMap = new ConcurrentHashMap<>();

    /**
     * keep metrics that won't be cleared when they have been collected
     */
    private final Map<List<String>, IMetricSet> retainedMetricsMap = new ConcurrentHashMap<>();
    private final List<Field> metricField = new ArrayList<>();

    @SuppressWarnings("unchecked")
    protected MetricsRegistryDelegate(String name,
                                      List<String> dimensionSpec,
                                      Class<?> metricClass) {
        final Constructor<?> defaultCtor = Arrays.stream(metricClass.getConstructors())
                                                 .filter(ctor -> ctor.getParameterCount() == 0)
                                                 .findFirst()
                                                 .orElseThrow(() -> new SdkException(String.format(
                                                     "Class[%s] has no default ctor",
                                                     metricClass.getName())));
        defaultCtor.setAccessible(true);
        this.metricInstantiator = () -> {
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

        // reserved metric
        metricsSpec.add(new LongSumMetricSpec("interval"));

        this.schema = new Schema(name, dimensionSpecs, metricsSpec);
    }

    public Object getOrCreateMetric(boolean retained, String... dimensions) {
        AppInstance appInstance = AgentContext.getInstance().getAppInstance();

        String[] newDimensions = new String[dimensions.length + 2];
        newDimensions[0] = appInstance.getAppName();
        newDimensions[1] = appInstance.getHostAndPort();
        System.arraycopy(dimensions, 0, newDimensions, 2, dimensions.length);

        if (newDimensions.length != schema.getDimensionsSpec().size()) {
            throw new RuntimeException("dimensions not matched. Expected dimensions: " + schema.getDimensionsSpec());
        }

        List<String> dimensionList = Arrays.asList(newDimensions);

        //noinspection rawtypes
        Map map = retained ? retainedMetricsMap : metricsMap;
        //noinspection unchecked
        return ((MetricSet) map.computeIfAbsent(dimensionList, key -> {
            Object metricProvider = metricInstantiator.get();
            return new MetricSet(metricProvider, dimensionList, metricField);
        })).getMetricProvider();
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
        Map<List<String>, IMetricSet> metricMap = this.metricsMap;
        this.metricsMap = new ConcurrentHashMap<>();
        metricMap.putAll(retainedMetricsMap);

        return messageConverter.from(this.schema, metricMap.values(), timestamp, interval);
    }
}
