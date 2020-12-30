package com.sbss.bithon.agent.core.transformer.matcher;

import shaded.net.bytebuddy.description.type.TypeDescription;
import shaded.net.bytebuddy.matcher.ElementMatcher;

import static shaded.net.bytebuddy.matcher.ElementMatchers.named;

/**
 * Description : 默认classMatcher <br>
 * Date: 18/3/2
 *
 * @author 马至远
 */
public class DefaultClassNameMatcher implements AgentClassMatcher {
    private String className;

    private DefaultClassNameMatcher(String className) {
        this.className = className;
    }

    public static DefaultClassNameMatcher byName(String className) {
        return new DefaultClassNameMatcher(className);
    }

    @Override
    public ElementMatcher<? super TypeDescription> getMatcher() {
        return named(className);
    }
}
