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

package org.bithon.agent.plugin.grpc;

import org.bithon.agent.configuration.ConfigurationManager;
import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.matcher.Matchers;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frankchen
 */
public class GrpcPlugin implements IPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {
        List<InterceptorDescriptor> grpcInterceptorDescriptors = Arrays.asList(
            // Hook to enhance stub classes
            forClass("io.grpc.stub.AbstractBlockingStub")
                .hook()
                .onMethod(Matchers.name("newStub").and(Matchers.takesArguments(3)))
                .to("org.bithon.agent.plugin.grpc.client.interceptor.AbstractBlockingStub$NewStub")
                .build(),

            // Hook to enhance stub classes
            forClass("io.grpc.stub.AbstractFutureStub")
                .hook()
                .onMethod(Matchers.name("newStub").and(Matchers.takesArguments(3)))
                .to("org.bithon.agent.plugin.grpc.client.interceptor.AbstractFutureStub$NewStub")
                .build(),

            // Hook to enhance stub classes
            forClass("io.grpc.stub.AbstractAsyncStub")
                .hook()
                .onMethod(Matchers.name("newStub").and(Matchers.takesArguments(3)))
                .to("org.bithon.agent.plugin.grpc.client.interceptor.AbstractAsyncStub$NewStub")
                .build(),

            forClass("io.grpc.internal.ManagedChannelImplBuilder")
                .hook()
                .onMethodAndNoArgs("build")
                .to("org.bithon.agent.plugin.grpc.client.interceptor.ManagedChannelImplBuilder$Build")
                .build(),

            forClass("io.grpc.internal.ServerImplBuilder")
                .hook()
                .onMethodAndNoArgs("build")
                .to("org.bithon.agent.plugin.grpc.server.interceptor.ServerImplBuilder$Build")
                .build()
        );

        //
        // Install interceptor for shaded gRPC
        //
        List<String> shadedGrpcList = ConfigurationManager.getInstance().getConfig(ShadedGrpcList.class);
        if (!shadedGrpcList.isEmpty()) {
            List<InterceptorDescriptor> shadedGrpcInterceptors = new ArrayList<>();

            // create interceptor descriptors for shaded grpc
            for (String shadedGrpc : shadedGrpcList) {
                for (InterceptorDescriptor interceptorDescriptor : grpcInterceptorDescriptors) {
                    String shadedGrpcClazz = interceptorDescriptor.getTargetClass().replace("io.grpc", shadedGrpc);
                    shadedGrpcInterceptors.add(interceptorDescriptor.withTargetClazz(shadedGrpcClazz));
                }
            }

            // Copy the readonly list to a new writable list
            grpcInterceptorDescriptors = new ArrayList<>(grpcInterceptorDescriptors);

            // Merge interceptors
            grpcInterceptorDescriptors.addAll(shadedGrpcInterceptors);
        }

        return grpcInterceptorDescriptors;
    }
}
