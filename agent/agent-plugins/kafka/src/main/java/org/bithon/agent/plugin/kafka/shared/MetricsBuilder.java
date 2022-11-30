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

package org.bithon.agent.plugin.kafka.shared;

import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 *
 * @author frankchen
 */
public class MetricsBuilder {

    private static final ILogAdaptor LOG = LoggerFactory.getLogger(MetricsBuilder.class);

    /**
     * Class - the class
     * Map<String, Field> - String: field name; Field: field object
     */
    private static final Map<Class<?>, Map<String, Field>> FIELDS_CACHE = new ConcurrentHashMap<>();


    public static <T> T toMetricSet(List<Map.Entry<MetricName, ? extends Metric>> kafkaMetrics,
                                    Class<T> metricSetClass) {
        try {
            T result = metricSetClass.getConstructor().newInstance();

            kafkaMetrics.forEach((metric) -> getField(metricSetClass, metric.getKey()).ifPresent(
                field -> {
                    try {
                        double value = (double) metric.getValue().metricValue();
                        if (Double.isInfinite(value) || Double.isNaN(value)) {
                            value = 0.0;
                        }
                        field.setDouble(result, value);
                    } catch (Exception e) {
                        LOG.error("[kafka metrics] fail to buildEntity field {} {} {}",
                                  metricSetClass,
                                  field.getName(),
                                  e.getMessage());
                    }
                }));
            return result;
        } catch (Exception e) {
            LOG.error("[kafka metrics] fail to create entity {} {}", metricSetClass, e.getMessage());
        }
        return null;
    }

    public static <T> Map<String, T> toMetricsGroupBy(List<Map.Entry<MetricName, ? extends Metric>> kafkaMetrics,
                                                      String groupBy,
                                                      Class<T> targetMetricClass) {
        Map<String, List<Map.Entry<MetricName, ? extends Metric>>> groupedMetrics = kafkaMetrics.stream()
                                                                                                .collect(Collectors.groupingBy(e -> e.getKey()
                                                                                                                                     .tags()
                                                                                                                                     .get(groupBy)));

        Map<String, T> result = new HashMap<>(groupedMetrics.size());
        groupedMetrics.forEach((tag, oneTagMetrics) -> {
            T one = toMetricSet(oneTagMetrics, targetMetricClass);
            if (one != null) {
                result.put(tag, one);
            }
        });
        return result;
    }

    private static Optional<Field> getField(Class<?> clazz, MetricName metricName) {
        Map<String, Field> classFieldMap = FIELDS_CACHE.computeIfAbsent(clazz,
                                                                        key -> Arrays.stream(clazz.getFields())
                                                                                     .filter(field -> field.getModifiers() == Modifier.PUBLIC)
                                                                                     .collect(Collectors.toMap(k -> Tools.camelCaseToDash(k.getName()),
                                                                                                               value -> value)));

        return Optional.ofNullable(classFieldMap.get(metricName.name()));
    }

}
