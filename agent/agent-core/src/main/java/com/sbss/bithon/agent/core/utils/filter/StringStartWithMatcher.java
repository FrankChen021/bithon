package com.sbss.bithon.agent.core.utils.filter;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/17 9:17 下午
 */
public class StringStartWithMatcher implements IMatcher {

    private String prefix;

    @Override
    public boolean matches(Object input) {
        return ((String) input).startsWith(prefix);
    }
}
