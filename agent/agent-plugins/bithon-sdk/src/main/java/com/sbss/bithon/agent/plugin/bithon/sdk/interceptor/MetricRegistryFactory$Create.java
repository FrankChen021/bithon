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

import com.sbss.bithon.agent.bootstrap.aop.AbstractInterceptor;
import com.sbss.bithon.agent.bootstrap.aop.AopContext;
import com.sbss.bithon.agent.core.context.AgentContext;
import com.sbss.bithon.agent.core.context.AppInstance;
import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;
import com.sbss.bithon.agent.core.metric.collector.IMetricCollector2;
import com.sbss.bithon.agent.core.metric.collector.IMetricCollectorBase;
import com.sbss.bithon.agent.core.metric.collector.MetricCollectorManager;
import com.sbss.bithon.agent.core.plugin.PluginClassLoaderManager;
import com.sbss.bithon.agent.sdk.metric.IMetricsRegistry;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021-10-01
 */
public class MetricRegistryFactory$Create extends AbstractInterceptor {

    @Override
    public void onMethodLeave(AopContext aopContext) {

        String name = aopContext.getArgAs(0);
        List<String> dimensionSpec = aopContext.getArgAs(1);
        Class<?> metricClass = aopContext.getArgAs(2);

        // TODO: use bytebuddy to generate dynamic proxy
        Object delegate = Proxy.newProxyInstance(PluginClassLoaderManager.getDefaultLoader(),
                                                 new Class[]{IMetricsRegistry.class, IMetricCollector2.class},
                                                 new MetricRegistryInvocationHandler(name,
                                                                                     dimensionSpec,
                                                                                     metricClass));

        // TODO: register the object into bithon's metric system
        MetricCollectorManager.getInstance().register(name, (IMetricCollectorBase) delegate);

        aopContext.setReturning(delegate);
    }

    static class MetricRegistryInvocationHandler implements InvocationHandler {

        private final MetricsRegistryDelegate delegate;

        public MetricRegistryInvocationHandler(String name, List<String> dimensionSpec, Class<?> metricClass) {
            Constructor<?> defaultCtor = Arrays.stream(metricClass.getConstructors())
                                               .filter(ctor -> ctor.getParameterCount() == 0)
                                               .findFirst()
                                               .get();

            dimensionSpec = new ArrayList<>(dimensionSpec);
            dimensionSpec.add(0, "appName");
            dimensionSpec.add(1, "instance");
            delegate = new MetricsRegistryDelegate(name,
                                                   dimensionSpec,
                                                   () -> {
                                                       try {
                                                           return defaultCtor.newInstance();
                                                       } catch (Exception e) {
                                                           throw new RuntimeException(e);
                                                       }
                                                   },
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
