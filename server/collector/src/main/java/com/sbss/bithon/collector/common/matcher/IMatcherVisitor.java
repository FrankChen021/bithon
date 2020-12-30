package com.sbss.bithon.collector.common.matcher;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/31 1:54 下午
 */
public interface IMatcherVisitor<T> {
    <T> T visit(EqualMatcher matcher);

    <T> T visit(AntPathMatcher antPathMatcher);

    <T> T visit(ContainsMatcher containsMatcher);

    <T> T visit(EndwithMatcher endwithMatcher);

    <T> T visit(IContainsMatcher iContainsMatcher);

    <T> T visit(RegexMatcher regexMatcher);

    <T> T visit(StartwithMatcher startwithMatcher);
}
