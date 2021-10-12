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

package org.bithon.agent.plugin.tomcat.metric;

import org.apache.tomcat.util.net.AbstractEndpoint;
import org.bithon.agent.core.dispatcher.IMessageConverter;
import org.bithon.agent.core.metric.collector.IMetricCollector;
import org.bithon.agent.core.metric.domain.web.WebServerMetricSet;
import org.bithon.agent.core.metric.domain.web.WebServerType;

import java.util.Collections;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/13 10:46 下午
 */
public class WebServerMetricCollector implements IMetricCollector {

    private final AbstractEndpoint<?> endpoint;

    public WebServerMetricCollector(AbstractEndpoint<?> endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public List<Object> collect(IMessageConverter messageConverter,
                                int interval,
                                long timestamp) {

        return Collections.singletonList(messageConverter.from(timestamp,
                                                               interval,
                                                               new WebServerMetricSet(WebServerType.TOMCAT,
                                                                                      endpoint.getConnectionCount(),
                                                                                      endpoint.getMaxConnections(),
                                                                                      endpoint.getCurrentThreadsBusy(),
                                                                                      endpoint.getMaxThreads())));
    }
}
