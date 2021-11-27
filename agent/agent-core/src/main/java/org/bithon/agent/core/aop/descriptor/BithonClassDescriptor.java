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

package org.bithon.agent.core.aop.descriptor;

import shaded.net.bytebuddy.description.type.TypeDescription;
import shaded.net.bytebuddy.matcher.ElementMatcher;

import static shaded.net.bytebuddy.matcher.ElementMatchers.named;
import static shaded.net.bytebuddy.matcher.ElementMatchers.namedOneOf;

/**
 * Defines which class should be transformed into IBithonObject subclasses
 *
 * @author frank.chen021@outlook.com
 * @date 2021/3/27 19:59
 */
public class BithonClassDescriptor {
    private final ElementMatcher.Junction<? super TypeDescription> classMatcher;
    private final boolean debug;

    private BithonClassDescriptor(ElementMatcher.Junction<? super TypeDescription> classMatcher, boolean debug) {
        this.classMatcher = classMatcher;
        this.debug = debug;
    }

    public ElementMatcher.Junction<? super TypeDescription> getClassMatcher() {
        return classMatcher;
    }

    public boolean isDebug() {
        return debug;
    }

    /**
     * NOTE: For multiple class, {@link #of(ElementMatcher.Junction)} should be used where argument is call of {@link shaded.net.bytebuddy.matcher.ElementMatchers#namedOneOf(String...)}
     */
    public static BithonClassDescriptor of(String clazz) {
        return new BithonClassDescriptor(named(clazz), false);
    }

    public static BithonClassDescriptor of(String... clazz) {
        return new BithonClassDescriptor(namedOneOf(clazz), false);
    }

    public static BithonClassDescriptor of(String clazz, boolean debug) {
        return new BithonClassDescriptor(named(clazz), debug);
    }

    public static BithonClassDescriptor of(ElementMatcher.Junction<? super TypeDescription> classMatcher) {
        return new BithonClassDescriptor(classMatcher, false);
    }

    public static BithonClassDescriptor of(ElementMatcher.Junction<? super TypeDescription> classMatcher,
                                           boolean debug) {
        return new BithonClassDescriptor(classMatcher, debug);
    }
}
