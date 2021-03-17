package com.sbss.bithon.agent.core.plugin.descriptor;

import shaded.net.bytebuddy.description.type.TypeDescription;
import shaded.net.bytebuddy.matcher.ElementMatcher;

/**
 * Class-oriented descriptor
 *
 * @author frank.chen021@outlook.com
 * @date 2021/2/21 12:50 上午
 */
public class InterceptorDescriptor {

    private final boolean debug;
    private final boolean isBootstrapClass;
    private final ElementMatcher.Junction<? super TypeDescription> classMatcher;

    private final MethodPointCutDescriptor[] methodPointCutDescriptors;

    public InterceptorDescriptor(boolean debug,
                                 boolean isBootstrapClass,
                                 ElementMatcher.Junction<? super TypeDescription> classMatcher,
                                 MethodPointCutDescriptor[] methodPointCutDescriptors) {
        this.debug = debug;
        this.isBootstrapClass = isBootstrapClass;
        this.classMatcher = classMatcher;
        this.methodPointCutDescriptors = methodPointCutDescriptors;
    }

    public boolean isBootstrapClass() {
        return isBootstrapClass;
    }

    public ElementMatcher.Junction<? super TypeDescription> getClassMatcher() {
        return classMatcher;
    }

    public MethodPointCutDescriptor[] getMethodPointCutDescriptors() {
        return methodPointCutDescriptors;
    }

    public boolean isDebug() {
        return debug;
    }
}
