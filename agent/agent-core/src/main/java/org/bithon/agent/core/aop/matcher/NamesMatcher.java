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

package org.bithon.agent.core.aop.matcher;

import shaded.net.bytebuddy.description.NamedElement;
import shaded.net.bytebuddy.matcher.ElementMatcher;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Frank Chen
 * @date 25/12/21 10:30 PM
 */
public class NamesMatcher<T extends NamedElement> extends ElementMatcher.Junction.AbstractBase<T> {

    private final Set<String> names;

    public NamesMatcher(String... names) {
        this.names = Stream.of(names).collect(Collectors.toSet());
    }

    @Override
    public boolean matches(T target) {
        return names.contains(target.getActualName());
    }

    @Override
    public String toString() {
        return names.toString();
    }

    public Set<String> getNames() {
        return names;
    }
}
