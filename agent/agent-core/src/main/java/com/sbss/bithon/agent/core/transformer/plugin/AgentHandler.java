package com.sbss.bithon.agent.core.transformer.plugin;

import com.sbss.bithon.agent.core.interceptor.AbstractMethodIntercepted;

/**
 * @author frankchen
 * @Date Jan 13, 2020 1:10:25 PM
 */
public class AgentHandler implements IAgentHandler {

    private String handlerClass;
    private IMethodPointCut[] pointCuts;

    public AgentHandler(Class<? extends AbstractMethodIntercepted> handlerClass) {
        this.handlerClass = handlerClass.getName();
        this.pointCuts = null;
    }

    public AgentHandler(Class<? extends AbstractMethodIntercepted> handlerClass, IMethodPointCut... pointCuts) {
        this(handlerClass.getName(), pointCuts);
    }

    public AgentHandler(String handlerClass, IMethodPointCut... pointCuts) {
        this.handlerClass = handlerClass;
        this.pointCuts = pointCuts;
    }

    @Override
    public String getHandlerClass() {
        return handlerClass;
    }

    @Override
    public IMethodPointCut[] getPointcuts() {
        return pointCuts;
    }

}
