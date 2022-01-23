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

import org.bithon.agent.core.metric.model.IMetricSet;

import java.util.List;
import java.util.function.Supplier;

/**
 * @author Frank Chen
 * @date 23/1/22 3:58 PM
 */
public class MetricRegistryFactory {

    public static <T extends IMetricSet> MetricRegistry<T> getOrCreateRegistry(String registryName,
                                                                               List<String> dimensionNames,
                                                                               Class<T> metricSetClass,
                                                                               Supplier<T> newMetricSupplier,
                                                                               boolean clearAfterCollect) {
        MetricRegistry.Collector<T> collector = MetricCollectorManager.getInstance()
                                                                      .getOrRegister(registryName,
                                                                                     () -> {
                                                                                         MetricRegistry<T> registry = new MetricRegistry<T>(registryName,
                                                                                                                                            dimensionNames,
                                                                                                                                            metricSetClass,
                                                                                                                                            newMetricSupplier,
                                                                                                                                            clearAfterCollect);
                                                                                         return new MetricRegistry.Collector<T>(registry);
                                                                                     });
        return collector.registry;
    }

    public static <T extends IMetricSet, R extends MetricRegistry<T>> R getOrCreateRegistry(String registryName, Supplier<R> registrySupplier) {
        MetricRegistry.Collector<T> collector = MetricCollectorManager.getInstance()
                                                                      .getOrRegister(registryName, () -> new MetricRegistry.Collector<T>(registrySupplier.get()));
        //noinspection unchecked
        return (R) collector.registry;
    }
}
