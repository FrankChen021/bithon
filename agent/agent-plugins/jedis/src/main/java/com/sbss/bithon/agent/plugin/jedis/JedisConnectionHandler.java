package com.sbss.bithon.agent.plugin.jedis;

import com.sbss.bithon.agent.core.interceptor.AbstractMethodIntercepted;
import com.sbss.bithon.agent.core.interceptor.AfterJoinPoint;

/**
 * Description : jedis connection handler <br>
 * Date: 18/4/3
 *
 * @author 马至远
 */
public class JedisConnectionHandler extends AbstractMethodIntercepted {
    private RedisCounter counter;

    @Override
    public boolean init() {
        counter = RedisCounter.getInstance();
        return true;
    }

    @Override
    protected void after(AfterJoinPoint joinPoint) {
        counter.parseConnection(joinPoint);
    }
}
