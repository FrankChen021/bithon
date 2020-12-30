package com.sbss.bithon.agent.core.transformer.matcher;

import shaded.net.bytebuddy.description.type.TypeDescription;
import shaded.net.bytebuddy.matcher.ElementMatcher;

/**
 * Description : agent class matcher <br>
 * Date: 18/3/2
 *
 * @author 马至远
 */
public interface AgentClassMatcher {
    ElementMatcher<? super TypeDescription> getMatcher();

    // TODO 考虑这里加一个classType方法, 用于主动区分class是否由bootstrapClassloader加载
}
