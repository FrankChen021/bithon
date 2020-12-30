package com.sbss.bithon.agent.core.transformer.plugin;

import com.sbss.bithon.agent.core.transformer.matcher.AgentClassMatcher;
import com.sbss.bithon.agent.core.transformer.matcher.AgentMethodMatcher;

/**
 * 插件切点接口
 */
public interface IMethodPointCut {
    AgentClassMatcher getClassMatcher();

    AgentMethodMatcher getMethodMatcher();
}
