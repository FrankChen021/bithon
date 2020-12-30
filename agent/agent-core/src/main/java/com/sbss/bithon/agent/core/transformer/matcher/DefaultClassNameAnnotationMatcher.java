package com.sbss.bithon.agent.core.transformer.matcher;

import shaded.net.bytebuddy.description.type.TypeDescription;
import shaded.net.bytebuddy.matcher.ElementMatcher;

import static shaded.net.bytebuddy.matcher.ElementMatchers.named;

/**
 * Description : default class matcher by name and annotations <br>
 * Date: 18/3/6
 *
 * @author 马至远
 */
public class DefaultClassNameAnnotationMatcher extends AgentAnnotationMatcher implements AgentClassMatcher {
    private String className;

    private DefaultClassNameAnnotationMatcher(String[] annotations, String className) {
        super(annotations);
        this.className = className;
    }

    public static AgentClassMatcher byAnnotationAndName(String[] annotationTypes,
                                                        String className) {
        return new DefaultClassNameAnnotationMatcher(annotationTypes, className);
    }

    @Override
    public ElementMatcher<? super TypeDescription> getMatcher() {
        return named(this.className).and(super.getAnnotationMatcher());
    }
}
