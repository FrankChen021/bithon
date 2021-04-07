package com.sbss.bithon.agent.core.plugin.descriptor;

import shaded.net.bytebuddy.description.type.TypeDescription;
import shaded.net.bytebuddy.matcher.ElementMatcher;

import static shaded.net.bytebuddy.matcher.ElementMatchers.named;

/**
 * @author frankchen
 * @date Jan 13, 2020 1:07:41 PM
 */
public class InterceptorDescriptorBuilder {

    private ElementMatcher.Junction<? super TypeDescription> targetClass;
    private boolean debug;
    private boolean isBootstrapClass;

    public static InterceptorDescriptorBuilder forClass(String targetClass) {
        return new InterceptorDescriptorBuilder().targetClass(targetClass);
    }

    /**
     * for classes which are loaded by bootstrap class loader
     */
    public static InterceptorDescriptorBuilder forBootstrapClass(String targetClass) {
        return new InterceptorDescriptorBuilder().targetClass(targetClass).isBootstrapClass(true);
    }

    public InterceptorDescriptor methods(MethodPointCutDescriptor... pointCuts) {
        if (debug) {
            for (MethodPointCutDescriptor pointCut : pointCuts) {
                pointCut.setDebug(debug);
            }
        }
        return new InterceptorDescriptor(debug, isBootstrapClass, targetClass, pointCuts);

    }

    public InterceptorDescriptorBuilder targetClass(String targetClass) {
        this.targetClass = named(targetClass);
        return this;
    }

    public InterceptorDescriptorBuilder debug() {
        this.debug = true;
        return this;
    }

    public InterceptorDescriptorBuilder isBootstrapClass(boolean isBootstrapClass) {
        this.isBootstrapClass = isBootstrapClass;
        return this;
    }
}
