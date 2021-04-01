package com.sbss.bithon.agent.core.plugin.descriptor;

import shaded.net.bytebuddy.description.method.MethodDescription;
import shaded.net.bytebuddy.matcher.ElementMatcher;
import shaded.net.bytebuddy.matcher.ElementMatchers;

import static shaded.net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static shaded.net.bytebuddy.matcher.ElementMatchers.named;
import static shaded.net.bytebuddy.matcher.ElementMatchers.takesNoArguments;

/**
 * @author frankchen
 * @date Jan 13, 2020 1:07:41 PM
 */
public class MethodPointCutDescriptorBuilder {

    private TargetMethodType targetMethodType;
    private ElementMatcher.Junction<MethodDescription> method;
    private ElementMatcher<MethodDescription> argsMatcher;
    private boolean debug;

    public static MethodPointCutDescriptorBuilder build() {
        return new MethodPointCutDescriptorBuilder();
    }

    public MethodPointCutDescriptor to(String interceptorQualifiedClassName) {

        ElementMatcher.Junction<? super MethodDescription> m = MatcherUtils.debuggableMatcher(debug, method);
        if (argsMatcher != null) {
            m = m.and(argsMatcher);
        }
        return new MethodPointCutDescriptor(debug,
                                            true,
                                            null,
                                            m,
                                            targetMethodType,
                                            interceptorQualifiedClassName);
    }

    public MethodPointCutDescriptorBuilder onAllMethods(String method) {
        this.method = named(method);
        this.targetMethodType = TargetMethodType.INSTANCE_METHOD;
        return this;
    }

    public MethodPointCutDescriptorBuilder onMethodAndArgs(String method, String... args) {
        this.method = named(method);
        this.argsMatcher = MatcherUtils.createArgumentsMatcher(debug, args);
        this.targetMethodType = TargetMethodType.INSTANCE_METHOD;
        return this;
    }

    public MethodPointCutDescriptorBuilder onMethodAndNoArgs(String method) {
        this.method = named(method);
        this.argsMatcher = takesNoArguments();
        this.targetMethodType = TargetMethodType.INSTANCE_METHOD;
        return this;
    }

    public MethodPointCutDescriptorBuilder onMethod(ElementMatcher.Junction<MethodDescription> method) {
        this.method = method;
        this.targetMethodType = TargetMethodType.INSTANCE_METHOD;
        return this;
    }

    public MethodPointCutDescriptorBuilder onAllConstructor() {
        this.method = ElementMatchers.isConstructor();
        this.targetMethodType = TargetMethodType.CONSTRUCTOR;
        return this;
    }

    public MethodPointCutDescriptorBuilder onConstructor(ElementMatcher.Junction<MethodDescription> matcher) {
        this.method = isConstructor().and(matcher);
        this.targetMethodType = TargetMethodType.CONSTRUCTOR;
        return this;
    }

    public MethodPointCutDescriptorBuilder onConstructor(String... args) {
        if (args == null) {
            throw new IllegalArgumentException("args should not be null");
        }
        this.method = ElementMatchers.isConstructor();
        this.argsMatcher = MatcherUtils.createArgumentsMatcher(debug, args);
        this.targetMethodType = TargetMethodType.CONSTRUCTOR;
        return this;
    }

    public MethodPointCutDescriptorBuilder onDefaultConstructor() {
        this.method = ElementMatchers.isDefaultConstructor();
        this.targetMethodType = TargetMethodType.CONSTRUCTOR;
        return this;
    }

    public MethodPointCutDescriptorBuilder onArgs(String... args) {
        this.argsMatcher = MatcherUtils.createArgumentsMatcher(debug, args);
        return this;
    }

    public MethodPointCutDescriptorBuilder noArgs() {
        argsMatcher = takesNoArguments();
        return this;
    }

    public MethodPointCutDescriptorBuilder debug() {
        this.debug = true;
        return this;
    }
}
