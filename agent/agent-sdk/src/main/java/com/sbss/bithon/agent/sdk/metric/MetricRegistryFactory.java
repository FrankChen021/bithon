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

package com.sbss.bithon.agent.sdk.metric;

import java.util.List;

/**
 * @author Frank Chen
 * @date {} {}
 */
public class MetricRegistryFactory {

    static class NotImplementedException extends RuntimeException {
        public NotImplementedException(String message) {
            super(message);
        }
    }

    static class EmptyMetricProvider {
    }

    static class NotImplemented implements IMetricsRegistry<EmptyMetricProvider> {

        static final NotImplemented INSTANCE = new NotImplemented();

        @Override
        public EmptyMetricProvider getOrCreateMetric(String... dimensions) {
            throw new NotImplementedException("'getOrCreateMetric' proxy is not installed correctly");
        }

        @Override
        public Metrics get(boolean reset) {
            throw new NotImplementedException("'get' proxy is not installed correctly");
        }

        @Override
        public void unregister() {
            throw new NotImplementedException("'unregister' proxy is not installed correctly");
        }

    }

    public static <T> IMetricsRegistry<T> create(String name,
                                                 List<String> dimensionSpec,
                                                 Class<T> metricClass) {
        //
        // return an object in case of missing agent
        // so that any call on this object won't cause NPE but our defined exception
        //
        //noinspection unchecked
        return (IMetricsRegistry<T>) NotImplemented.INSTANCE;
    }
}
