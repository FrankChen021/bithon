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

import com.sbss.bithon.agent.bootstrap.aop.IReplacementInterceptor;
import com.sbss.bithon.agent.core.context.AgentContext;
import com.sbss.bithon.agent.core.context.AppInstance;
import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;
import com.sbss.bithon.agent.core.metric.collector.IMetricCollector2;
import com.sbss.bithon.agent.core.metric.collector.IMetricCollectorBase;
import com.sbss.bithon.agent.core.metric.collector.MetricCollectorManager;
import com.sbss.bithon.agent.core.plugin.PluginClassLoaderManager;
import com.sbss.bithon.agent.core.utils.CollectionUtils;
import com.sbss.bithon.agent.core.utils.lang.StringUtils;
import com.sbss.bithon.agent.sdk.expt.SdkException;
import com.sbss.bithon.agent.sdk.metric.IMetricsRegistry;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021-10-01
 */
public class MetricRegistryFactory$Create implements IReplacementInterceptor {

    private static final Logger log = LoggerFactory.getLogger(MetricRegistryFactory$Create.class);

    @Override
    public Object onExecute(Object[] args) {
        String name = (String) args[0];
        if (StringUtils.isEmpty(name)) {
            throw new SdkException("name can't be null");
        }

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

        public MetricRegistryInvocationHandler(String name, List<String> dimensionSpec, Class<?> metricClass) {
            dimensionSpec = new ArrayList<>(dimensionSpec);
            dimensionSpec.add(0, "appName");
            dimensionSpec.add(1, "instance");
            delegate = new MetricsRegistryDelegate(name,
                                                   dimensionSpec,
                                                   metricClass);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if ("getOrCreateMetric".equals(method.getName())) {
                AppInstance appInstance = AgentContext.getInstance().getAppInstance();

                String[] newArgs = new String[args.length + 2];
                newArgs[0] = appInstance.getAppName();
                newArgs[1] = appInstance.getHostIp();

                String[] oldArgs = (String[]) args[0];
                System.arraycopy(oldArgs, 0, newArgs, 2, oldArgs.length);
                return delegate.getOrCreateMetric(newArgs);
            }
            if ("unregister".equals(method.getName())) {
                // TODO:
                MetricCollectorManager.getInstance().unregister(delegate.getSchema().getName());
                return null;
            }
            if ("isEmpty".equals(method.getName())) {
                return delegate.isEmpty();
            }
            if ("collect".equals(method.getName())) {
                return delegate.collect((IMessageConverter) args[0], (int) args[1], (long) args[2]);
            }
            return null;
        }
    }
}
