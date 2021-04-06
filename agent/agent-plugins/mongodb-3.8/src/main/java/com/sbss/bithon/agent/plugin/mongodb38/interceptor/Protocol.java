package com.sbss.bithon.agent.plugin.mongodb38.interceptor;

import com.mongodb.MongoNamespace;
import com.sbss.bithon.agent.core.metric.domain.mongo.MongoCommand;
import com.sbss.bithon.agent.boot.aop.AbstractInterceptor;
import com.sbss.bithon.agent.boot.aop.AopContext;
import com.sbss.bithon.agent.boot.aop.IBithonObject;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/4/1 6:37 下午
 */
public class Protocol {

    /**
     * {@link com.mongodb.internal.connection.CommandProtocolImpl}
     */
    public static class CommandProtocol extends AbstractInterceptor {
        @Override
        public void onConstruct(AopContext aopContext) {
            IBithonObject obj = aopContext.castTargetAs();
            obj.setInjectedObject(new MongoCommand(aopContext.getArgAs(0),
                                                   MongoNamespace.COMMAND_COLLECTION_NAME,
                                                   "Command"));
        }
    }
}
