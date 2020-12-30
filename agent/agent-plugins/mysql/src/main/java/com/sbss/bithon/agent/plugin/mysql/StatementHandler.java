package com.sbss.bithon.agent.plugin.mysql;

import com.sbss.bithon.agent.core.interceptor.AbstractMethodIntercepted;
import com.sbss.bithon.agent.core.interceptor.AfterJoinPoint;
import com.sbss.bithon.agent.core.interceptor.BeforeJoinPoint;
import com.sbss.bithon.agent.dispatcher.metrics.counter.IAgentCounter;

public class StatementHandler extends AbstractMethodIntercepted {
    private IAgentCounter counter;

    @Override
    protected Object createContext(BeforeJoinPoint joinPoint) {
        return System.nanoTime();
    }

    @Override
    public boolean init() {
        counter = MySqlCounter.getInstance();
        return true;
    }

    @Override
    protected void after(AfterJoinPoint joinPoint) {
        counter.add(joinPoint);
    }
}
