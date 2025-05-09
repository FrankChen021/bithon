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

package org.bithon.agent.plugin.apache.zookeeper;

import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;
import org.bithon.agent.instrumentation.aop.interceptor.precondition.IInterceptorPrecondition;
import org.bithon.agent.instrumentation.aop.interceptor.precondition.PropertyFileValuePrecondition;
import org.bithon.shaded.net.bytebuddy.description.modifier.Visibility;

import java.util.Arrays;
import java.util.List;

import static org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frankchen
 */
public class ZooKeeperPlugin implements IPlugin {

    @Override
    public IInterceptorPrecondition getPreconditions() {
        return new PropertyFileValuePrecondition("META-INF/maven/org.apache.zookeeper/zookeeper/pom.properties",
                                                 "version",
                                                 PropertyFileValuePrecondition.VersionGTE.of("3.5"));
    }

    @Override
    public List<InterceptorDescriptor> getInterceptors() {
        return Arrays.asList(
            forClass("org.apache.zookeeper.ClientCnxn")
                .onConstructor()
                .andArgs(5, "org.apache.zookeeper.ClientCnxnSocket")
                .interceptedBy("org.bithon.agent.plugin.apache.zookeeper.ClientCnxn$Ctor")

                .onMethod("submitRequest")
                .andVisibility(Visibility.PUBLIC)
                .andArgsSize(5)
                .andArgs(0, "org.apache.zookeeper.proto.RequestHeader")
                .andArgs(1, "org.apache.jute.Record")
                .andArgs(2, "org.apache.jute.Record")
                .interceptedBy("org.bithon.agent.plugin.apache.zookeeper.ClientCnxn$SubmitRequest")

                .onMethod("finishPacket")
                .andArgs("org.apache.zookeeper.ClientCnxn$Packet")
                .interceptedBy("org.bithon.agent.plugin.apache.zookeeper.ClientCnxn$FinishPacket")
                .build(),

            forClass("org.apache.zookeeper.ClientCnxn$SendThread")
                .onMethod("readResponse")
                .andArgs("java.nio.ByteBuffer")
                .interceptedBy("org.bithon.agent.plugin.apache.zookeeper.ClientCnxnSendThread$ReadResponse")
                .build(),

            // For IO metrics
            forClass("org.apache.zookeeper.ClientCnxn$Packet")
                .onMethod("createBB")
                .andArgsSize(0)
                .interceptedBy("org.bithon.agent.plugin.apache.zookeeper.ClientCnxnPacket$CreateBB")
                .build(),

            // For IO metrics
            forClass("org.apache.zookeeper.proto.RequestHeader")
                .onConstructor()
                .interceptedBy("org.bithon.agent.plugin.apache.zookeeper.RequestHeader$Ctor")
                .build(),

            // Context injection
            forClass("org.apache.zookeeper.ClientCnxnSocketNIO")
                .onMethod("connect")
                .andArgs("java.net.InetSocketAddress")
                .interceptedBy("org.bithon.agent.plugin.apache.zookeeper.ClientCnxnSocket$Connect")
                .build(),

            // Context injection
            forClass("org.apache.zookeeper.ClientCnxnSocketNetty")
                .onMethod("connect")
                .interceptedBy("org.bithon.agent.plugin.apache.zookeeper.ClientCnxnSocket$Connect")
                .build()
        );
    }
}
