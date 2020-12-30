package com.sbss.bithon.agent.core.transformer.matcher;

import shaded.net.bytebuddy.description.method.MethodDescription;
import shaded.net.bytebuddy.description.method.ParameterList;
import shaded.net.bytebuddy.matcher.ElementMatcher;
import shaded.net.bytebuddy.matcher.ElementMatchers;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Description : 默认方法匹配器 <br>
 * Date: 18/3/5
 *
 * @author 马至远
 */
public class DefaultMethodNameMatcher implements AgentMethodMatcher {
    private static final Logger log = LoggerFactory.getLogger(DefaultMethodNameMatcher.class);

    private String methodName;
    private String[] methodArgumentTypes;
    private AgentMethodType agentMethodType;

    private DefaultMethodNameMatcher(String methodName, String[] methodArgumentTypes, AgentMethodType agentMethodType) {
        this.methodName = methodName;
        this.methodArgumentTypes = methodArgumentTypes;
        this.agentMethodType = agentMethodType;
    }

    public static DefaultMethodNameMatcher byName(String methodName) {
        return new DefaultMethodNameMatcher(methodName, null, AgentMethodType.NORMAL);
    }

    public static DefaultMethodNameMatcher byNameAndEmptyArgs(String methodName) {
        return new DefaultMethodNameMatcher(methodName, new String[]{}, AgentMethodType.NORMAL);
    }

    public static DefaultMethodNameMatcher byNameAndArgs(String methodName,
                                                         String... arguments) {
        return new DefaultMethodNameMatcher(methodName,
                                            arguments == null ? new String[]{} : arguments,
                                            AgentMethodType.NORMAL);
    }

    public static DefaultMethodNameMatcher byDefaultCtor() {
        return new DefaultMethodNameMatcher(null,
                                            new String[]{},
                                            AgentMethodType.CONSTRUCTOR);
    }

    public static DefaultMethodNameMatcher byConstructorAndArgs(String[] arguments) {
        return new DefaultMethodNameMatcher(null,
                                            arguments == null ? new String[]{} : arguments,
                                            AgentMethodType.CONSTRUCTOR);
    }

    public static DefaultMethodNameMatcher byAllConstructors() {
        return new DefaultMethodNameMatcher(null, null, AgentMethodType.CONSTRUCTOR);
    }

    public static DefaultMethodNameMatcher byStaticName(String methodName) {
        return new DefaultMethodNameMatcher(methodName, null, AgentMethodType.STATIC);
    }

    public static DefaultMethodNameMatcher byStaticNameAndArgs(String methodName,
                                                               String[] arguments) {
        return new DefaultMethodNameMatcher(methodName,
                                            arguments == null ? new String[]{} : arguments,
                                            AgentMethodType.STATIC);
    }

    @Override
    public AgentMethodType getMethodMatcherType() {
        return agentMethodType;
    }

    @Override
    public ElementMatcher<? super MethodDescription> getMatcher() {
        ElementMatcher.Junction<? super MethodDescription> methodMatcher = ElementMatchers.any();

        switch (this.agentMethodType) {
            case NORMAL:
            case STATIC:
                methodMatcher = methodMatcher.and(ElementMatchers.named(this.methodName));
                break;
            case CONSTRUCTOR:
                break;
            default:
                break;
        }

        if (this.methodArgumentTypes != null) {
            methodMatcher = methodMatcher.and(argumentsMatcher(this.methodArgumentTypes));
        }

        return methodMatcher;
    }

    /**
     * 参数类型匹配器, 根据参数名称匹配
     *
     * @param args 参数类型
     * @return 参数名称匹配器
     */
    private ElementMatcher<MethodDescription> argumentsMatcher(String... args) {
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
                log.debug("~~~ current matching methodDescription is:" + methodDescription.toString());

                // 对比当前构造器的参数和传入的是否匹配, 要求顺序也要严格一致
                ParameterList<?> parameterList = methodDescription.getParameters();
                if (parameterList.size() != args.length) {
                    log.debug("~~~ unmatched, param num not match");
                    return false;
                } else {
                    for (int i = 0; i < args.length; i++) {
                        String paramType = parameterList.get(i).getType().getTypeName();
                        if (!paramType.equals(args[i])) {
                            log.debug("~~~ unmatched, input param type is " + args[i] + ", but actual type is " +
                                          paramType + " not match");
                            return false;
                        }
                    }
                    log.debug("~~~ matched");
                    return true;
                }
            }
        };
    }

    @Override
    public String toString() {
        String args = this.methodArgumentTypes == null ? "[]" : Arrays.stream(this.methodArgumentTypes)
            .reduce((x,
                     y) -> x + ", " + y)
            .orElse("");
        return String.format("%s(%s)", this.methodName, args);
    }
}
