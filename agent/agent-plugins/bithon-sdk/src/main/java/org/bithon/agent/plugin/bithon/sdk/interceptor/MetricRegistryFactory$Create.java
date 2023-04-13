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

import org.bithon.agent.instrumentation.aop.interceptor.ReplaceInterceptor;
import org.bithon.agent.instrumentation.loader.PluginClassLoaderManager;
import org.bithon.agent.observability.dispatcher.IMessageConverter;
import org.bithon.agent.observability.metric.collector.IMetricCollector2;
import org.bithon.agent.observability.metric.collector.IMetricCollectorBase;
import org.bithon.agent.observability.metric.collector.MetricCollectorManager;
import org.bithon.agent.sdk.expt.SdkException;
import org.bithon.agent.sdk.metric.IMetricsRegistry;
import org.bithon.agent.sdk.metric.MetricRegistryFactory;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.StringUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

/**
 * A replacement of {@link MetricRegistryFactory#create(String, List, Class)}
 *
 * @author frank.chen021@outlook.com
 * @date 2021-10-01
 */
public class MetricRegistryFactory$Create extends ReplaceInterceptor {

    private static final ILogAdaptor log = LoggerFactory.getLogger(MetricRegistryFactory$Create.class);

    @Override
    public Object execute(Object[] args, Object returning) {
        String name = (String) args[0];
        if (StringUtils.isEmpty(name)) {
            throw new SdkException("name can't be null");
        }

        //noinspection unchecked
        List<String> dimensionSpec = (List<String>) args[1];
        if (CollectionUtils.isEmpty(dimensionSpec)) {
            throw new SdkException("dimensionSpec can't be null");
        }

        Class<?> metricClass = (Class<?>) args[2];
        if (metricClass == null) {
            throw new SdkException("metricClass can't be null");
        }

        // TODO: use bytebuddy to generate dynamic proxy
        MetricRegistryInvocationHandler proxyHandler = new MetricRegistryInvocationHandler(name,
                                                                                           dimensionSpec,
                                                                                           metricClass);
        Object delegate = Proxy.newProxyInstance(PluginClassLoaderManager.getDefaultLoader(),
                                                 new Class[]{IMetricsRegistry.class, IMetricCollector2.class},
                                                 proxyHandler);

        MetricCollectorManager.getInstance().register(name, (IMetricCollectorBase) delegate);

        log.info("MetricRegister[{}] Registered with Dimensions: {}, Metrics: {}",
                 name,
                 dimensionSpec,
                 proxyHandler.delegate.getSchema().getMetricsSpec());
        return delegate;
    }

    static class MetricRegistryInvocationHandler implements InvocationHandler {

        private final MetricsRegistryDelegate delegate;

        public MetricRegistryInvocationHandler(String name,
                                               List<String> dimensionSpec,
                                               Class<?> metricClass) {
            delegate = new MetricsRegistryDelegate(name,
                                                   dimensionSpec,
                                                   metricClass);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if ("getOrCreateMetric".equals(method.getName())) {
                return delegate.getOrCreateMetric(false, (String[]) args[0]);
            }
            if ("getPermanentMetrics".equals(method.getName())) {
                return delegate.getOrCreateMetric(true, (String[]) args[0]);
            }
            if ("unregister".equals(method.getName())) {
                MetricCollectorManager.getInstance().unregister(delegate.getSchema().getName());
                return null;
            }
            if ("isEmpty".equals(method.getName())) {
                return delegate.isEmpty();
            }
            if ("collect".equals(method.getName())) {
                return delegate.collect((IMessageConverter) args[0], (int) args[1], (long) args[2]);
            }
            throw new SdkException("method [%s] is not implemented", method.getName());
        }
    }
}
