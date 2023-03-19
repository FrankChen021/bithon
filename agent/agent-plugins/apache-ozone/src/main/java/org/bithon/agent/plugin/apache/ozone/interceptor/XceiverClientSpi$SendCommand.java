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

package org.bithon.agent.plugin.apache.ozone.interceptor;

import org.apache.hadoop.hdds.protocol.datanode.proto.ContainerProtos;
import org.bithon.agent.bootstrap.aop.context.AopContext;
import org.bithon.agent.bootstrap.aop.interceptor.AroundInterceptor;
import org.bithon.agent.bootstrap.aop.interceptor.InterceptionDecision;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceSpanFactory;

import java.util.List;

/**
 * {@link org.apache.hadoop.hdds.scm.XceiverClientRatis} does not override the following methods, so we hook the parent of XceiverClientRatis .
 * These two are sync methods, we can get the right execution time on underlying RPC
 * <p>
 * {@link org.apache.hadoop.hdds.scm.XceiverClientSpi#sendCommand(ContainerProtos.ContainerCommandRequestProto)}
 * {@link org.apache.hadoop.hdds.scm.XceiverClientSpi#sendCommand(ContainerProtos.ContainerCommandRequestProto, List)}
 *
 * @author Frank Chen
 * @date 21/12/22 5:23 pm
 */
public class XceiverClientSpi$SendCommand extends AroundInterceptor {

    @Override
    public InterceptionDecision before(AopContext aopContext) {
        ITraceSpan span = TraceSpanFactory.newSpan("ozone-hdds");
        if (span == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        ContainerProtos.ContainerCommandRequestProto request = aopContext.getArgAs(0);

        aopContext.setUserContext(span.method(aopContext.getMethod())
                                      .tag("request", request.getCmdType().name())
                                      .start());

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void after(AopContext aopContext) {
        ITraceSpan span = aopContext.getUserContextAs();
        span.tag(aopContext.getException()).finish();
    }
}
