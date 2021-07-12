package com.sbss.bithon.agent.core.utils.filter;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/7/12 7:55 下午
 */
public class MatcherFactory {
    public static IMatcher create(String type, String pattern) {
        switch (type) {
            case StringPrefixMatcher.TYPE:
                return new StringPrefixMatcher(pattern);
            case StringSuffixMatcher.TYPE:
                return new StringSuffixMatcher(pattern);
            case StringContainsMatcher.TYPE:
                return new StringContainsMatcher(pattern);
            default:
                return null;
        }
    }
}
