package com.sbss.bithon.agent.plugin.jetty;

import com.keruyun.commons.agent.collector.entity.ServerInfoEntity;
import com.keruyun.commons.agent.collector.entity.ServerMetricEntity;
import com.keruyun.commons.agent.collector.enums.ServerBrand;
import com.sbss.bithon.agent.core.interceptor.AbstractMethodIntercepted;
import com.sbss.bithon.agent.core.interceptor.AfterJoinPoint;
import com.sbss.bithon.agent.core.transformer.plugin.AgentHandlerHolder;
import com.sbss.bithon.agent.dispatcher.metrics.DispatchProcessor;
import org.eclipse.jetty.server.AbstractNetworkConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.util.Timer;
import java.util.TimerTask;

public class ConnectorHandler extends AbstractMethodIntercepted {
    private static final Logger log = LoggerFactory.getLogger(ConnectorHandler.class);

    private int checkPeriod;

    private AbstractNetworkConnector connector;

    private QueuedThreadPool threadPool;

    private DispatchProcessor dispatchProcessor;

    @Override
    public boolean init() throws Exception {
        checkPeriod = 10;
        dispatchProcessor = DispatchProcessor.getInstance();
        return true;
    }

    @Override
    protected void after(AfterJoinPoint joinPoint) {
        if (null == connector) {
            connector = (AbstractNetworkConnector) joinPoint.getTarget();
            ThreadPoolHandler threadPoolHandler = (ThreadPoolHandler) AgentHandlerHolder.get(ThreadPoolHandler.class);
            if (null != threadPoolHandler) {
                threadPool = threadPoolHandler.getThreadPool();
            }
            dispatchProcessor.setPort(connector.getPort());
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    dispatch();
                }
            }, 0, checkPeriod * 1000);
        }
    }

    private ServerInfoEntity buildEntity() {
        return new ServerInfoEntity(dispatchProcessor.getAppName(),
                                    dispatchProcessor.getIpAddress(),
                                    connector.getPort(),
                                    System.currentTimeMillis(),
                                    checkPeriod,
                                    null,
                                    new ServerMetricEntity(connector.getConnectedEndPoints().size(),
                                                           connector.getAcceptors(),
                                                           threadPool.getBusyThreads(),
                                                           threadPool.getMaxThreads(),
                                                           ServerBrand.JETTY.toString()));
    }

    private void dispatch() {
        try {
            if (dispatchProcessor.ready) {
                dispatchProcessor.pushMessage(buildEntity());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
