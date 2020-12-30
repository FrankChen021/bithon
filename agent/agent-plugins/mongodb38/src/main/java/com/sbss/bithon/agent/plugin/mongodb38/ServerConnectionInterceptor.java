package com.sbss.bithon.agent.plugin.mongodb38;

import com.mongodb.internal.connection.DefaultServerConnection;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;

/**
 * @author frankchen
 */
public class ServerConnectionInterceptor extends AbstractInterceptor {
    private MongoMetricProvider counter;

    @Override
    public boolean initialize() {
        counter = MongoMetricProvider.getInstance();
        return true;
    }

    @Override
    public void onMethodLeave(AopContext context) {
        DefaultServerConnection connection = (DefaultServerConnection) context.getTarget();
        String hostPort = connection.getDescription().getServerAddress().toString();
        int exceptionCount = null == context.getException() ? 0 : 1;
        counter.recordRequestInfo(connection.getDescription().getConnectionId().toString(),
                                  hostPort,
                                  context.getCostTime(),
                                  exceptionCount);
    }
}
