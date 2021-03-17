package com.sbss.bithon.agent.plugin.tomcat.interceptor;

import com.sbss.bithon.agent.core.context.AgentContext;
import com.sbss.bithon.agent.core.metrics.MetricProviderManager;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import com.sbss.bithon.agent.plugin.tomcat.metric.WebServerMetricProvider;
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

            MetricProviderManager.getInstance().register("webserver-tomcat", new WebServerMetricProvider(endpoint));
        }
    }
}
