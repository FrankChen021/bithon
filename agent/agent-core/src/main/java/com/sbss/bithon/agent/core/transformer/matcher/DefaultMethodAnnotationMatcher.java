package com.sbss.bithon.agent.core.transformer.matcher;

import shaded.net.bytebuddy.description.annotation.AnnotationDescription;
import shaded.net.bytebuddy.description.annotation.AnnotationList;
import shaded.net.bytebuddy.description.method.MethodDescription;
import shaded.net.bytebuddy.description.method.MethodList;
import shaded.net.bytebuddy.matcher.ElementMatcher;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import static shaded.net.bytebuddy.matcher.ElementMatchers.named;

/**
 * Description : default method annotation matcher <br>
 * Date: 18/3/7
 *
 * @author 马至远
 */
public class DefaultMethodAnnotationMatcher extends AgentAnnotationMatcher implements AgentMethodMatcher {
    private static final Logger log = LoggerFactory.getLogger(DefaultMethodAnnotationMatcher.class);

    private AgentMethodType agentMethodType;

    private DefaultMethodAnnotationMatcher(String[] annotations, AgentMethodType agentMethodType) {
        super(annotations);
        this.agentMethodType = agentMethodType == null ? AgentMethodType.NORMAL : agentMethodType;
    }

    /**
     * 根据方法的注解匹配
     *
     * @param annotationTypes 注解类型列表
     * @param agentMethodType 方法类型{@link AgentMethodMatcher.AgentMethodType}
     * @return 方法匹配器
     */
    public static AgentMethodMatcher byAnnotations(String[] annotationTypes,
                                                   AgentMethodType agentMethodType) {
        return new DefaultMethodAnnotationMatcher(annotationTypes, agentMethodType);
    }

    @Override
    public ElementMatcher<? super MethodDescription> getMatcher() {
        return checkMethod().and(super.getAnnotationMatcher());
    }

    @Override
    public AgentMethodType getMethodMatcherType() {
        return this.agentMethodType;
    }

    private ElementMatcher.Junction<MethodDescription> checkMethod() {
        return new ElementMatcher.Junction<MethodDescription>() {
            @Override
            public <U extends MethodDescription> Junction<U> and(ElementMatcher<? super U> elementMatcher) {
                return null;
            }

            @Override
            public <U extends MethodDescription> Junction<U> or(ElementMatcher<? super U> elementMatcher) {
                return null;
            }

            @Override
            public boolean matches(MethodDescription methodDescription) {
                AnnotationList annotationDescriptions = methodDescription.getDeclaredAnnotations();
                for (AnnotationDescription annotationDescription : annotationDescriptions) {
                    // find annotation class
                    MethodList<MethodDescription.InDefinedShape> methodList = annotationDescription.getAnnotationType()
                        .getDeclaredMethods();
                    MethodDescription.InDefinedShape level = methodList.filter(named("level")).getOnly();

                    log.debug(String.format("~~ annotation name=%s, values={%s}",
                                            annotationDescription.getAnnotationType().getTypeName(),
                                            annotationDescription.getValue(level).resolve(String.class)));
                }

                return true;
            }
        };
    }
}
