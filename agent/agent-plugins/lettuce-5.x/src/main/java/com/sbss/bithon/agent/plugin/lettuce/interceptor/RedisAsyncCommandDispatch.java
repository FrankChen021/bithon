package com.sbss.bithon.agent.plugin.lettuce.interceptor;

import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.IBithonObject;
import com.sbss.bithon.agent.core.utils.ReflectionUtils;
import com.sbss.bithon.agent.plugin.lettuce.LettuceAsyncContext;
import io.lettuce.core.RedisAsyncCommandsImpl;
import io.lettuce.core.api.StatefulConnection;

/**
 * @author frankchen
 */
public class RedisAsyncCommandDispatch extends AbstractInterceptor {

    @Override
    public void onMethodLeave(AopContext aopContext) {
        if (!(aopContext.getReturning() instanceof IBithonObject)) {
            return;
        }
        RedisAsyncCommandsImpl s;
        IBithonObject result = (IBithonObject) aopContext.getReturning();

        LettuceAsyncContext asyncContext = new LettuceAsyncContext();
        asyncContext.setStartTime(System.nanoTime());
        result.setInjectedObject(asyncContext);

        StatefulConnection<?, ?> connection = ((StatefulConnection<?, ?>) ReflectionUtils.getFieldValue(aopContext.getTarget(), "connection"));
        if (connection instanceof IBithonObject) {
            String endpoint = (String) ((IBithonObject) connection).getInjectedObject();
            asyncContext.setEndpoint(endpoint);
        }
    }
}
