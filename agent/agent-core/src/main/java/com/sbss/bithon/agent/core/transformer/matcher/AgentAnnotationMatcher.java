package com.sbss.bithon.agent.core.transformer.matcher;

import shaded.net.bytebuddy.description.annotation.AnnotationSource;
import shaded.net.bytebuddy.matcher.ElementMatcher;

import static shaded.net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;
import static shaded.net.bytebuddy.matcher.ElementMatchers.named;

/**
 * Description : 注解matcher interface <br>
 * Date: 18/3/5
 *
 * @author 马至远
 */
abstract class AgentAnnotationMatcher {
    private String[] annotations;

    AgentAnnotationMatcher(String[] annotations) {
        this.annotations = annotations;
    }

    <T extends AnnotationSource> ElementMatcher.Junction<T> getAnnotationMatcher() {
        ElementMatcher.Junction<T> junction = null;
        for (String annotation : annotations) {
            if (null == junction) {
                junction = getOneAnnotationMatcher(annotation);
            } else {
                junction = junction.and(getAnnotationMatcher());
            }
        }

        return junction;
    }

    private <T extends AnnotationSource> ElementMatcher.Junction<T> getOneAnnotationMatcher(String annotation) {
        return isAnnotatedWith(named(annotation));
    }
}
