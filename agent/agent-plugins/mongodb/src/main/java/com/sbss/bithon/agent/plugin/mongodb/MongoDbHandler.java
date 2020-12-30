package com.sbss.bithon.agent.plugin.mongodb;

import com.sbss.bithon.agent.core.interceptor.AbstractMethodIntercepted;
import com.sbss.bithon.agent.core.interceptor.AfterJoinPoint;
import com.sbss.bithon.agent.core.interceptor.BeforeJoinPoint;
import com.sbss.bithon.agent.dispatcher.metrics.counter.IAgentCounter;

/**
 * Description : mongodb plugin <br>
 * Date: 17/11/3
 *
 * @author 马至远
 */
public class MongoDbHandler extends AbstractMethodIntercepted {
    private IAgentCounter counter;

    @Override
    public boolean init() {
        counter = MongoDbCounter.getInstance();
        return true;
    }

    @Override
    public void onConstruct(Object constructedObject,
                            Object[] args) {
        AfterJoinPoint joinPoint = new AfterJoinPoint(constructedObject, null, args, null, null, null);
        counter.add(joinPoint);
    }

    @Override
    protected Object createContext(BeforeJoinPoint joinPoint) {
        return System.nanoTime();
    }

    @Override
    protected void after(AfterJoinPoint joinPoint) {
        counter.add(joinPoint);
    }
}
