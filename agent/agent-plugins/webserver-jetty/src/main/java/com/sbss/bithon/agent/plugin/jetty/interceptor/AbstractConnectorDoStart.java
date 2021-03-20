package com.sbss.bithon.agent.plugin.jetty.interceptor;

import com.sbss.bithon.agent.core.context.AgentContext;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import com.sbss.bithon.agent.plugin.jetty.metric.WebServerMetricCollector;
import org.eclipse.jetty.server.AbstractNetworkConnector;

/**
 * @author frankchen
 */
public class AbstractConnectorDoStart extends AbstractInterceptor {

    @Override
    public void onMethodLeave(AopContext context) {
        AbstractNetworkConnector connector = (AbstractNetworkConnector) context.getTarget();

        AgentContext.getInstance().getAppInstance().setPort(connector.getPort());

        WebServerMetricCollector.getInstance().setConnector(connector);
    }
}
