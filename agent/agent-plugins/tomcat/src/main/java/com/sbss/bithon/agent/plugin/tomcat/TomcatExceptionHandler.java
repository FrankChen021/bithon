package com.sbss.bithon.agent.plugin.tomcat;

import com.sbss.bithon.agent.core.interceptor.AbstractMethodIntercepted;
import com.sbss.bithon.agent.core.interceptor.AfterJoinPoint;
import com.sbss.bithon.agent.dispatcher.metrics.counter.AgentCounterRepository;
import com.sbss.bithon.agent.dispatcher.metrics.counter.IAgentCounter;

/**
 * Description : tomcat异常捕获器, 是tomcat执行过程中的service所抛出的exception,
 * 不是tomcat本身的exception<br>
 * </br>
 *
 * <br>
 * Date: 17/12/13
 *
 * @author 马至远
 */
public class TomcatExceptionHandler extends AbstractMethodIntercepted {
    private static final String TOMCAT_EXCEPTION_COUNTER_NAME = "tomcat-exception";

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
        counterRepository.register(TOMCAT_EXCEPTION_COUNTER_NAME, exceptionCounter);

        return true;
    }

    @Override
    protected void after(AfterJoinPoint joinPoint) {
        exceptionCounter.add(joinPoint);
    }
}
