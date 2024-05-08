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
                .hook()
                .onMethod(Matchers.implement("org.apache.hadoop.ozone.client.protocol.ClientProtocol"))
                .to("org.bithon.agent.plugin.apache.ozone.interceptor.RpcClient$All")
                .build(),

            forClass("org.apache.hadoop.ozone.om.protocolPB.OzoneManagerProtocolClientSideTranslatorPB")
                .hook()
                .onMethod(Matchers.implement("org.apache.hadoop.ozone.om.protocol.OzoneManagerProtocol"))
                .to("org.bithon.agent.plugin.apache.ozone.interceptor.RpcClient$All")

                .hook()
                .onMethodAndNoArgs("close")
                .to("org.bithon.agent.plugin.apache.ozone.interceptor.RpcClient$All")
                .build(),

            // s3g -> scm
            forClass("org.apache.hadoop.hdds.scm.XceiverClientGrpc")
                .hook()
                .onMethodAndNoArgs("connect")
                .to("org.bithon.agent.plugin.apache.ozone.interceptor.RpcClient$All")

                .hook()
                .onMethodName("sendCommand")
                .to("org.bithon.agent.plugin.apache.ozone.interceptor.XceiverClientGrpc$SendCommand")

                .hook()
                .onMethodName("sendCommandOnAllNodes")
                .to("org.bithon.agent.plugin.apache.ozone.interceptor.XceiverClientGrpc$SendCommandOnAllNodes")

                // sendCommandAsync contains the DataNode parameter that we know where the command is sent to
                .hook()
                .onMethod(Matchers.name("sendCommandAsync").and(Matchers.takesArguments(2)))
                .to("org.bithon.agent.plugin.apache.ozone.interceptor.XceiverClientGrpc$SendCommandAsync")
                .build(),

            // Hook to save leader info
            forClass("org.apache.hadoop.hdds.scm.XceiverClientRatis")
                .hook()
                .onMethodAndNoArgs("connect")
                .to("org.bithon.agent.plugin.apache.ozone.interceptor.XceiverClientRatis$Connect")

                .hook()
                .onMethod(Matchers.name("sendCommandAsync").and(Matchers.takesArguments(1)))
                .to("org.bithon.agent.plugin.apache.ozone.interceptor.XceiverClientRatis$SendCommandAsync")
                .build(),

            forClass("org.apache.hadoop.hdds.scm.XceiverClientSpi")
                .hook()
                .onMethodName("sendCommand")
                .to("org.bithon.agent.plugin.apache.ozone.interceptor.XceiverClientSpi$SendCommand")
                .build()
        );
    }
}
