/*
 *    Copyright 2020 bithon.org
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.bithon.server.commons.matcher;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/31 1:54 下午
 */
public interface IMatcherVisitor<T> {
    T visit(EqualMatcher matcher);

    T visit(NotEqualMatcher matcher);

    T visit(StringAntPathMatcher matcher);

    T visit(StringContainsMatcher matcher);

    T visit(StringEndWithMatcher matcher);

    T visit(StringIContainsMatcher matcher);

    T visit(StringRegexMatcher matcher);

    T visit(StringStartsWithMatcher matcher);

    T visit(BetweenMatcher matcher);

    T visit(InMatcher inMatcher);

    T visit(GreaterThanMatcher matcher);

    T visit(GreaterThanOrEqualMatcher matcher);

    T visit(LessThanMatcher matcher);

    T visit(LessThanOrEqualMatcher matcher);

    T visit(StringLikeMatcher matcher);

    T visit(NotMatcher matcher);
}
