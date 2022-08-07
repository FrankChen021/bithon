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

package org.bithon.agent.sdk.metric;

import org.bithon.agent.sdk.expt.SdkException;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author frank.chen021@outlook.com
 * @date 2021-10-02
 */
public class MetricRegistryFactory {

    static class EmptyMetricsRegistry<T> implements IMetricsRegistry<T> {
        final Constructor<T> defaultCtor;

        public EmptyMetricsRegistry(Class<T> metricClass) {
            //noinspection unchecked
            defaultCtor = (Constructor<T>) Stream.of(metricClass.getConstructors())
                                                 .filter(ctor -> ctor.getParameterCount() == 0)
                                                 .findFirst()
                                                 .orElseThrow(() -> new SdkException("[%s] has no default constructor",
                                                                                     metricClass.getName()));
        }

        @Override
        public T getOrCreateMetric(String... dimensions) {
            System.out.println("MetricRegistryFactory is not installed correctly");
            try {
                return defaultCtor.newInstance();
            } catch (Exception e) {
                throw new SdkException("Can't instantiate metric class[%s]: %s",
                                       defaultCtor.getDeclaringClass().getName(),
                                       e.getMessage());
            }
        }

        @Override
        public T getPermanentMetrics(String... dimensions) {
            System.out.println("MetricRegistryFactory is not installed correctly");
            try {
                return defaultCtor.newInstance();
            } catch (Exception e) {
                throw new SdkException("Can't instantiate metric class[%s]: %s",
                                       defaultCtor.getDeclaringClass().getName(),
                                       e.getMessage());
            }
        }

        @Override
        public void unregister() {
            System.out.println("MetricRegistryFactory is not installed correctly");
        }

    }

    public static <T> IMetricsRegistry<T> create(String name,
                                                 List<String> dimensionSpec,
                                                 Class<T> metricClass) {
        //
        // return an object in case of missing agent
        // so that any call on this object won't cause NPE but our defined exception
        //
        return new EmptyMetricsRegistry<>(metricClass);
    }
}
