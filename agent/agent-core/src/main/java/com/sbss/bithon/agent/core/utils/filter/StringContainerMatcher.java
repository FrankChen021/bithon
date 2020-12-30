package com.sbss.bithon.agent.core.utils.filter;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/17 9:18 下午
 */
public class StringContainerMatcher implements IMatcher {

    private String pattern;

    @Override
    public boolean matches(Object input) {
        return ((String) input).contains(pattern);
    }
}
