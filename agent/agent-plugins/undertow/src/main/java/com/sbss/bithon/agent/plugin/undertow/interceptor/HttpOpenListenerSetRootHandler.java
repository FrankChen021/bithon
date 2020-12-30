package com.sbss.bithon.agent.plugin.undertow.interceptor;

import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import com.sbss.bithon.agent.plugin.undertow.metric.WebServerMetricProvider;
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
        WebServerMetricProvider.getInstance().setConnectorStatistics(openListener.getConnectorStatistics());
    }
}
