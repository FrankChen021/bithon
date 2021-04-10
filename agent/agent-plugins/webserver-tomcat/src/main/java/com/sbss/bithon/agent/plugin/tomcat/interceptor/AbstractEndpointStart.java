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

package com.sbss.bithon.agent.plugin.tomcat.interceptor;

import com.sbss.bithon.agent.boot.aop.AbstractInterceptor;
import com.sbss.bithon.agent.boot.aop.AopContext;
import com.sbss.bithon.agent.core.context.AgentContext;
import com.sbss.bithon.agent.core.metric.collector.MetricCollectorManager;
import com.sbss.bithon.agent.plugin.tomcat.metric.WebServerMetricCollector;
import org.apache.tomcat.util.net.AbstractEndpoint;

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
