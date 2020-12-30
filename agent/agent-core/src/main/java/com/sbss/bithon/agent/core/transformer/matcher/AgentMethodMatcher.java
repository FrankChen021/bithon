package com.sbss.bithon.agent.core.transformer.matcher;

import com.sbss.bithon.agent.core.interceptor.AroundMethodInterceptor;
import com.sbss.bithon.agent.core.interceptor.ConstructorInterceptor;
import com.sbss.bithon.agent.core.interceptor.StaticMethodInterceptor;
import shaded.net.bytebuddy.description.method.MethodDescription;
import shaded.net.bytebuddy.matcher.ElementMatcher;

/**
 * Description : agent method matcher <br>
 * Date: 18/3/2
 *
 * @author 马至远
 */
public interface AgentMethodMatcher {
    ElementMatcher<? super MethodDescription> getMatcher();

    AgentMethodType getMethodMatcherType();

    enum AgentMethodType {
        CONSTRUCTOR(ConstructorInterceptor.class.getName()),
        STATIC(StaticMethodInterceptor.class.getName()),
        NORMAL(AroundMethodInterceptor.class.getName());

        private String interceptorName;

        AgentMethodType(String interceptorName) {
            this.interceptorName = interceptorName;
        }

        public String getInterceptorName() {
            return this.interceptorName;
        }
    }
}
