package com.sbss.bithon.agent.core.transformer.matcher;

import shaded.net.bytebuddy.description.type.TypeDescription;
import shaded.net.bytebuddy.matcher.ElementMatcher;

/**
 * Description : default agent class annotation matcher <br>
 * Date: 18/3/5
 *
 * @author 马至远
 */
public class DefaultClassAnnotationMatcher extends AgentAnnotationMatcher implements AgentClassMatcher {
    private DefaultClassAnnotationMatcher(String[] annotations) {
        super(annotations);
    }

    public static DefaultClassAnnotationMatcher byAnnotation(String[] annotationsTypes) {
        return new DefaultClassAnnotationMatcher(annotationsTypes);
    }

    @Override
    public ElementMatcher<? super TypeDescription> getMatcher() {
        return super.getAnnotationMatcher();
    }
}
