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
import org.bithon.agent.core.interceptor.installer.DynamicInterceptorInstaller;
import org.bithon.agent.instrumentation.aop.advice.AdviceClassGenerator;
import org.bithon.agent.instrumentation.aop.advice.DynamicAopAdvice;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.BeforeInterceptor;
import org.bithon.agent.instrumentation.aop.interceptor.matcher.Matchers;
import org.bithon.shaded.net.bytebuddy.asm.Advice;
import org.bithon.shaded.net.bytebuddy.description.modifier.Visibility;
import org.bithon.shaded.net.bytebuddy.dynamic.ClassFileLocator;
import org.bithon.shaded.net.bytebuddy.dynamic.DynamicType;

import java.lang.reflect.ParameterizedType;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Intercept {@link io.grpc.stub.AbstractFutureStub#newStub(AbstractStub.StubFactory, Channel, CallOptions)} to enhance the generated stub classes
 *
 * @author Frank Chen
 * @date 13/12/22 5:36 pm
 */
public class AbstractFutureStub$NewStub extends BeforeInterceptor {

    private static final Set<String> INSTRUMENTED = new ConcurrentSkipListSet<>();

    private final DynamicType.Unloaded<?> grpcStubAopClass;

    public AbstractFutureStub$NewStub() {
        String targetAopClassName = AbstractFutureStub$NewStub.class.getPackage().getName() + ".FutureStubAop";

        grpcStubAopClass = AdviceClassGenerator.generateAdviceClass(DynamicAopAdvice.class,
                                                                    targetAopClassName,
                                                                    AbstractGrpcStubInterceptor.FutureStubInterceptor.class.getName(),
                                                                    true);
        AdviceClassGenerator.inject(grpcStubAopClass);
    }

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
        DynamicInterceptorInstaller.getInstance().installOne(new DynamicInterceptorInstaller.AopDescriptor(clientStubClass.getName(),
                                                                                                           Advice.to(grpcStubAopClass.getTypeDescription(),
                                                                                                                     ClassFileLocator.Simple.of(grpcStubAopClass.getAllTypes())),
                                                                                                           Matchers.visibility(Visibility.PUBLIC)));
    }
}
