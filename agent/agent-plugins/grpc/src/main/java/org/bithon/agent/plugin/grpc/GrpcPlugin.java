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

import org.bithon.agent.core.aop.descriptor.InterceptorDescriptor;
import org.bithon.agent.core.aop.descriptor.MethodPointCutDescriptorBuilder;
import org.bithon.agent.core.aop.matcher.Matchers;
import org.bithon.agent.core.config.ConfigurationManager;
import org.bithon.agent.core.plugin.IPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.bithon.agent.core.aop.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frankchen
 */
public class GrpcPlugin implements IPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {
        List<InterceptorDescriptor> grpcInterceptorDescriptors = Arrays.asList(
            forClass("io.grpc.stub.AbstractBlockingStub")
                .methods(
                    // Hook to enhance stub classes
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethod(Matchers.withName("newStub").and(Matchers.takesArguments(3)))
                                                   .to("org.bithon.agent.plugin.grpc.client.interceptor.AbstractBlockingStub$NewStub")
                ),

            forClass("io.grpc.stub.AbstractFutureStub")
                .methods(
                    // Hook to enhance stub classes
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethod(Matchers.withName("newStub").and(Matchers.takesArguments(3)))
                                                   .to("org.bithon.agent.plugin.grpc.client.interceptor.AbstractFutureStub$NewStub")
                ),

            forClass("io.grpc.stub.AbstractAsyncStub")
                .methods(
                    // Hook to enhance stub classes
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethod(Matchers.withName("newStub").and(Matchers.takesArguments(3)))
                                                   .to("org.bithon.agent.plugin.grpc.client.interceptor.AbstractAsyncStub$NewStub")
                ),

            forClass("io.grpc.internal.ManagedChannelImplBuilder")
                .methods(
                    // Hook to enhance stub classes
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndNoArgs("build")
                                                   .to("org.bithon.agent.plugin.grpc.client.interceptor.ManagedChannelImplBuilder$Build")
                ),


            forClass("io.grpc.internal.ServerImplBuilder")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndNoArgs("build")
                                                   .to("org.bithon.agent.plugin.grpc.server.interceptor.ServerImplBuilder$Build")
                )
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
