/*
 *    Copyright 2020 bithon.org
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

package org.bithon.agent.instrumentation.aop.interceptor.matcher;

import org.bithon.agent.instrumentation.logging.ILogger;
import org.bithon.agent.instrumentation.logging.LoggerFactory;
import org.bithon.shaded.net.bytebuddy.description.NamedElement;
import org.bithon.shaded.net.bytebuddy.description.annotation.AnnotationDescription;
import org.bithon.shaded.net.bytebuddy.description.annotation.AnnotationList;
import org.bithon.shaded.net.bytebuddy.description.annotation.AnnotationSource;
import org.bithon.shaded.net.bytebuddy.description.method.MethodDescription;
import org.bithon.shaded.net.bytebuddy.description.method.ParameterList;
import org.bithon.shaded.net.bytebuddy.description.modifier.Visibility;
import org.bithon.shaded.net.bytebuddy.matcher.ElementMatcher;
import org.bithon.shaded.net.bytebuddy.matcher.ElementMatchers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/20 9:30 下午
 */
public class Matchers {
    private static final ILogger log = LoggerFactory.getLogger(Matchers.class);

    public static <T extends NamedElement> ElementMatcher.Junction<T> endsWith(String name) {
        return new ElementMatcher.Junction.AbstractBase<T>() {
            @Override
            public boolean matches(T target) {
                return target.getActualName().endsWith(name);
            }

            @Override
            public String toString() {
                return "endsWith('" + name + "')";
            }
        };
    }

    public static <T extends NamedElement> ElementMatcher.Junction<T> name(String name) {
        return new ElementMatcher.Junction.AbstractBase<T>() {
            @Override
            public boolean matches(T target) {
                return target.getActualName().equals(name);
            }

            @Override
            public String toString() {
                return name;
            }
        };
    }

    public static <T extends NamedElement> ElementMatcher.Junction<T> names(String... names) {
        return names(new HashSet<>(Arrays.asList(names)));
    }

    public static <T extends NamedElement> ElementMatcher.Junction<T> names(Set<String> names) {
        return new ElementMatcher.Junction.AbstractBase<T>() {
            @Override
            public boolean matches(T target) {
                return names.contains(target.getActualName());
            }
        };
    }

    public static <T extends MethodDescription> ElementMatcher.Junction<T> argumentSize(int size) {
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
                return String.format(Locale.ENGLISH, "(arg%d is %s)", index, typeName);
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
                return String.format(Locale.ENGLISH, "(annotations: %s)", annotationSet);
            }
        };
    }

    public static <T extends MethodDescription> ElementMatcher.Junction<T> visibility(Visibility... visibility) {
        return new ElementMatcher.Junction.AbstractBase<T>() {
            @Override
            public boolean matches(T target) {
                for (Visibility v : visibility) {
                    if (target.getVisibility().equals(v)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public String toString() {
                return String.format(Locale.ENGLISH, "(visible: %s)",
                                     Arrays.stream(visibility)
                                           .map(Enum::name)
                                           .collect(Collectors.joining(",")));
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

    public static ElementMatcher.Junction<MethodDescription> createArgumentsMatcher(boolean debug, String... args) {
        return new ArgumentsMatcher(debug, false, args);
    }

    public static ElementMatcher.Junction<MethodDescription> createArgumentsMatcher(boolean debug,
                                                                                    boolean matchRawArgType,
                                                                                    String... args) {
        return new ArgumentsMatcher(debug, matchRawArgType, args);
    }

    private static class ArgumentsMatcher implements ElementMatcher.Junction<MethodDescription> {
        private final boolean debug;
        private final boolean matchRawArgType;
        private final String[] args;

        public ArgumentsMatcher(boolean debug, boolean matchRawArgType, String... args) {
            this.debug = debug;
            this.matchRawArgType = matchRawArgType;
            this.args = args;
        }

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
                    String paramType = matchRawArgType
                        ? parameterList.get(i).getType().asRawType().getTypeName()
                        : parameterList.get(i).getType().getTypeName();
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
    }

    public static <T extends MethodDescription> ElementMatcher.Junction<T> implement(String interfaceName) {
        return ElementMatchers.isPublic()
                              .and(ElementMatchers.not(ElementMatchers.isDefaultMethod()))
                              .and(new IsMethodOverriddenFrom<>(interfaceName));
    }

    public static <T extends MethodDescription> ElementMatcher.Junction<T> implement(String... interfaceNames) {
        return ElementMatchers.isPublic()
                              .and(ElementMatchers.not(ElementMatchers.isDefaultMethod()))
                              .and(new IsMethodOverriddenFrom<>(interfaceNames));
    }

    public static <T extends MethodDescription> ElementMatcher.Junction<T> declared(String... interfaceNames) {
        return ElementMatchers.isPublic()
                              .and(ElementMatchers.not(ElementMatchers.isDefaultMethod()))
                              .and(ElementMatchers.isDeclaredBy(ElementMatchers.namedOneOf(interfaceNames)));

    }
}
