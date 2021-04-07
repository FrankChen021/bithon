package com.sbss.bithon.agent.core.plugin.descriptor;

import shaded.net.bytebuddy.description.type.TypeDescription;
import shaded.net.bytebuddy.matcher.ElementMatcher;

import static shaded.net.bytebuddy.matcher.ElementMatchers.named;

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
