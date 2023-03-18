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

package org.bithon.agent.plugin.undertow.interceptor;

import io.undertow.Undertow;
import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.expt.AgentException;
import org.bithon.agent.observability.context.AppInstance;
import org.bithon.agent.observability.metric.collector.MetricRegistryFactory;
import org.bithon.agent.observability.metric.domain.web.WebServerMetricRegistry;
import org.bithon.agent.observability.metric.domain.web.WebServerMetrics;
import org.bithon.agent.observability.metric.domain.web.WebServerType;
import org.bithon.component.commons.utils.ReflectionUtils;
import org.xnio.XnioWorker;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/**
 * @author frankchen
 */
public class UndertowStart extends AbstractInterceptor {

    @Override
    public void onMethodLeave(AopContext aopContext) {
        if (AppInstance.getInstance().getPort() != 0 || aopContext.hasException()) {
            return;
        }

        Undertow server = (Undertow) aopContext.getTarget();

        List<?> listeners = (List<?>) ReflectionUtils.getFieldValue(server, "listeners");
        int port = Integer.parseInt(ReflectionUtils.getFieldValue(listeners.iterator().next(), "port").toString());
        AppInstance.getInstance().setPort(port);

        XnioWorker worker = (XnioWorker) ReflectionUtils.getFieldValue(server, "worker");
        TaskPoolAccessor accessor = new TaskPoolAccessor(ReflectionUtils.getFieldValue(worker, "taskPool"));

        WebServerMetrics metrics = MetricRegistryFactory.getOrCreateRegistry(WebServerMetricRegistry.NAME, WebServerMetricRegistry::new)
                                                        .getOrCreateMetrics(Collections.singletonList(WebServerType.UNDERTOW.type()),
                                                                            WebServerMetrics::new);
        metrics.activeThreads.setProvider(accessor.getActiveCount::getValue);
        metrics.maxThreads.setProvider(accessor.getMaximumPoolSize::getValue);
        metrics.queueSize.setProvider(accessor.getQueueSize::getValue);
        metrics.pooledThreads.setProvider(accessor.getPoolSize::getValue);
    }

    /**
     * Implementations of TaskPool in different version(3.3.8 used by Undertow 1.x vs 3.8 used by Undertow 2.x) differ from each other
     * Fortunately, they have same method name so that we could use reflect to unify the code together.
     * <p>
     * For undertow 1.x, the method is provided by the parent of TaskPool, however, on 2.x, the TaskPool is defined as an interface as XnioWorker$TaskPool
     */
    static class TaskPoolAccessor {
        private final Invoker getActiveCount;
        private final Invoker getMaximumPoolSize;
        private final Invoker getPoolSize;
        private final Invoker getQueueSize;

        TaskPoolAccessor(Object taskPool) {
            getActiveCount = new Invoker(taskPool, "getActiveCount") {
                @Override
                public int getValue() {
                    int v = super.getValue();
                    return v == -1 ? 0 : v;
                }
            };
            getMaximumPoolSize = new Invoker(taskPool, "getMaximumPoolSize");
            getPoolSize = new Invoker(taskPool, "getPoolSize");
            getQueueSize = new Invoker(taskPool, "getQueueSize");
        }
    }

    static class Invoker {
        private final Method method;
        private final Object target;

        public Invoker(Object target, String method) {
            this.method = getMethod(target.getClass(), method);
            this.target = target;
        }

        public int getValue() {
            try {
                return (int) method.invoke(target);
            } catch (IllegalAccessException | InvocationTargetException e) {
                //TODO: warning log
                return 0;
            }
        }

        Method getMethod(Class<?> clazz, String name) {
            Class<?> thisClass = clazz;
            while (thisClass != null) {
                try {
                    Method method = thisClass.getDeclaredMethod(name);
                    method.setAccessible(true);
                    return method;
                } catch (NoSuchMethodException e) {
                    thisClass = thisClass.getSuperclass();
                }
            }
            throw new AgentException("can't find [%s] in [%s]", name, clazz.getName());
        }
    }
}
