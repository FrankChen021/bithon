package com.sbss.bithon.agent.plugin.undertow.interceptor;

import com.sbss.bithon.agent.boot.aop.AbstractInterceptor;
import com.sbss.bithon.agent.boot.aop.AopContext;
import com.sbss.bithon.agent.plugin.undertow.metric.WebServerMetricCollector;
import io.undertow.UndertowOptions;
import io.undertow.server.protocol.http.HttpOpenListener;
import org.xnio.OptionMap;

/**
 * @author frankchen
 */
public class HttpOpenListenerSetRootHandler extends AbstractInterceptor {

    @Override
    public void onMethodLeave(AopContext aopContext) {
        HttpOpenListener openListener = aopContext.castTargetAs();
        openListener.setUndertowOptions(OptionMap.builder().addAll(openListener.getUndertowOptions())
                                            .set(UndertowOptions.ENABLE_CONNECTOR_STATISTICS, true)
                                            .getMap());
        WebServerMetricCollector.getInstance().setConnectorStatistics(openListener.getConnectorStatistics());
    }
}
