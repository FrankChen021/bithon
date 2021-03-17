package com.sbss.bithon.agent.plugin.lettuce.interceptor;

import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.IBithonObject;
import com.sbss.bithon.agent.core.utils.HostAndPort;
import com.sbss.bithon.agent.core.utils.ReflectionUtils;
import io.lettuce.core.RedisURI;


/**
 * @author frankchen
 */
public class RedisClientConnect extends AbstractInterceptor {

    @Override
    public void onMethodLeave(AopContext aopContext) {
        if (aopContext.hasException()) {
            return;
        }

        Object connection = aopContext.getReturning();
        if (connection instanceof IBithonObject) {
            // forward endpoint to connection
            RedisURI uri = ((RedisURI) ReflectionUtils.getFieldValue(aopContext.getTarget(), "redisURI"));

            // Since RedisClient allows passing RedisURI to connect method
            // it's a little bit complex to intercept this method to keep HostAndPort on RedisClient
            // So, we always construct a HostAndPort string here
            ((IBithonObject) connection).setInjectedObject(HostAndPort.of(uri.getHost(), uri.getPort()));
        }
    }
}
