package com.sbss.bithon.agent.core.transformer.plugin;

/**
 * agent插件接口
 *
 * @author lizheng
 * @author mazy modified
 */
public interface IAgentHandler {
    String getHandlerClass();

    IMethodPointCut[] getPointcuts();
}
