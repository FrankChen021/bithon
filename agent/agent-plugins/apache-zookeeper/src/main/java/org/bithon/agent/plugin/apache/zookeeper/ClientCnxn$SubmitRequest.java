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
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AroundInterceptor;
import org.bithon.agent.plugin.apache.zookeeper.metrics.ZKClientMetricRegistry;

/**
 * {@link org.apache.zookeeper.ClientCnxn#submitRequest(RequestHeader, Record, Record, ZooKeeper.WatchRegistration, WatchDeregistration)}
 *
 * @author frank.chen021@outlook.com
 * @date 15/1/25 3:55 pm
 */
public class ClientCnxn$SubmitRequest extends AroundInterceptor {

    @Override
    public InterceptionDecision before(AopContext aopContext) {
        return aopContext.getArgAs(1) == null ? InterceptionDecision.SKIP_LEAVE : InterceptionDecision.CONTINUE;
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

        Record request = aopContext.getArgAs(1);
        String operation = request.getClass().getSimpleName();
        if (operation.endsWith("Request")) {
            operation = operation.substring(0, operation.length() - 7);
        }

        // The ConnectionContext is injected by ClientCnxn$Ctor
        IBithonObject clientCnxn = aopContext.getTargetAs();
        ZKConnectionContext ctx = ((ZKConnectionContext) clientCnxn.getInjectedObject());

        ZKClientMetricRegistry.getInstance()
                              .getOrCreateMetrics(ctx.getServerAddress(),
                                                  operation,
                                                  status)
                              .add(aopContext.getExecutionTime());
    }
}
