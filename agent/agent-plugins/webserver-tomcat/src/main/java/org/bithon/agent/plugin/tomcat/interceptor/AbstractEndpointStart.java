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

package org.bithon.agent.plugin.tomcat.interceptor;

import org.apache.tomcat.util.net.AbstractEndpoint;
import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.core.context.AgentContext;
import org.bithon.agent.core.metric.collector.MetricCollectorManager;
import org.bithon.agent.plugin.tomcat.metric.WebServerMetricCollector;

/**
 * @author frankchen
 */
public class AbstractEndpointStart extends AbstractInterceptor {

    private AbstractEndpoint<?> endpoint;

    @Override
    public void onMethodLeave(AopContext context) {
        if (null == endpoint) {
            endpoint = (AbstractEndpoint<?>) context.getTarget();

            AgentContext.getInstance().getAppInstance().setPort(endpoint.getPort());

            MetricCollectorManager.getInstance().register("webserver-tomcat", new WebServerMetricCollector(endpoint));
        }
    }
}
