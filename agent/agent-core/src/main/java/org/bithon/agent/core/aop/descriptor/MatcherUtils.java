/*
 *    Copyright 2020 bithon.cn
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.bithon.agent.core.aop.descriptor;

import shaded.net.bytebuddy.description.NamedElement;
import shaded.net.bytebuddy.description.annotation.AnnotationDescription;
import shaded.net.bytebuddy.description.annotation.AnnotationList;
import shaded.net.bytebuddy.description.annotation.AnnotationSource;
import shaded.net.bytebuddy.description.method.MethodDescription;
import shaded.net.bytebuddy.description.method.ParameterList;
import shaded.net.bytebuddy.matcher.ElementMatcher;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/20 9:30 下午
 */
public class MatcherUtils {
    private static final Logger log = LoggerFactory.getLogger(MatcherUtils.class);

    public static <T extends NamedElement> ElementMatcher.Junction<T> named(String typeName) {
        return new ElementMatcher.Junction.AbstractBase<T>() {
            @Override
            public boolean matches(T target) {
                return target.getActualName().equals(typeName);
            }

            @Override
            public String toString() {
                return typeName;
            }
        };
    }

    public static <T extends MethodDescription> ElementMatcher.Junction<T> takesArguments(int size) {
        return new ElementMatcher.Junction.AbstractBase<T>() {

            @Override
            public boolean matches(T target) {
                return target.getParameters().size() == size;
            }

            @Override
            public String toString() {
                return "argSize=" + size;
            }
        };
    }

    public static <T extends MethodDescription> ElementMatcher.Junction<T> takesArgument(int index, String typeName) {
        return new ElementMatcher.Junction.AbstractBase<T>() {
            @Override
            public boolean matches(T target) {
                ParameterList<?> parameters = target.getParameters();
                if (index < parameters.size()) {
                    return typeName.equals(parameters.get(index).getType().asErasure().getName());
                }
                return false;
            }

            @Override
            public String toString() {
                return String.format("(arg%d is %s)", index, typeName);
            }
        };
    }

    public static <T extends MethodDescription> ElementMatcher.Junction<T> takesFirstArgument(String typeName) {
        return takesArgument(0, typeName);
    }

    public static <T extends MethodDescription> ElementMatcher.Junction<T> takesLastArgument(String typeName) {
        return new ElementMatcher.Junction.AbstractBase<T>() {
            @Override
            public boolean matches(T target) {
                ParameterList<?> parameters = target.getParameters();
                int lastIndex = parameters.size() - 1;
                if (lastIndex < 0) {
                    return false;
                } else {
                    return typeName.equals(parameters.get(lastIndex).getType().asErasure().getName());
                }
            }

            @Override
            public String toString() {
                return String.format("(lastArg is %s)", typeName);
            }
        };
    }

    public static <T extends AnnotationSource> ElementMatcher.Junction<T> isAnnotatedWith(String... annotations) {
        final Set<String> annotationSet = new HashSet<>(Arrays.asList(annotations));
        return new ElementMatcher.Junction.AbstractBase<T>() {
            @Override
            public boolean matches(T target) {
                AnnotationList annotationList = target.getDeclaredAnnotations();
                for (AnnotationDescription annotationDescription : annotationList) {
                    if (!annotationSet.contains(annotationDescription.getAnnotationType().asErasure().getName())) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public String toString() {
                return String.format("(annotations: %s)", annotationSet);
            }
        };
    }

    public static ElementMatcher.Junction<MethodDescription> debuggableMatcher(boolean debug,
                                                                               ElementMatcher.Junction<MethodDescription> matcher) {
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

            @Override
            public String toString() {
                return matcher.toString();
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

            @Override
            public String toString() {
                StringBuilder sb = new StringBuilder("args=[");
                for (String arg : args) {
                    sb.append(arg);
                    sb.append(',');
                }
                sb.delete(sb.length() - 1, sb.length());
                sb.append("]");
                return sb.toString();
            }
        };
    }
}
