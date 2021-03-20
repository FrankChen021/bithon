package com.sbss.bithon.server.common.matcher;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/31 1:54 下午
 */
public interface IMatcherVisitor<T> {
    T visit(EqualMatcher matcher);

    T visit(AntPathMatcher antPathMatcher);

    T visit(ContainsMatcher containsMatcher);

    T visit(EndwithMatcher endwithMatcher);

    T visit(IContainsMatcher iContainsMatcher);

    T visit(RegexMatcher regexMatcher);

    T visit(StartwithMatcher startwithMatcher);
}
