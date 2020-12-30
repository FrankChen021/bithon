package com.sbss.bithon.agent.core.transformer.plugin;

import com.sbss.bithon.agent.core.transformer.matcher.AgentClassMatcher;
import com.sbss.bithon.agent.core.transformer.matcher.AgentMethodMatcher;
import com.sbss.bithon.agent.core.transformer.matcher.DefaultClassNameMatcher;

/**
 * @author frankchen
 * @Date Jan 13, 2020 1:07:41 PM
 */
public class MethodPointCut implements IMethodPointCut {

    private AgentClassMatcher classMatcher;
    private AgentMethodMatcher methodMatcher;

    public static IMethodPointCut New(String classMatcher,
                                      AgentMethodMatcher methodMatcher) {
        return new MethodPointCut(classMatcher, methodMatcher);
    }

    public MethodPointCut(String classMatcher, AgentMethodMatcher methodMatcher) {
        this.classMatcher = DefaultClassNameMatcher.byName(classMatcher);
        this.methodMatcher = methodMatcher;
    }

    @Override
    public AgentClassMatcher getClassMatcher() {
        return classMatcher;
    }

    @Override
    public AgentMethodMatcher getMethodMatcher() {
        return methodMatcher;
    }

}
