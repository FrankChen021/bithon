package com.sbss.bithon.agent.plugin.tomcat.metric;

import com.sbss.bithon.agent.core.context.AppInstance;
import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;
import com.sbss.bithon.agent.core.metric.IMetricProvider;
import com.sbss.bithon.agent.core.metric.web.WebServerMetric;
import com.sbss.bithon.agent.core.metric.web.WebServerType;
import org.apache.tomcat.util.net.AbstractEndpoint;

import java.util.Collections;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/13 10:46 下午
 */
public class WebServerMetricProvider implements IMetricProvider {

    private final AbstractEndpoint<?> endpoint;

    public WebServerMetricProvider(AbstractEndpoint<?> endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public List<Object> buildMessages(IMessageConverter messageConverter,
                                      AppInstance appInstance,
                                      int interval,
                                      long timestamp) {

        return Collections.singletonList(messageConverter.from(appInstance,
                                                               timestamp,
                                                               interval,
                                                               new WebServerMetric(WebServerType.TOMCAT,
                                                                                   endpoint.getConnectionCount(),
                                                                                   endpoint.getMaxConnections(),
                                                                                   endpoint.getCurrentThreadsBusy(),
                                                                                   endpoint.getMaxThreads())));
    }
}
