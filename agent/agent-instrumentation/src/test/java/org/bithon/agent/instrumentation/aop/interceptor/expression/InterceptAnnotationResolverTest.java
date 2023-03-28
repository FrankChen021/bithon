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

package org.bithon.agent.instrumentation.aop.interceptor.expression;

import org.bithon.agent.instrumentation.aop.interceptor.BeforeInterceptor;
import org.bithon.agent.instrumentation.aop.interceptor.Intercept;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.InterceptAnnotationResolver;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/3/28 23:01
 */
public class InterceptAnnotationResolverTest {

    @Intercept(expressions = "java.lang.ThreadPool#ctor()")
    static class MyInterceptor extends BeforeInterceptor {
    }


    @Test
    public void testResolver() throws IOException {
        URL url = this.getClass().getClassLoader().getResource(MyInterceptor.class.getName().replace('.', '/') + ".class");

        InterceptAnnotationResolver resolver = new InterceptAnnotationResolver();
        resolver.resolve(url.openStream(), this.getClass().getClassLoader());
    }
}
