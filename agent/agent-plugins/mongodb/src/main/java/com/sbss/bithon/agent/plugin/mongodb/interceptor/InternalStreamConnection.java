package com.sbss.bithon.agent.plugin.mongodb.interceptor;

import com.mongodb.connection.ServerId;
import com.mongodb.connection.StreamFactory;
import com.mongodb.event.ConnectionListener;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.IBithonObject;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/30 11:29 上午
 */
public class InternalStreamConnection {

    /**
     * {@link com.mongodb.connection.InternalStreamConnection#InternalStreamConnection(ServerId, StreamFactory, InternalConnectionInitializer, ConnectionListener)}
     */
    public static class Constructor extends AbstractInterceptor {
        @Override
        public void onConstruct(Object constructedObject, Object[] args) throws Exception {
            IBithonObject bithonObject = (IBithonObject) constructedObject;
            bithonObject.setInjectedObject(((ServerId)args[0]).getAddress().toString());
        }
    }
}
