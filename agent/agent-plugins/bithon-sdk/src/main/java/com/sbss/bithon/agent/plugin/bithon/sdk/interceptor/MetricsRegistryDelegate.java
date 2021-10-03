/*
 *    Copyright 2020 bithon.cn
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

package com.sbss.bithon.agent.plugin.bithon.sdk.interceptor;

import com.sbss.bithon.agent.sdk.metric.Metrics;
import com.sbss.bithon.agent.sdk.metric.aggregator.LongMax;
import com.sbss.bithon.agent.sdk.metric.aggregator.LongMin;
import com.sbss.bithon.agent.sdk.metric.aggregator.LongSum;
import com.sbss.bithon.agent.sdk.metric.schema.IDimensionSpec;
import com.sbss.bithon.agent.sdk.metric.schema.IMetricSpec;
import com.sbss.bithon.agent.sdk.metric.schema.LongMaxMetricSpec;
import com.sbss.bithon.agent.sdk.metric.schema.LongMinMetricSpec;
import com.sbss.bithon.agent.sdk.metric.schema.LongSumMetricSpec;
import com.sbss.bithon.agent.sdk.metric.schema.Schema;
import com.sbss.bithon.agent.sdk.metric.schema.StringDimensionSpec;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author Frank Chen
 * @date 2021-10-01
 */
public class MetricsRegistryDelegate {
    private final Supplier<Object> metricInstantiator;
    private final Schema schema;
    private Map<List<String>, Object> metricsMap = new ConcurrentHashMap<>();
    private long start = System.currentTimeMillis();

    protected MetricsRegistryDelegate(String name,
                                      List<String> dimensionSpec,
                                      Supplier<Object> metricInstantiator,
                                      Class<?> metricClass) {
        this.metricInstantiator = metricInstantiator;

        List<IDimensionSpec> dimensionSpecs = dimensionSpec.stream().map(StringDimensionSpec::new).collect(Collectors.toList());

        List<IMetricSpec> metricsSpec = new ArrayList<>();
        for (Field field : metricClass.getDeclaredFields()) {
            Class fieldClass = field.getType();
            if (fieldClass == LongMax.class) {
                metricsSpec.add(new LongMaxMetricSpec(field.getName()));
            } else if (fieldClass == LongMin.class) {
                metricsSpec.add(new LongMinMetricSpec(field.getName()));
            } else if (fieldClass == LongSum.class) {
                metricsSpec.add(new LongSumMetricSpec(field.getName()));
            }
        }

        schema = new Schema(name, dimensionSpecs, metricsSpec);
    }

    public Object getOrCreateMetric(String... dimensions) {
        if (dimensions.length != schema.getDimensionsSpec().size()) {
            throw new RuntimeException("dimensions not matched. Expected dimensions: " + schema.getDimensionsSpec());
        }
        return metricsMap.computeIfAbsent(Arrays.asList(dimensions), key -> metricInstantiator.get());
    }

    public Metrics get(boolean reset) {
        long timestamp = this.start;
        long interval = (System.currentTimeMillis() - this.start) / 1000;

        Map<List<String>, Object> metricMap;
        if (reset) {
            metricMap = this.metricsMap;
            this.metricsMap = new ConcurrentHashMap<>();
            this.start = System.currentTimeMillis();
        } else {
            metricMap = new HashMap<>(this.metricsMap);
        }

        List<LinkedHashMap<String, Object>> metrics = new ArrayList<>();
        metricMap.forEach((k, v) -> {

            LinkedHashMap<String, String> dimensions = new LinkedHashMap<>();
            for (int i = 0; i < schema.getDimensionsSpec().size(); i++) {
                dimensions.put(schema.getDimensionsSpec().get(i).getName(), k.get(i));
            }

            LinkedHashMap<String, Object> row = new LinkedHashMap<>();
            row.put("timestamp", timestamp);
            row.put("interval", interval);
            row.put("dimensions", dimensions);
            row.put("metrics", v);

            metrics.add(row);
        });

        return new Metrics(schema, metrics);
    }

    public boolean isEmpty() {
        return metricsMap.isEmpty();
    }
}
