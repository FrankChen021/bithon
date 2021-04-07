package com.sbss.bithon.agent.core.plugin.descriptor;

import shaded.net.bytebuddy.description.method.MethodDescription;
import shaded.net.bytebuddy.description.type.TypeDescription;
import shaded.net.bytebuddy.matcher.ElementMatcher;

/**
 * @author frankchen
 * @date Jan 13, 2020 1:07:41 PM
 */
public class MethodPointCutDescriptor {

    private boolean debug;
    private final boolean isBootstrapClass;
    private final ElementMatcher.Junction<? super TypeDescription> classMatcher;
    private final ElementMatcher.Junction<? super MethodDescription> methodMatcher;
    private final TargetMethodType targetMethodType;
    private final String interceptor;

    public MethodPointCutDescriptor(boolean debug,
                                    boolean isBootstrapClass,
                                    ElementMatcher.Junction<? super TypeDescription> classMatcher,
                                    ElementMatcher.Junction<? super MethodDescription> methodMatcher,
                                    TargetMethodType targetMethodType) {
        this.debug = debug;
        this.isBootstrapClass = isBootstrapClass;
        this.classMatcher = classMatcher;
        this.methodMatcher = methodMatcher;
        this.targetMethodType = targetMethodType;
        this.interceptor = null;
    }

    public MethodPointCutDescriptor(boolean debug,
                                    boolean isBootstrapClass,
                                    ElementMatcher.Junction<? super TypeDescription> classMatcher,
                                    ElementMatcher.Junction<? super MethodDescription> methodMatcher,
                                    TargetMethodType targetMethodType,
                                    String interceptor) {
        this.debug = debug;
        this.isBootstrapClass = isBootstrapClass;
        this.classMatcher = classMatcher;
        this.methodMatcher = methodMatcher;
        this.targetMethodType = targetMethodType;
        this.interceptor = interceptor;
    }

    public boolean isBootstrapClass() {
        return isBootstrapClass;
    }

    public ElementMatcher.Junction<? super TypeDescription> getClassMatcher() {
        return classMatcher;
    }

    public ElementMatcher.Junction<? super MethodDescription> getMethodMatcher() {
        return methodMatcher;
    }

    public TargetMethodType getTargetMethodType() {
        return targetMethodType;
    }

    public boolean isDebug() {
        return debug;
    }

    public String getInterceptor() {
        return interceptor;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }
}
