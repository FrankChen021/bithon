package com.sbss.bithon.agent.plugin.mysql;

import com.sbss.bithon.agent.core.interceptor.AbstractMethodIntercepted;
import com.sbss.bithon.agent.core.interceptor.AfterJoinPoint;
import com.sbss.bithon.agent.core.interceptor.BeforeJoinPoint;
import com.sbss.bithon.agent.dispatcher.metrics.counter.IAgentCounter;

/**
 * Description : <br>
 * Date: 18/5/8
 *
 * @author 马至远
 */
public class PreparedStatementHandler extends AbstractMethodIntercepted {
    private IAgentCounter counter;
    private IAgentCounter sqlCounter;

    @Override
    protected Object createContext(BeforeJoinPoint joinPoint) {
        return (Long) System.nanoTime();
    }

    @Override
    public boolean init() {
        counter = MySqlCounter.getInstance();
        sqlCounter = MySqlStatsCounter.getInstance();
        return true;
    }

    @Override
    protected void after(AfterJoinPoint joinPoint) {
        counter.add(joinPoint);
        sqlCounter.add(joinPoint);
    }
}
