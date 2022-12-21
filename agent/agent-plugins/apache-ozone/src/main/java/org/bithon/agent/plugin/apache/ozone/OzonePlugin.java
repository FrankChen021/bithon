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

package org.bithon.agent.plugin.apache.ozone;

import org.bithon.agent.core.aop.descriptor.InterceptorDescriptor;
import org.bithon.agent.core.aop.descriptor.MethodPointCutDescriptorBuilder;
import org.bithon.agent.core.aop.matcher.Matchers;
import org.bithon.agent.core.plugin.IPlugin;
import org.bithon.shaded.net.bytebuddy.matcher.ElementMatchers;

import java.util.Arrays;
import java.util.List;

import static org.bithon.agent.core.aop.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frankchen
 * @date 2022-01-04 18:35
 */
public class OzonePlugin implements IPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {
        return Arrays.asList(
            forClass("org.apache.hadoop.ozone.client.rpc.RpcClient")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethod(ElementMatchers.isOverriddenFrom(ElementMatchers.named(
                                                       "org.apache.hadoop.ozone.client.protocol.ClientProtocol")))
                                                   .to("org.bithon.agent.plugin.apache.ozone.interceptor.RpcClient$All")
                ),

            forClass("org.apache.hadoop.ozone.om.protocolPB.OzoneManagerProtocolClientSideTranslatorPB")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethod(ElementMatchers.isOverriddenFrom(ElementMatchers.named(
                                                       "org.apache.hadoop.ozone.om.protocol.OzoneManagerProtocol")))
                                                   .to("org.bithon.agent.plugin.apache.ozone.interceptor.RpcClient$All"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndNoArgs("close")
                                                   .to("org.bithon.agent.plugin.apache.ozone.interceptor.RpcClient$All")
                ),

            // s3g -> scm
            forClass("org.apache.hadoop.hdds.scm.XceiverClientGrpc")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndNoArgs("connect")
                                                   .to("org.bithon.agent.plugin.apache.ozone.interceptor.RpcClient$All"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("sendCommand")
                                                   .to("org.bithon.agent.plugin.apache.ozone.interceptor.XceiverClientGrpc$SendCommand"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("sendCommandOnAllNodes")
                                                   .to("org.bithon.agent.plugin.apache.ozone.interceptor.XceiverClientGrpc$SendCommandOnAllNodes"),

                    // sendCommandAsync contains the DataNode parameter that we know where the command is sent to
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethod(Matchers.withName("sendCommandAsync").and(Matchers.takesArguments(2)))
                                                   .to("org.bithon.agent.plugin.apache.ozone.interceptor.XceiverClientGrpc$SendCommandAsync")
                ),

            forClass("org.apache.hadoop.hdds.scm.XceiverClientRatis")
                .methods(
                    // Hook to save leader info
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndNoArgs("connect")
                                                   .to("org.bithon.agent.plugin.apache.ozone.interceptor.XceiverClientRatis$Connect"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethod(Matchers.withName("sendCommandAsync").and(Matchers.takesArguments(1)))
                                                   .to("org.bithon.agent.plugin.apache.ozone.interceptor.XceiverClientRatis$SendCommandAsync")
                ),

            forClass("org.apache.hadoop.hdds.scm.XceiverClientSpi")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("sendCommand")
                                                   .to("org.bithon.agent.plugin.apache.ozone.interceptor.XceiverClientSpi$SendCommand")
                )
        );
    }
}
