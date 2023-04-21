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

package org.bithon.agent.plugin.grpc.client.interceptor;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.stub.AbstractStub;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.BeforeInterceptor;
import org.bithon.agent.instrumentation.aop.interceptor.installer.DynamicInterceptorInstaller;
import org.bithon.agent.instrumentation.aop.interceptor.matcher.Matchers;
import org.bithon.shaded.net.bytebuddy.description.modifier.Visibility;

import java.lang.reflect.ParameterizedType;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Intercept {@link io.grpc.stub.AbstractAsyncStub#newStub(AbstractStub.StubFactory, Channel, CallOptions)} to enhance the generated stub classes
 *
 * @author Frank Chen
 * @date 13/12/22 5:36 pm
 */
public class AbstractAsyncStub$NewStub extends BeforeInterceptor {

    private static final Set<String> INSTRUMENTED = new ConcurrentSkipListSet<>();

    @Override
    public void before(AopContext aopContext) {
        Object stubFactory = aopContext.getArgs()[0];

        // StubFactory is defined to accept one generic argument
        ParameterizedType genericArgumentType = (ParameterizedType) stubFactory.getClass().getGenericInterfaces()[0];
        Class<?> clientStubClass = (Class<?>) genericArgumentType.getActualTypeArguments()[0];

        if (!INSTRUMENTED.add(clientStubClass.getName())) {
            return;
        }

        // Enhance the stub class
        DynamicInterceptorInstaller.getInstance()
                                   .installOne(new DynamicInterceptorInstaller.AopDescriptor(clientStubClass.getName(),
                                                                                             Matchers.visibility(Visibility.PUBLIC),
                                                                                             AbstractGrpcStubInterceptor.AsyncStubInterceptor.class.getName()));
    }
}
