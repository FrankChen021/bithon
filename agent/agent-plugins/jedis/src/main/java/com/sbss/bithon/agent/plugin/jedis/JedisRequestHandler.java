package com.sbss.bithon.agent.plugin.jedis;

import com.sbss.bithon.agent.core.interceptor.AbstractMethodIntercepted;
import com.sbss.bithon.agent.core.interceptor.AfterJoinPoint;
import com.sbss.bithon.agent.core.interceptor.BeforeJoinPoint;
import redis.clients.jedis.Client;

/**
 * Description : Jedis plugin <br>
 * Date: 17/11/1
 *
 * @author 马至远
 */
public class JedisRequestHandler extends AbstractMethodIntercepted {
    private RedisCounter counter;

    @Override
    public boolean init() throws Exception {
        counter = RedisCounter.getInstance();
        return true;
    }

    @Override
    protected Object createContext(BeforeJoinPoint joinPoint) {
        return (Long) System.nanoTime();
    }

    @Override
    protected void after(AfterJoinPoint joinPoint) {
        Client redisClient = (Client) joinPoint.getTarget();

        //
        // 注入了sendCommand和readProtocolWithCheckingBroken两个方法
        // sendCommand有参数
        //
        boolean isWrite = joinPoint.getArgs().length > 0;

        counter.countRequestNum(redisClient, joinPoint.getException() != null, (Long) joinPoint.getContext(), isWrite);
    }
}
