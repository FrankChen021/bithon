package com.sbss.bithon.agent.plugin.undertow;

import com.keruyun.commons.agent.collector.entity.ServerInfoEntity;
import com.keruyun.commons.agent.collector.entity.ServerMetricEntity;
import com.keruyun.commons.agent.collector.enums.ServerBrand;
import com.sbss.bithon.agent.core.interceptor.AbstractMethodIntercepted;
import com.sbss.bithon.agent.core.interceptor.AfterJoinPoint;
import com.sbss.bithon.agent.core.transformer.plugin.AgentHandlerHolder;
import com.sbss.bithon.agent.core.util.ReflectUtil;
import com.sbss.bithon.agent.dispatcher.metrics.DispatchProcessor;
import io.undertow.Undertow;
import io.undertow.server.ConnectorStatistics;
import org.xnio.XnioWorker;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ThreadPoolExecutor;

public class WorkerHandler extends AbstractMethodIntercepted {
    private static final Logger log = LoggerFactory.getLogger(WorkerHandler.class);

    private int checkPeriod;

    private Integer port;

    private ThreadPoolExecutor taskPool;

    private ConnectorStatistics cs;

    private DispatchProcessor dispatchProcessor;

    @Override
    public boolean init() throws Exception {
        checkPeriod = 10;

        dispatchProcessor = DispatchProcessor.getInstance();

        return true;
    }

    @Override
    protected void after(AfterJoinPoint joinPoint) {
        if (null == port) {
            Undertow server = (Undertow) joinPoint.getTarget();
            List<?> listeners = (List<?>) ReflectUtil.getFieldValue(server, "listeners");
            XnioWorker worker = (XnioWorker) ReflectUtil.getFieldValue(server, "worker");
            port = Integer.parseInt(ReflectUtil.getFieldValue(listeners.iterator().next(), "port").toString());
            taskPool = (ThreadPoolExecutor) ReflectUtil.getFieldValue(worker, "taskPool");
            OpenListenerHandler openListenerHandler = (OpenListenerHandler) AgentHandlerHolder.get(OpenListenerHandler.class);
            if (null != openListenerHandler) {
                cs = openListenerHandler.getConnectorStatistics();
            }
            dispatchProcessor.setPort(port);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    dispatch();
                }
            }, 0, checkPeriod * 1000);
        }
    }

    private ServerInfoEntity buildEntity() {
        int connectionCount = null == cs ? 0 : (int) cs.getActiveConnections();
        int maxConnections = null == cs ? 0 : (int) cs.getMaxActiveConnections();
        return new ServerInfoEntity(dispatchProcessor.getAppName(),
                                    dispatchProcessor.getIpAddress(),
                                    port,
                                    System.currentTimeMillis(),
                                    checkPeriod,
                                    null,
                                    new ServerMetricEntity(connectionCount,
                                                           maxConnections,
                                                           taskPool.getActiveCount(),
                                                           taskPool.getMaximumPoolSize(),
                                                           ServerBrand.UNDERTOW.toString()));
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
