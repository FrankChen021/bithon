package com.sbss.bithon.agent.core.plugin.descriptor;

import shaded.net.bytebuddy.description.annotation.AnnotationSource;
import shaded.net.bytebuddy.description.method.MethodDescription;
import shaded.net.bytebuddy.description.method.ParameterDescription;
import shaded.net.bytebuddy.description.method.ParameterList;
import shaded.net.bytebuddy.matcher.ElementMatcher;
import shaded.net.bytebuddy.matcher.ElementMatchers;
import shaded.net.bytebuddy.matcher.MethodParametersMatcher;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import static shaded.net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;
import static shaded.net.bytebuddy.matcher.ElementMatchers.named;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/20 9:30 下午
 */
public class MatcherUtils {
    private static final Logger log = LoggerFactory.getLogger(MatcherUtils.class);

    public static MethodParametersMatcher takesArgument(int index, String typeName) {
        return new MethodParametersMatcher((ElementMatcher<ParameterList<? extends ParameterDescription>>) parameters -> {
            if (index < parameters.size()) {
                return typeName.equals(parameters.get(index).getType().asErasure().getName());
            }
            return false;
        });
    }

    public static MethodParametersMatcher takesFirstArgument(String typeName) {
        return new MethodParametersMatcher((ElementMatcher<ParameterList<? extends ParameterDescription>>) parameters -> {
            int lastIndex = parameters.size() - 1;
            if ( lastIndex < 0 ) {
                return false;
            } else {
                return typeName.equals(parameters.get(lastIndex).getType().asErasure().getName());
            }
        });
    }

    public static MethodParametersMatcher takesLastArgument(String typeName) {
        return new MethodParametersMatcher((ElementMatcher<ParameterList<? extends ParameterDescription>>) parameters -> {
            int lastIndex = parameters.size() - 1;
            if ( lastIndex < 0 ) {
                return false;
            } else {
                return typeName.equals(parameters.get(lastIndex).getType().asErasure().getName());
            }
        });
    }

    public static <T extends AnnotationSource> ElementMatcher.Junction<T> createAnnotationMatchers(String... annotations) {
        ElementMatcher.Junction<T> junction = ElementMatchers.isAnnotatedWith(named(annotations[0]));
        for (int i = 1; i < annotations.length; i++) {
            junction = junction.and(isAnnotatedWith(named(annotations[i])));
        }
        return junction;
    }

    public static ElementMatcher.Junction<MethodDescription> debuggableMatcher(boolean debug, ElementMatcher.Junction<MethodDescription> matcher) {
        if (!debug) {
            return matcher;
        }

        return new ElementMatcher.Junction<MethodDescription>() {
            @Override
            public <U extends MethodDescription> Junction<U> and(ElementMatcher<? super U> elementMatcher) {
                return new Conjunction<>(this, elementMatcher);
            }

            @Override
            public <U extends MethodDescription> Junction<U> or(ElementMatcher<? super U> elementMatcher) {
                return new Disjunction<>(this, elementMatcher);
            }

            @Override
            public boolean matches(MethodDescription methodDescription) {
                boolean matched = matcher.matches(methodDescription);
                log.info("[{}] matched to [{}]: {}", methodDescription, matcher, matched);
                return matched;
            }
        };
    }

    public static ElementMatcher<MethodDescription> createArgumentsMatcher(boolean debug, String... args) {
        return new ElementMatcher.Junction<MethodDescription>() {
            @Override
            public <U extends MethodDescription> Junction<U> and(ElementMatcher<? super U> elementMatcher) {
                return new Conjunction<>(this, elementMatcher);
            }

            @Override
            public <U extends MethodDescription> Junction<U> or(ElementMatcher<? super U> elementMatcher) {
                return new Disjunction<>(this, elementMatcher);
            }

            @Override
            public boolean matches(MethodDescription methodDescription) {
                ParameterList<?> parameterList = methodDescription.getParameters();
                if (parameterList.size() != args.length) {
                    if (debug) {
                        log.info("matching [{}]: argument size not match", methodDescription);
                    }
                    return false;
                } else {
                    for (int i = 0; i < args.length; i++) {
                        String paramType = parameterList.get(i).getType().getTypeName();
                        if (!paramType.equals(args[i])) {
                            if (debug) {
                                log.info("matching [{}]: type of parameter {} not match. Given is {}, actual is {}",
                                        methodDescription,
                                        i,
                                        args[i],
                                        paramType);
                            }
                            return false;
                        }
                    }
                    if (debug) {
                        log.info("matching [{}]: Matched", methodDescription);
                    }
                    return true;
                }
            }
        };
    }
}
