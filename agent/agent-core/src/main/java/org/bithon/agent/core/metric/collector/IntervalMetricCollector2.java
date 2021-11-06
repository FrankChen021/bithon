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

import org.bithon.agent.core.dispatcher.IMessageConverter;
import org.bithon.agent.core.metric.model.ICompositeMetric;
import org.bithon.agent.core.metric.model.Max;
import org.bithon.agent.core.metric.model.Min;
import org.bithon.agent.core.metric.model.Sum;
import org.bithon.agent.core.metric.model.schema.Schema2;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This collector clear metrics after it collect metrics.
 *
 * @author frank.chen021@outlook.com
 * @date 2021/11/06 2:37 下午
 */
public abstract class IntervalMetricCollector2<T extends ICompositeMetric> implements IMetricCollector2 {

    class MetricSet implements IMetricSet {
        private final List<String> dimensions;
        private final ICompositeMetric metrics;

        MetricSet(List<String> dimensions, ICompositeMetric metrics) {
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

    private final Schema2 schema;
    private Map<List<String>, IMetricSet> metricsMap = new ConcurrentHashMap<>();

    protected IntervalMetricCollector2(String name, List<String> dimensionSpec, Class<T> metricClass) {
        List<String> metricsSpec = new ArrayList<>();
        for (Field field : metricClass.getDeclaredFields()) {
            //noinspection rawtypes
            Class fieldClass = field.getType();
            if (fieldClass == Max.class) {
                metricsSpec.add(field.getName());
            } else if (fieldClass == Min.class) {
                metricsSpec.add(field.getName());
            } else if (fieldClass == Sum.class) {
                metricsSpec.add(field.getName());
            }
        }
        schema = new Schema2(name, dimensionSpec, metricsSpec);
    }

    @SuppressWarnings("unchecked")
    protected T getOrCreateMetric(String... dimensionValues) {
        if (dimensionValues.length != this.schema.getDimensionsSpec().size()) {
            // TODO: exception
        }
        List<String> dimensions = Arrays.asList(dimensionValues);
        MetricSet metricSet = (MetricSet) metricsMap.computeIfAbsent(dimensions, key -> new MetricSet(dimensions, newMetrics()));
        return (T) metricSet.metrics;
    }

    @Override
    public boolean isEmpty() {
        return metricsMap.isEmpty();
    }

    @Override
    public Object collect(IMessageConverter messageConverter, int interval, long timestamp) {
        //swap metrics
        Map<List<String>, IMetricSet> metrics = metricsMap;
        metricsMap = new ConcurrentHashMap<>();

        return messageConverter.from(schema, metrics.values(), timestamp, interval);
    }

    protected abstract T newMetrics();
}
