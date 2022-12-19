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

package org.bithon.agent.plugin.thread.interceptor;

import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.expt.AgentException;
import org.bithon.agent.plugin.thread.ThreadPoolUtils;
import org.bithon.agent.plugin.thread.metrics.ThreadPoolExecutorMetrics;
import org.bithon.agent.plugin.thread.metrics.ThreadPoolMetricRegistry;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/25 9:10 下午
 */
public class ThreadPoolExecutor$Ctor extends AbstractInterceptor {
    private static final ILogAdaptor LOG = LoggerFactory.getLogger(ThreadPoolExecutor$Ctor.class);

    @Override
    public void onConstruct(AopContext aopContext) {
        ThreadPoolMetricRegistry registry = ThreadPoolMetricRegistry.getInstance();
        if (registry != null) {
            try {
                ThreadPoolExecutor executor = aopContext.castTargetAs();
                registry.addThreadPool(executor,
                                       executor.getThreadFactory().getClass().getName(),
                                       ThreadPoolUtils.getThreadPoolName(executor.getThreadFactory()),
                                       new ThreadPoolExecutorMetrics(executor));
            } catch (AgentException e) {
                LOG.warn(e.getMessage());
            }
        }
    }
}
