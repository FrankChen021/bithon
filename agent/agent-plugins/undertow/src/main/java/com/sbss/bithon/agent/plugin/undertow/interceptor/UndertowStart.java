package com.sbss.bithon.agent.plugin.undertow.interceptor;

import com.sbss.bithon.agent.core.context.AgentContext;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import com.sbss.bithon.agent.core.utils.ReflectionUtils;
import com.sbss.bithon.agent.plugin.undertow.metric.WebServerMetricProvider;
import io.undertow.Undertow;
import org.xnio.XnioWorker;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author frankchen
 */
public class UndertowStart extends AbstractInterceptor {

    private Integer port;

    @Override
    public void onMethodLeave(AopContext aopContext) {
        if (port == null && !aopContext.hasException()) {
            Undertow server = (Undertow) aopContext.getTarget();

            List<?> listeners = (List<?>) ReflectionUtils.getFieldValue(server, "listeners");
            XnioWorker worker = (XnioWorker) ReflectionUtils.getFieldValue(server, "worker");
            port = Integer.parseInt(ReflectionUtils.getFieldValue(listeners.iterator().next(), "port").toString());
            AgentContext.getInstance().getAppInstance().setPort(port);

            Object taskPool = ReflectionUtils.getFieldValue(worker, "taskPool");
            WebServerMetricProvider.getInstance().setThreadPool(taskPool);
        }
    }
}
