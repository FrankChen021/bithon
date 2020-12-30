package com.sbss.bithon.agent.plugin.undertow;

import com.sbss.bithon.agent.core.interceptor.AbstractMethodIntercepted;
import com.sbss.bithon.agent.core.interceptor.AfterJoinPoint;
import com.sbss.bithon.agent.dispatcher.metrics.counter.AgentCounterRepository;
import com.sbss.bithon.agent.dispatcher.metrics.counter.IAgentCounter;

/**
 * Description : undertow 异常 handler <br>
 * Date: 18/3/7
 *
 * @author 马至远
 */
public class UndertowErrorHandler extends AbstractMethodIntercepted {
    private static final String UNDERTOW_ERROR_COUNTER_NAME = "undertow-exception";

    /**
     * 数据记录器, 用于统计数据
     */
    private IAgentCounter exceptionCounter;

    @Override
    public boolean init() throws Exception {
        // 获取CounterRepository 实例
        AgentCounterRepository counterRepository = AgentCounterRepository.getInstance();

        // 向counterRepository注册, 开始统计request信息
        exceptionCounter = new ExceptionCounter();
        counterRepository.register(UNDERTOW_ERROR_COUNTER_NAME, exceptionCounter);

        return true;
    }

    @Override
    protected void after(AfterJoinPoint joinPoint) {
        exceptionCounter.add(joinPoint);
    }
}
