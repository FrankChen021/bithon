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

import org.apache.jute.Record;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchDeregistration;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.proto.ReplyHeader;
import org.apache.zookeeper.proto.RequestHeader;
import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AroundInterceptor;
import org.bithon.agent.plugin.apache.zookeeper.metrics.ZKClientMetricRegistry;

/**
 * For version below 3.5, only the first is defined while these two are defined for 3.5+
 * {@link org.apache.zookeeper.ClientCnxn#submitRequest(RequestHeader, Record, Record, ZooKeeper.WatchRegistration)}
 * {@link org.apache.zookeeper.ClientCnxn#submitRequest(RequestHeader, Record, Record, ZooKeeper.WatchRegistration, WatchDeregistration)}
 *
 * @author frank.chen021@outlook.com
 * @date 15/1/25 3:55 pm
 */
public class ClientCnxn$SubmitRequest extends AroundInterceptor {
    private final OpNameLookup opNameLookup;

    public ClientCnxn$SubmitRequest() {
        opNameLookup = new OpNameLookup();
    }

    @Override
    public void after(AopContext aopContext) {
        String status = "";
        if (aopContext.hasException()) {
            status = aopContext.getException().getClass().getName();
        } else {
            ReplyHeader rh = aopContext.getReturningAs();
            try {
                status = KeeperException.Code.get(rh.getErr()).name();
            } catch (IllegalArgumentException ignored) {
                status = "UNKNOWN(" + rh.getErr() + ")";
            }
        }

        String operation;
        Record request = aopContext.getArgAs(1);
        if (request != null) {
            operation = request.getClass().getSimpleName();
            if (operation.endsWith("Request")) {
                operation = operation.substring(0, operation.length() - 7);
            }
        } else {
            RequestHeader requestHeader = aopContext.getArgAs(0);
            operation = opNameLookup.lookup(requestHeader.getType());
        }

        // The ConnectionContext is injected by ClientCnxn$Ctor
        IBithonObject clientCnxn = aopContext.getTargetAs();
        ZKConnectionContext ctx = ((ZKConnectionContext) clientCnxn.getInjectedObject());

        // The IOContext is initialized when the RequestHeader is created
        IBithonObject requestHeader = aopContext.getArgAs(0);
        IOMetrics ioMetrics = (IOMetrics) requestHeader.getInjectedObject();

        ZKClientMetricRegistry.getInstance()
                              .getOrCreateMetrics(ctx.getServerAddress(),
                                                  operation,
                                                  status)
                              .add(aopContext.getExecutionTime(),
                                   ioMetrics.bytesReceived,
                                   ioMetrics.bytesSent);
    }
}