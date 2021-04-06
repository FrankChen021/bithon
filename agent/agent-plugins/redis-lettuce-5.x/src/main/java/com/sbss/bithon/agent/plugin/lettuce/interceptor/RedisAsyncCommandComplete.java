package com.sbss.bithon.agent.plugin.lettuce.interceptor;

import com.sbss.bithon.agent.boot.aop.AbstractInterceptor;
import com.sbss.bithon.agent.boot.aop.AopContext;
import com.sbss.bithon.agent.boot.aop.IBithonObject;
import com.sbss.bithon.agent.core.metric.collector.MetricCollectorManager;
import com.sbss.bithon.agent.core.metric.domain.redis.RedisMetricCollector;
import com.sbss.bithon.agent.plugin.lettuce.LettuceAsyncContext;
import io.lettuce.core.protocol.AsyncCommand;
import shaded.org.slf4j.LoggerFactory;

/**
 * @author frankchen
 */
public class RedisAsyncCommandComplete extends AbstractInterceptor {

    private RedisMetricCollector metricProvider;

    @Override
    public boolean initialize() {
        metricProvider = MetricCollectorManager.getInstance().getOrRegister("lettuce", RedisMetricCollector.class);
        return true;
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        LoggerFactory.getLogger(RedisAsyncCommandComplete.class)
                     .info("after {} AsyncCommandInterceptor", aopContext.getMethod().getName());

        if (!(aopContext.getTarget() instanceof IBithonObject)) {
            return;
        }

        LettuceAsyncContext asyncContext = (LettuceAsyncContext) ((IBithonObject) aopContext.getTarget()).getInjectedObject();

        if (asyncContext != null &&
            asyncContext.getEndpoint() != null &&
            asyncContext.getStartTime() != null) {
            AsyncCommand asyncCommand = (AsyncCommand) aopContext.getTarget();

            boolean fail = "cancel".equalsIgnoreCase(aopContext.getMethod().getName()) || asyncCommand.getOutput()
                                                                                                      .hasError();

            //TODO: read/write
            //TODO: bytes
            this.metricProvider.addWrite(asyncContext.getEndpoint(),
                                         asyncCommand.getType().name(),
                                         System.nanoTime() - (long) asyncContext.getStartTime(),
                                         fail);
        }
    }
}
