package com.sbss.bithon.agent.plugin.tomcat;

import com.keruyun.commons.agent.collector.entity.ServerInfoEntity;
import com.keruyun.commons.agent.collector.entity.ServerMetricEntity;
import com.keruyun.commons.agent.collector.enums.ServerBrand;
import com.sbss.bithon.agent.core.interceptor.AbstractMethodIntercepted;
import com.sbss.bithon.agent.core.interceptor.AfterJoinPoint;
import com.sbss.bithon.agent.dispatcher.metrics.DispatchProcessor;
import org.apache.tomcat.util.net.AbstractEndpoint;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.util.Timer;
import java.util.TimerTask;

public class EndpointHandler extends AbstractMethodIntercepted {
    private static final Logger log = LoggerFactory.getLogger(EndpointHandler.class);

    private int checkPeriod;

    private AbstractEndpoint<?> endpoint;

    private DispatchProcessor dispatchProcessor;

    @Override
    public boolean init() throws Exception {
        checkPeriod = 10;
        dispatchProcessor = DispatchProcessor.getInstance();

        return true;
    }

    @Override
    protected void after(AfterJoinPoint joinPoint) {
        if (null == endpoint) {
            endpoint = (AbstractEndpoint<?>) joinPoint.getTarget();
            dispatchProcessor.setPort(endpoint.getPort());
//            DispatchTraceProcessor.getInstance().setPort(endpoint.getPort());
            log.info("Update DispatchProcessor local port to {}, DispatchProcessor will soon be at work",
                     endpoint.getPort());

            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    dispatch(endpoint);
                }
            }, 0, checkPeriod * 1000);
        }
    }

    private ServerInfoEntity buildEntity(AbstractEndpoint<?> ep) {
        return new ServerInfoEntity(dispatchProcessor.getAppName(),
                                    dispatchProcessor.getIpAddress(),
                                    ep.getPort(),
                                    System.currentTimeMillis(),
                                    checkPeriod,
                                    null,
                                    new ServerMetricEntity((int) ep.getConnectionCount(),
                                                           ep.getMaxConnections(),
                                                           ep.getCurrentThreadsBusy(),
                                                           ep.getMaxThreads(),
                                                           ServerBrand.TOMCAT.toString()));
    }

    private void dispatch(AbstractEndpoint<?> ep) {
        try {
            if (dispatchProcessor.ready) {
                dispatchProcessor.pushMessage(buildEntity(ep));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
