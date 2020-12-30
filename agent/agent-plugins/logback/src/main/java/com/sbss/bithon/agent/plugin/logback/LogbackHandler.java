package com.sbss.bithon.agent.plugin.logback;

import com.sbss.bithon.agent.core.interceptor.AbstractMethodIntercepted;
import com.sbss.bithon.agent.core.interceptor.AfterJoinPoint;
import com.sbss.bithon.agent.dispatcher.metrics.counter.AgentCounterRepository;
import com.sbss.bithon.agent.dispatcher.metrics.counter.IAgentCounter;

/**
 * Description : logback handler, intercept log action <br>
 * Date: 18/4/13
 *
 * @author 马至远
 */
public class LogbackHandler extends AbstractMethodIntercepted {
    private static final String LOGBACK_PLUGIN = "logback";

    /**
     * 数据记录器, 用于统计数据
     */
    private IAgentCounter counter;

    @Override
    public boolean init() throws Exception {
        // 获取CounterRepository 实例
        AgentCounterRepository counterRepository = AgentCounterRepository.getInstance();

        // 向counterRepository注册, 开始统计request信息
        counter = new LogCounter();
        counterRepository.register(LOGBACK_PLUGIN, counter);
        return true;
    }

    @Override
    protected void after(AfterJoinPoint joinPoint) {
        counter.add(joinPoint);
    }
}
