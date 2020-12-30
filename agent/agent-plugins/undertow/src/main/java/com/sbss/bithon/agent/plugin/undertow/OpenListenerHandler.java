package com.sbss.bithon.agent.plugin.undertow;

import com.sbss.bithon.agent.core.interceptor.AbstractMethodIntercepted;
import com.sbss.bithon.agent.core.interceptor.AfterJoinPoint;
import io.undertow.server.ConnectorStatistics;
import io.undertow.server.protocol.http.HttpOpenListener;

public class OpenListenerHandler extends AbstractMethodIntercepted {

    private boolean processed = false;

    private ConnectorStatistics connectorStatistics;

    @Override
    public boolean init() throws Exception {
        return true;
    }

    @Override
    protected void after(AfterJoinPoint joinPoint) {
        if (!processed) {
            HttpOpenListener listener = (HttpOpenListener) joinPoint.getTarget();
            connectorStatistics = listener.getConnectorStatistics();
            processed = true;
        }
    }

    ConnectorStatistics getConnectorStatistics() {
        return connectorStatistics;
    }

}
