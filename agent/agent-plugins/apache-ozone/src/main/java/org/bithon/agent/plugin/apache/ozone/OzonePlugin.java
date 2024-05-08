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

import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.matcher.Matchers;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;

import java.util.Arrays;
import java.util.List;

import static org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frankchen
 * @date 2022-01-04 18:35
 */
public class OzonePlugin implements IPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {
        return Arrays.asList(
            forClass("org.apache.hadoop.ozone.client.rpc.RpcClient")
                .onMethod(Matchers.implement("org.apache.hadoop.ozone.client.protocol.ClientProtocol"))
                .interceptedBy("org.bithon.agent.plugin.apache.ozone.interceptor.RpcClient$All")
                .build(),

            forClass("org.apache.hadoop.ozone.om.protocolPB.OzoneManagerProtocolClientSideTranslatorPB")
                .onMethod(Matchers.implement("org.apache.hadoop.ozone.om.protocol.OzoneManagerProtocol"))
                .interceptedBy("org.bithon.agent.plugin.apache.ozone.interceptor.RpcClient$All")

                .onMethod("close")
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.apache.ozone.interceptor.RpcClient$All")
                .build(),

            // s3g -> scm
            forClass("org.apache.hadoop.hdds.scm.XceiverClientGrpc")
                .onMethod("connect")
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.apache.ozone.interceptor.RpcClient$All")

                .onMethod("sendCommand")
                .interceptedBy("org.bithon.agent.plugin.apache.ozone.interceptor.XceiverClientGrpc$SendCommand")

                .onMethod("sendCommandOnAllNodes")
                .interceptedBy("org.bithon.agent.plugin.apache.ozone.interceptor.XceiverClientGrpc$SendCommandOnAllNodes")

                // sendCommandAsync contains the DataNode parameter that we know where the command is sent to
                .onMethod(Matchers.name("sendCommandAsync").and(Matchers.takesArguments(2)))
                .interceptedBy("org.bithon.agent.plugin.apache.ozone.interceptor.XceiverClientGrpc$SendCommandAsync")
                .build(),

            // Hook to save leader info
            forClass("org.apache.hadoop.hdds.scm.XceiverClientRatis")
                .onMethod("connect")
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.apache.ozone.interceptor.XceiverClientRatis$Connect")

                .onMethod(Matchers.name("sendCommandAsync").and(Matchers.takesArguments(1)))
                .interceptedBy("org.bithon.agent.plugin.apache.ozone.interceptor.XceiverClientRatis$SendCommandAsync")
                .build(),

            forClass("org.apache.hadoop.hdds.scm.XceiverClientSpi")
                .onMethod("sendCommand")
                .interceptedBy("org.bithon.agent.plugin.apache.ozone.interceptor.XceiverClientSpi$SendCommand")
                .build()
        );
    }
}
